package com.bebeis.skillweaver.api.agent

import com.bebeis.skillweaver.agent.domain.GeneratedLearningPlan
import com.bebeis.skillweaver.agent.domain.LearningRequest
import com.bebeis.skillweaver.agent.domain.LearningResource
import com.bebeis.skillweaver.agent.domain.DailyScheduleItem
import com.bebeis.skillweaver.api.agent.dto.AgentEventDto
import com.bebeis.skillweaver.api.agent.dto.ActionExecutionDto
import com.bebeis.skillweaver.api.agent.dto.ActionStatus
import com.bebeis.skillweaver.api.common.auth.AuthUser
import com.bebeis.skillweaver.api.plan.dto.CreateLearningPlanRequest
import com.bebeis.skillweaver.api.plan.dto.CreateStepRequest
import com.bebeis.skillweaver.api.plan.dto.DailyScheduleItemRequest
import com.bebeis.skillweaver.api.plan.dto.SuggestedResourceRequest
import com.bebeis.skillweaver.core.domain.learning.ResourceType
import com.bebeis.skillweaver.core.domain.learning.StepDifficulty
import com.bebeis.skillweaver.core.domain.agent.AgentType
import com.bebeis.skillweaver.core.domain.agent.SseEventType.AGENT_STARTED
import com.bebeis.skillweaver.core.service.agent.AgentRunService
import com.bebeis.skillweaver.core.service.learning.LearningPlanService
import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.AgentProcess
import com.embabel.agent.core.AgentProcessStatusCode
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.Verbosity
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

@RestController
@RequestMapping("/api/v1/agents")
class AgentStreamController(
    private val agentRunService: AgentRunService,
    private val learningPlanService: LearningPlanService,
    private val agentPlatform: AgentPlatform,
    private val objectMapper: ObjectMapper,
    private val agentEventBroadcaster: AgentEventBroadcaster
) {
    private val logger = LoggerFactory.getLogger(AgentStreamController::class.java)
    private val executor = Executors.newCachedThreadPool()

    @GetMapping(
        "/learning-plan/stream",
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun executeLearningPlanAgentWithStream(
        @AuthUser authMemberId: Long,
        @RequestParam memberId: Long,
        @RequestParam targetTechnology: String,
        @RequestParam(name = "accessToken", required = false) accessToken: String? = null,
        @RequestParam(name = "access_token", required = false) snakeAccessToken: String? = null
    ): SseEmitter {
        require(authMemberId == memberId) { "본인의 학습 플랜만 실행할 수 있습니다" }
        if ((accessToken ?: snakeAccessToken) != null) {
            logger.debug("Access token supplied via query parameter for SSE request, memberId={}", memberId)
        }
        val emitter = SseEmitter(30 * 60 * 1000L)
        val emitterActive = AtomicBoolean(true)
        val sequence = AtomicLong(0)
        val learningRequest = LearningRequest(
            memberId = memberId,
            targetTechnologyKey = targetTechnology
        )
        
        CompletableFuture.runAsync({
            var runId: Long? = null
            try {
                val agentRun = agentRunService.createRun(
                    memberId = memberId,
                    agentType = AgentType.LEARNING_PLAN,
                    parameters = objectMapper.writeValueAsString(
                        mapOf(
                            "targetTechnology" to targetTechnology
                        )
                    )
                )
                runId = agentRun.agentRunId!!

                sendEvent(
                    emitter = emitter,
                    runId = runId,
                    eventName = "agent_started",
                    sequence = sequence.incrementAndGet(),
                    event = AgentEventDto(
                        type = AGENT_STARTED,
                        agentRunId = runId,
                        message = "Agent 실행 시작",
                        timestamp = System.currentTimeMillis()
                    )
                )

                val agent = agentPlatform.agents()
                    .firstOrNull { it.name.contains("NewTechLearningAgent", ignoreCase = true) }
                    ?: throw IllegalStateException("NewTechLearningAgent를 찾을 수 없습니다.")

                val agentProcess = agentPlatform.createAgentProcessFrom(
                    agent = agent,
                    processOptions = ProcessOptions(
                        verbosity = Verbosity(
                            showPrompts = true,
                            showLlmResponses = true
                        )
                    ),
                    learningRequest
                )

                agentRunService.startRun(runId)

                sendEvent(
                    emitter = emitter,
                    runId = runId,
                    eventName = "planning_started",
                    sequence = sequence.incrementAndGet(),
                    event = AgentEventDto(
                        type = com.bebeis.skillweaver.core.domain.agent.SseEventType.PLANNING_STARTED,
                        agentRunId = runId,
                        message = "GOAP 경로 계획 중...",
                        timestamp = System.currentTimeMillis()
                    )
                )

                monitorProcessHistory(agentProcess, runId!!, emitter, emitterActive, sequence)

                agentPlatform.start(agentProcess)

                waitForProcessCompletion(agentProcess)

                val result = agentProcess.last(Any::class.java)
                val normalizedResult = normalizeResult(result)
                val persistedPlan = persistGeneratedPlanIfNeeded(memberId, normalizedResult)
                val finalResult = persistedPlan ?: normalizedResult
                val resultJson = objectMapper.writeValueAsString(finalResult)

                agentRunService.completeRun(
                    agentRunId = runId,
                    result = resultJson,
                    learningPlanId = persistedPlan?.learningPlanId,
                    cost = 0.0,
                    executionTimeMs = System.currentTimeMillis()
                )

                sendEvent(
                    emitter = emitter,
                    runId = runId,
                    eventName = "agent_completed",
                    sequence = sequence.incrementAndGet(),
                    event = AgentEventDto(
                        type = com.bebeis.skillweaver.core.domain.agent.SseEventType.AGENT_COMPLETED,
                        agentRunId = runId,
                        message = "Agent 실행 완료",
                        result = finalResult,
                        timestamp = System.currentTimeMillis()
                    )
                )

                runId?.let { agentEventBroadcaster.complete(it) }
                emitter.complete()

            } catch (e: Exception) {
                logger.error("Agent 실행 중 오류 발생", e)
                val errorEvent = AgentEventDto(
                    type = com.bebeis.skillweaver.core.domain.agent.SseEventType.ERROR,
                    agentRunId = runId,
                    message = "오류 발생: ${e.message}",
                    timestamp = System.currentTimeMillis()
                )
                sendEvent(
                    emitter = emitter,
                    runId = runId,
                    eventName = "error",
                    sequence = sequence.incrementAndGet(),
                    event = errorEvent
                )
                runId?.let {
                    agentRunService.failRun(it, e.message ?: "Agent execution failed")
                    agentEventBroadcaster.complete(it)
                }
                emitter.completeWithError(e)
            }
        }, executor)

        emitter.onCompletion {
            emitterActive.set(false)
            logger.info("SSE 연결 완료")
        }
        emitter.onTimeout {
            emitterActive.set(false)
            logger.warn("SSE 연결 타임아웃")
        }
        emitter.onError { e ->
            emitterActive.set(false)
            logger.error("SSE 연결 오류", e)
        }

        return emitter
    }

    private fun monitorProcessHistory(
        process: AgentProcess,
        agentRunId: Long,
        emitter: SseEmitter,
        emitterActive: AtomicBoolean,
        sequence: AtomicLong
    ) {
        Thread {
            var lastHistorySize = 0
            val executedActionRecords = mutableListOf<ActionExecutionDto>()
            val executedPath = mutableListOf<String>()

            while (emitterActive.get()) {
                try {
                    val history = process.history
                    if (history.size > lastHistorySize) {
                        val newActions = history.subList(lastHistorySize, history.size)

                        newActions.forEach { action ->
                            val actionName = action.actionName.ifBlank { "UnknownAction" }
                            val duration = action.runningTime.toMillis()

                            executedActionRecords += ActionExecutionDto(
                                name = actionName,
                                status = ActionStatus.COMPLETED,
                                durationMs = duration
                            )
                            executedPath += actionName

                            sendEvent(
                                emitter = emitter,
                                runId = agentRunId,
                                eventName = "action_executed",
                                sequence = sequence.incrementAndGet(),
                                event = AgentEventDto(
                                    type = com.bebeis.skillweaver.core.domain.agent.SseEventType.ACTION_EXECUTED,
                                    agentRunId = agentRunId,
                                    actionName = actionName,
                                    message = "$actionName 실행 완료 (${duration}ms)",
                                    executedActions = executedActionRecords.toList(),
                                    executedPath = executedPath.toList(),
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }

                        sendEvent(
                            emitter = emitter,
                            runId = agentRunId,
                            eventName = "path_updated",
                            sequence = sequence.incrementAndGet(),
                            event = AgentEventDto(
                                type = com.bebeis.skillweaver.core.domain.agent.SseEventType.PATH_UPDATED,
                                agentRunId = agentRunId,
                                message = "경로 업데이트 (${executedPath.size}개 액션 실행)",
                                executedPath = executedPath.toList(),
                                executedActions = executedActionRecords.toList(),
                                timestamp = System.currentTimeMillis()
                            )
                        )

                        sendProgress(emitter, agentRunId, sequence, executedActionRecords)
                        lastHistorySize = history.size
                    }

                    if (process.finished) {
                        break
                    }
                    Thread.sleep(200)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    logger.error("이벤트 모니터링 중 오류", e)
                    break
                }
            }
        }.start()
    }

    private fun sendProgress(
        emitter: SseEmitter,
        agentRunId: Long,
        sequence: AtomicLong,
        executedActionRecords: List<ActionExecutionDto>
    ) {
        val completedCount = executedActionRecords.count { it.status == ActionStatus.COMPLETED }
        val lastActionName = executedActionRecords.lastOrNull()?.name
        val progressMessage = if (lastActionName != null) {
            "진행 중... (${completedCount}개 액션 완료, 최근: $lastActionName)"
        } else {
            "진행 중... (${completedCount}개 액션 완료)"
        }

        sendEvent(
            emitter = emitter,
            runId = agentRunId,
            eventName = "progress",
            sequence = sequence.incrementAndGet(),
            event = AgentEventDto(
                type = com.bebeis.skillweaver.core.domain.agent.SseEventType.PROGRESS,
                agentRunId = agentRunId,
                message = progressMessage,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private fun sendEvent(
        emitter: SseEmitter,
        runId: Long?,
        eventName: String,
        sequence: Long,
        event: AgentEventDto
    ): Boolean {
        val enrichedEvent = event.copy(
            eventName = eventName,
            sequence = sequence
        )
        logger.debug(
            "Sending SSE event='{}' seq={} runId={} type={} message={}",
            eventName,
            sequence,
            runId,
            event.type,
            event.message
        )
        return try {
            emitter.send(
                SseEmitter.event()
                    .name(eventName)
                    .data(enrichedEvent)
            )
            if (runId != null) {
                agentEventBroadcaster.broadcast(runId, eventName, enrichedEvent)
            }
            true
        } catch (ex: IllegalStateException) {
            logger.debug(
                "Emitter already completed or unavailable for runId={}, event='{}': {}",
                runId,
                eventName,
                ex.message
            )
            false
        } catch (ex: IOException) {
            logger.debug(
                "Emitter IO error for runId={}, event='{}': {}",
                runId,
                eventName,
                ex.message
            )
            false
        }
    }

    private fun normalizeResult(result: Any?): Any? {
        if (result !is GeneratedLearningPlan) return result
        return normalizeGeneratedLearningPlan(result)
    }

    private fun normalizeGeneratedLearningPlan(plan: GeneratedLearningPlan): GeneratedLearningPlan {
        val normalizedSteps = plan.steps.map { step ->
            // step.resources의 런타임 타입이 LinkedHashMap일 수 있으므로 원시 컬렉션으로 받아 캐스팅 오류를 방지한다.
            val normalizedResources = (step.resources as? Collection<*>)?.mapNotNull { resource ->
                when (resource) {
                    is LearningResource -> resource
                    is Map<*, *> -> mapToLearningResource(resource)
                    null -> null
                    else -> {
                        logger.warn(
                            "Unexpected resource type '{}' in step '{}'",
                            resource::class.java.name,
                            step.title
                        )
                        null
                    }
                }
            }.orEmpty()
            step.copy(resources = normalizedResources)
        }
        return plan.copy(steps = normalizedSteps)
    }

    private fun mapToLearningResource(resourceMap: Map<*, *>): LearningResource? {
        return runCatching {
            val title = resourceMap["title"]?.toString() ?: return null
            val url = resourceMap["url"]?.toString() ?: return null
            val type = resourceMap["type"]?.toString()
                ?.let { normalizeResourceType(it) }
                ?: ResourceType.DOC
            val language = resourceMap["language"]?.toString()
            val description = resourceMap["description"]?.toString()
            LearningResource(
                type = type,
                title = title,
                url = url,
                language = language,
                description = description
            )
        }.onFailure {
            logger.warn("Failed to normalize learning resource: {}", resourceMap, it)
        }.getOrNull()
    }

    private fun normalizeResourceType(raw: String): ResourceType {
        return ResourceType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ResourceType.DOC
    }

    /**
     * GeneratedLearningPlan을 실제 LearningPlan 엔티티로 저장하고 planId를 반환한다.
     * 다른 타입이면 그대로 null 반환.
     */
    private fun persistGeneratedPlanIfNeeded(memberId: Long, result: Any?): com.bebeis.skillweaver.api.plan.dto.LearningPlanResponse? {
        if (result !is GeneratedLearningPlan) return null

        return try {
            val totalWeeks = max(1, ChronoUnit.WEEKS.between(result.startDate, result.targetEndDate).toInt().coerceAtLeast(1))
            val backgroundAnalysis = result.backgroundAnalysis?.let { objectMapper.writeValueAsString(it) } ?: result.description
            val dailySchedule = result.dailySchedule.map { it.toRequest() }
            val request = CreateLearningPlanRequest(
                targetTechnology = result.targetTechnologyName,
                totalWeeks = totalWeeks,
                totalHours = result.totalEstimatedHours,
                backgroundAnalysis = backgroundAnalysis,
                steps = result.steps.map {
                    CreateStepRequest(
                        title = it.title,
                        description = it.description,
                        estimatedHours = it.estimatedHours,
                        difficulty = StepDifficulty.MEDIUM,
                        objectives = it.keyTopics,
                        suggestedResources = it.resources.toSuggestedResourceRequests()
                    )
                },
                dailySchedule = dailySchedule
            )
            learningPlanService.createPlan(memberId, request)
        } catch (ex: Exception) {
            logger.error("GeneratedLearningPlan 저장 중 오류 발생, planId를 설정하지 못했습니다", ex)
            null
        }
    }

    private fun List<LearningResource>.toSuggestedResourceRequests(): List<SuggestedResourceRequest> {
        return this.map {
            SuggestedResourceRequest(
                type = it.type,
                title = it.title,
                url = it.url,
                language = it.language
            )
        }
    }
    
    private fun DailyScheduleItem.toRequest(): DailyScheduleItemRequest {
        return DailyScheduleItemRequest(
            dayIndex = this.dayIndex,
            date = this.date,
            allocatedMinutes = this.allocatedMinutes,
            stepRef = this.stepRef,
            tasks = this.tasks
        )
    }

    /**
     * 에이전트 프로세스가 실제로 종료될 때까지 대기한다.
     * 완료 전 상태(STUCK/FAILED/TERMINATED/KILLED)로 끝나면 예외를 던져 상위 로직이 오류 이벤트를 전송하도록 한다.
     */
    private fun waitForProcessCompletion(process: AgentProcess) {
        while (!process.finished) {
            try {
                Thread.sleep(200)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IllegalStateException("에이전트 실행 대기 중 인터럽트 발생")
            }
        }

        val status = process.status
        if (status != AgentProcessStatusCode.COMPLETED) {
            throw IllegalStateException("에이전트 프로세스가 완료되지 않았습니다. status=$status, failure=${process.failureInfo}")
        }
    }
}
