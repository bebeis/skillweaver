package com.bebeis.skillweaver.api.agent

import com.bebeis.skillweaver.api.agent.dto.AgentEventDto
import com.bebeis.skillweaver.api.agent.dto.AgentRunResponse
import com.bebeis.skillweaver.api.agent.dto.CreateAgentRunRequest
import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.core.domain.agent.AgentRunStatus
import com.bebeis.skillweaver.core.domain.agent.AgentType
import com.bebeis.skillweaver.core.domain.agent.SseEventType.AGENT_STARTED
import com.bebeis.skillweaver.core.service.agent.AgentRunService
import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.AgentProcess
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.Verbosity
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@RestController
@RequestMapping("/api/v1/agents")
class AgentStreamController(
    private val agentRunService: AgentRunService,
    private val agentPlatform: AgentPlatform,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(AgentStreamController::class.java)
    private val executor = Executors.newCachedThreadPool()

    @PostMapping("/learning-plan/stream")
    fun executeLearningPlanAgentWithStream(
        @RequestParam memberId: Long,
        @RequestParam targetTechnology: String,
        @RequestParam(required = false) prefersFastPlan: Boolean = false
    ): SseEmitter {
        val emitter = SseEmitter(30 * 60 * 1000L)
        
        CompletableFuture.runAsync({
            try {
                val agentRun = agentRunService.createRun(
                    memberId = memberId,
                    agentType = AgentType.LEARNING_PLAN,
                    parameters = objectMapper.writeValueAsString(
                        mapOf(
                            "targetTechnology" to targetTechnology,
                            "prefersFastPlan" to prefersFastPlan
                        )
                    )
                )

                emitter.send(
                    SseEmitter.event()
                        .name("agent_started")
                        .data(AgentEventDto(
                            type = AGENT_STARTED,
                            agentRunId = agentRun.agentRunId!!,
                            message = "Agent 실행 시작",
                            timestamp = System.currentTimeMillis()
                        ))
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
                    memberId,
                    targetTechnology,
                    prefersFastPlan
                )

                val runId = agentRun.agentRunId!!
                agentRunService.startRun(runId)

                emitter.send(
                    SseEmitter.event()
                        .name("planning_started")
                        .data(AgentEventDto(
                            type = com.bebeis.skillweaver.core.domain.agent.SseEventType.PLANNING_STARTED,
                            agentRunId = runId,
                            message = "GOAP 경로 계획 중...",
                            timestamp = System.currentTimeMillis()
                        ))
                )

                subscribeToProcessEvents(agentProcess, runId, emitter)

                agentPlatform.start(agentProcess)

                val result = agentProcess.last(Any::class.java)
                val resultJson = objectMapper.writeValueAsString(result)

                agentRunService.completeRun(
                    agentRunId = runId,
                    result = resultJson,
                    learningPlanId = null,
                    cost = 0.0,
                    executionTimeMs = System.currentTimeMillis()
                )

                emitter.send(
                    SseEmitter.event()
                        .name("agent_completed")
                        .data(AgentEventDto(
                            type = com.bebeis.skillweaver.core.domain.agent.SseEventType.AGENT_COMPLETED,
                            agentRunId = runId,
                            message = "Agent 실행 완료",
                            result = result,
                            timestamp = System.currentTimeMillis()
                        ))
                )

                emitter.complete()

            } catch (e: Exception) {
                logger.error("Agent 실행 중 오류 발생", e)
                emitter.send(
                    SseEmitter.event()
                        .name("error")
                        .data(AgentEventDto(
                            type = com.bebeis.skillweaver.core.domain.agent.SseEventType.ERROR,
                            message = "오류 발생: ${e.message}",
                            timestamp = System.currentTimeMillis()
                        ))
                )
                emitter.completeWithError(e)
            }
        }, executor)

        emitter.onCompletion { logger.info("SSE 연결 완료") }
        emitter.onTimeout { logger.warn("SSE 연결 타임아웃") }
        emitter.onError { e -> logger.error("SSE 연결 오류", e) }

        return emitter
    }

    private fun subscribeToProcessEvents(
        process: AgentProcess,
        agentRunId: Long,
        emitter: SseEmitter
    ) {
        var lastHistorySize = 0
        var actionStartTime = System.currentTimeMillis()

        Thread {
            while (!process.finished) {
                try {
                    val history = process.history
                    val currentHistorySize = history.size

                    if (currentHistorySize > lastHistorySize) {
                        val newActions = history.subList(lastHistorySize, currentHistorySize)
                        
                        newActions.forEach { action ->
                            val actionName = action::class.simpleName ?: "Unknown"
                            val duration = System.currentTimeMillis() - actionStartTime
                            
                            emitter.send(
                                SseEmitter.event()
                                    .name("action_executed")
                                    .data(AgentEventDto(
                                        type = com.bebeis.skillweaver.core.domain.agent.SseEventType.ACTION_EXECUTED,
                                        agentRunId = agentRunId,
                                        actionName = actionName,
                                        message = "$actionName 실행 완료 (${duration}ms)",
                                        timestamp = System.currentTimeMillis()
                                    ))
                            )
                            
                            actionStartTime = System.currentTimeMillis()
                        }
                        
                        lastHistorySize = currentHistorySize
                    }

                    emitter.send(
                        SseEmitter.event()
                            .name("progress")
                            .data(AgentEventDto(
                                type = com.bebeis.skillweaver.core.domain.agent.SseEventType.PROGRESS,
                                agentRunId = agentRunId,
                                message = "진행 중... (${lastHistorySize}개 액션 완료)",
                                timestamp = System.currentTimeMillis()
                            ))
                    )

                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    logger.error("이벤트 구독 중 오류", e)
                    break
                }
            }
        }.start()
    }
}
