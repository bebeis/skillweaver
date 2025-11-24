package com.bebeis.skillweaver.core.service.learning

import com.bebeis.skillweaver.agent.domain.DailyScheduleItem
import com.bebeis.skillweaver.agent.domain.GeneratedLearningPlan
import com.bebeis.skillweaver.api.common.dto.PaginationResponse
import com.bebeis.skillweaver.api.common.exception.ErrorCode
import com.bebeis.skillweaver.api.common.exception.notFound
import com.bebeis.skillweaver.api.plan.dto.*
import com.bebeis.skillweaver.api.plan.dto.toStepResource
import com.bebeis.skillweaver.core.domain.learning.LearningPlan
import com.bebeis.skillweaver.core.domain.learning.LearningPlanStatus
import com.bebeis.skillweaver.core.domain.learning.LearningStep
import com.bebeis.skillweaver.core.domain.learning.StepDifficulty
import com.bebeis.skillweaver.core.domain.learning.StepResource
import com.bebeis.skillweaver.core.storage.learning.LearningPlanRepository
import com.bebeis.skillweaver.core.storage.learning.LearningStepRepository
import com.bebeis.skillweaver.core.storage.member.MemberRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class LearningPlanService(
    private val learningPlanRepository: LearningPlanRepository,
    private val learningStepRepository: LearningStepRepository,
    private val memberRepository: MemberRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(LearningPlanService::class.java)

    @Transactional
    fun createPlan(memberId: Long, request: CreateLearningPlanRequest): LearningPlanResponse {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        val plan = LearningPlan(
            memberId = memberId,
            targetTechnology = request.targetTechnology,
            totalWeeks = request.totalWeeks,
            totalHours = request.totalHours,
            status = LearningPlanStatus.ACTIVE,
            progress = 0,
            backgroundAnalysis = request.backgroundAnalysis,
            dailySchedule = serializeDailySchedule(request.dailySchedule),
            startedAt = LocalDateTime.now()
        )

        val savedPlan = learningPlanRepository.save(plan)
        logger.info("Learning plan created: ${savedPlan.learningPlanId}")

        val steps = request.steps.mapIndexed { index, stepRequest ->
            LearningStep(
                learningPlanId = savedPlan.learningPlanId!!,
                order = index + 1,
                title = stepRequest.title,
                description = stepRequest.description,
                estimatedHours = stepRequest.estimatedHours,
                difficulty = stepRequest.difficulty,
                completed = false,
                objectives = stepRequest.objectives,
                suggestedResources = stepRequest.suggestedResources.map { it.toStepResource() }
            )
        }

        val savedSteps = learningStepRepository.saveAll(steps)
        logger.info("${savedSteps.size} learning steps created for plan ${savedPlan.learningPlanId}")

        return LearningPlanResponse.from(savedPlan, savedSteps, objectMapper)
    }

    fun getPlans(
        memberId: Long,
        status: LearningPlanStatus?,
        page: Int,
        size: Int
    ): LearningPlanListResponse {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceAtLeast(1))
        val planPage = if (status != null) {
            learningPlanRepository.findByMemberIdAndStatus(memberId, status, pageable)
        } else {
            learningPlanRepository.findByMemberId(memberId, pageable)
        }

        return LearningPlanListResponse(
            plans = planPage.content.map { LearningPlanSummaryResponse.from(it) },
            pagination = PaginationResponse(
                page = planPage.number,
                size = planPage.size,
                totalElements = planPage.totalElements,
                totalPages = planPage.totalPages
            )
        )
    }

    fun getPlanById(memberId: Long, planId: Long): LearningPlanResponse {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        val plan = learningPlanRepository.findById(planId).orElse(null)
            ?: notFound(ErrorCode.LEARNING_PLAN_NOT_FOUND)

        if (plan.memberId != memberId) {
            notFound(ErrorCode.LEARNING_PLAN_NOT_FOUND)
        }

        val steps = learningStepRepository.findByLearningPlanIdOrderByOrder(planId)
        return LearningPlanResponse.from(plan, steps, objectMapper)
    }

    @Transactional
    fun updatePlanStatus(memberId: Long, planId: Long, request: UpdatePlanStatusRequest): LearningPlanResponse {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        val plan = learningPlanRepository.findById(planId).orElse(null)
            ?: notFound(ErrorCode.LEARNING_PLAN_NOT_FOUND)

        if (plan.memberId != memberId) {
            notFound(ErrorCode.LEARNING_PLAN_NOT_FOUND)
        }

        val updated = LearningPlan(
            learningPlanId = plan.learningPlanId,
            memberId = plan.memberId,
            targetTechnology = plan.targetTechnology,
            totalWeeks = plan.totalWeeks,
            totalHours = plan.totalHours,
            status = request.status,
            progress = plan.progress,
            backgroundAnalysis = plan.backgroundAnalysis,
            dailySchedule = plan.dailySchedule,
            startedAt = plan.startedAt
        )

        val saved = learningPlanRepository.save(updated)
        logger.info("Learning plan status updated: ${saved.learningPlanId} -> ${request.status}")

        val steps = learningStepRepository.findByLearningPlanIdOrderByOrder(planId)
        return LearningPlanResponse.from(saved, steps, objectMapper)
    }

    @Transactional
    fun completeStep(memberId: Long, planId: Long, stepOrder: Int): LearningPlanResponse {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        val plan = learningPlanRepository.findById(planId).orElse(null)
            ?: notFound(ErrorCode.LEARNING_PLAN_NOT_FOUND)

        if (plan.memberId != memberId) {
            notFound(ErrorCode.LEARNING_PLAN_NOT_FOUND)
        }

        val step = learningStepRepository.findByLearningPlanIdAndOrder(planId, stepOrder)
            ?: notFound(ErrorCode.LEARNING_STEP_NOT_FOUND)

        val updatedStep = LearningStep(
            stepId = step.stepId,
            learningPlanId = step.learningPlanId,
            order = step.order,
            title = step.title,
            description = step.description,
            estimatedHours = step.estimatedHours,
            difficulty = step.difficulty,
            completed = true,
            objectives = step.objectives,
            suggestedResources = step.suggestedResources
        )

        learningStepRepository.save(updatedStep)
        logger.info("Learning step completed: plan=$planId, stepOrder=$stepOrder")

        val totalSteps = learningStepRepository.countByLearningPlanId(planId)
        val completedSteps = learningStepRepository.countByLearningPlanIdAndCompleted(planId, true)
        val newProgress = ((completedSteps.toDouble() / totalSteps) * 100).toInt()

        val updatedPlan = LearningPlan(
            learningPlanId = plan.learningPlanId,
            memberId = plan.memberId,
            targetTechnology = plan.targetTechnology,
            totalWeeks = plan.totalWeeks,
            totalHours = plan.totalHours,
            status = if (newProgress == 100) LearningPlanStatus.COMPLETED else plan.status,
            progress = newProgress,
            backgroundAnalysis = plan.backgroundAnalysis,
            dailySchedule = plan.dailySchedule,
            startedAt = plan.startedAt
        )

        val savedPlan = learningPlanRepository.save(updatedPlan)
        logger.info("Learning plan progress updated: ${savedPlan.learningPlanId} -> $newProgress%")

        val steps = learningStepRepository.findByLearningPlanIdOrderByOrder(planId)
        return LearningPlanResponse.from(savedPlan, steps, objectMapper)
    }

    @Transactional
    fun deletePlan(memberId: Long, planId: Long) {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        val plan = learningPlanRepository.findById(planId).orElse(null)
            ?: notFound(ErrorCode.LEARNING_PLAN_NOT_FOUND)

        if (plan.memberId != memberId) {
            notFound(ErrorCode.LEARNING_PLAN_NOT_FOUND)
        }

        val steps = learningStepRepository.findByLearningPlanIdOrderByOrder(planId)
        learningStepRepository.deleteAll(steps)
        learningPlanRepository.delete(plan)
        logger.info("Learning plan deleted: $planId")
    }

    fun getProgress(memberId: Long, planId: Long): PlanProgressResponse {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        val plan = learningPlanRepository.findById(planId).orElse(null)
            ?: notFound(ErrorCode.LEARNING_PLAN_NOT_FOUND)

        if (plan.memberId != memberId) {
            notFound(ErrorCode.LEARNING_PLAN_NOT_FOUND)
        }

        val totalSteps = learningStepRepository.countByLearningPlanId(planId)
        val completedSteps = learningStepRepository.countByLearningPlanIdAndCompleted(planId, true)
        val steps = learningStepRepository.findByLearningPlanIdOrderByOrder(planId)

        return PlanProgressResponse(
            planId = planId,
            targetTechnology = plan.targetTechnology,
            totalSteps = totalSteps,
            completedSteps = completedSteps,
            progress = plan.progress,
            status = plan.status,
            steps = steps.map { StepProgressResponse.from(it) }
        )
    }

    private fun serializeDailySchedule(dailySchedule: List<DailyScheduleItemRequest>): String? {
        if (dailySchedule.isEmpty()) return null
        return objectMapper.writeValueAsString(dailySchedule)
    }
    
    private fun serializeBackgroundAnalysis(generatedPlan: GeneratedLearningPlan): String? {
        return generatedPlan.backgroundAnalysis?.let { objectMapper.writeValueAsString(it) }
            ?: generatedPlan.description
    }

    private fun serializeAgentDailySchedule(dailySchedule: List<DailyScheduleItem>): String? {
        if (dailySchedule.isEmpty()) return null
        return objectMapper.writeValueAsString(dailySchedule)
    }

    @Transactional
    fun updateProgress(
        memberId: Long,
        planId: Long,
        request: UpdatePlanProgressRequest
    ): PlanProgressUpdateResponse {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        val plan = learningPlanRepository.findById(planId).orElse(null)
            ?: notFound(ErrorCode.LEARNING_PLAN_NOT_FOUND)

        if (plan.memberId != memberId) {
            notFound(ErrorCode.LEARNING_PLAN_NOT_FOUND)
        }

        val updatedPlan = LearningPlan(
            learningPlanId = plan.learningPlanId,
            memberId = plan.memberId,
            targetTechnology = plan.targetTechnology,
            totalWeeks = plan.totalWeeks,
            totalHours = plan.totalHours,
            status = request.status,
            progress = request.progress,
            backgroundAnalysis = plan.backgroundAnalysis,
            dailySchedule = plan.dailySchedule,
            startedAt = plan.startedAt
        )

        val saved = learningPlanRepository.save(updatedPlan)
        logger.info("Learning plan progress manually updated: ${saved.learningPlanId} -> ${request.progress}%")

        return PlanProgressUpdateResponse(
            learningPlanId = saved.learningPlanId!!,
            progress = saved.progress,
            status = saved.status,
            updatedAt = saved.updatedAt
        )
    }
    
    @Transactional
    fun createPlanFromAgent(generatedPlan: GeneratedLearningPlan): LearningPlan {
        if (!memberRepository.existsById(generatedPlan.memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }
        
        val totalWeeks = ChronoUnit.WEEKS.between(
            generatedPlan.startDate,
            generatedPlan.targetEndDate
        ).toInt().coerceAtLeast(1)
        
        val plan = LearningPlan(
            memberId = generatedPlan.memberId,
            targetTechnology = generatedPlan.targetTechnologyName,
            totalWeeks = totalWeeks,
            totalHours = generatedPlan.totalEstimatedHours,
            status = LearningPlanStatus.ACTIVE,
            progress = 0,
            backgroundAnalysis = serializeBackgroundAnalysis(generatedPlan),
            dailySchedule = serializeAgentDailySchedule(generatedPlan.dailySchedule),
            startedAt = generatedPlan.startDate.atStartOfDay()
        )
        
        val savedPlan = learningPlanRepository.save(plan)
        logger.info("Agent-generated learning plan saved: ${savedPlan.learningPlanId}")
        
        val steps = generatedPlan.steps.map { generatedStep ->
            LearningStep(
                learningPlanId = savedPlan.learningPlanId!!,
                order = generatedStep.order,
                title = generatedStep.title,
                description = generatedStep.description,
                estimatedHours = generatedStep.estimatedHours,
                difficulty = estimateDifficulty(generatedStep.estimatedHours),
                completed = false,
                objectives = generatedStep.keyTopics,
                suggestedResources = generatedStep.resources.map {
                    StepResource(
                        type = it.type,
                        title = it.title,
                        url = it.url,
                        language = it.language
                    )
                }
            )
        }
        
        val savedSteps = learningStepRepository.saveAll(steps)
        logger.info("${savedSteps.size} agent-generated learning steps saved for plan ${savedPlan.learningPlanId}")
        
        return savedPlan
    }
    
    private fun estimateDifficulty(estimatedHours: Int): StepDifficulty {
        return when {
            estimatedHours <= 8 -> StepDifficulty.EASY
            estimatedHours <= 16 -> StepDifficulty.MEDIUM
            else -> StepDifficulty.HARD
        }
    }
}
