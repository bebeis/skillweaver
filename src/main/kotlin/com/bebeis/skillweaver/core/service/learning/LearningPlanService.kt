package com.bebeis.skillweaver.core.service.learning

import com.bebeis.skillweaver.agent.domain.GeneratedLearningPlan
import com.bebeis.skillweaver.api.common.exception.ErrorCode
import com.bebeis.skillweaver.api.common.exception.notFound
import com.bebeis.skillweaver.api.plan.dto.*
import com.bebeis.skillweaver.core.domain.learning.LearningPlan
import com.bebeis.skillweaver.core.domain.learning.LearningPlanStatus
import com.bebeis.skillweaver.core.domain.learning.LearningStep
import com.bebeis.skillweaver.core.domain.learning.StepDifficulty
import com.bebeis.skillweaver.core.storage.learning.LearningPlanRepository
import com.bebeis.skillweaver.core.storage.learning.LearningStepRepository
import com.bebeis.skillweaver.core.storage.member.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class LearningPlanService(
    private val learningPlanRepository: LearningPlanRepository,
    private val learningStepRepository: LearningStepRepository,
    private val memberRepository: MemberRepository
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
                suggestedResources = stepRequest.suggestedResources
            )
        }

        val savedSteps = learningStepRepository.saveAll(steps)
        logger.info("${savedSteps.size} learning steps created for plan ${savedPlan.learningPlanId}")

        return LearningPlanResponse.from(savedPlan, savedSteps)
    }

    fun getPlansByMemberId(memberId: Long): List<LearningPlanResponse> {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        return learningPlanRepository.findByMemberId(memberId).map { plan ->
            val steps = learningStepRepository.findByLearningPlanIdOrderByOrder(plan.learningPlanId!!)
            LearningPlanResponse.from(plan, steps)
        }
    }

    fun getPlansByStatus(memberId: Long, status: LearningPlanStatus): List<LearningPlanResponse> {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        return learningPlanRepository.findByMemberIdAndStatus(memberId, status).map { plan ->
            val steps = learningStepRepository.findByLearningPlanIdOrderByOrder(plan.learningPlanId!!)
            LearningPlanResponse.from(plan, steps)
        }
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
        return LearningPlanResponse.from(plan, steps)
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
            startedAt = plan.startedAt
        )

        val saved = learningPlanRepository.save(updated)
        logger.info("Learning plan status updated: ${saved.learningPlanId} -> ${request.status}")

        val steps = learningStepRepository.findByLearningPlanIdOrderByOrder(planId)
        return LearningPlanResponse.from(saved, steps)
    }

    @Transactional
    fun completeStep(memberId: Long, planId: Long, stepId: Long): LearningPlanResponse {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        val plan = learningPlanRepository.findById(planId).orElse(null)
            ?: notFound(ErrorCode.LEARNING_PLAN_NOT_FOUND)

        if (plan.memberId != memberId) {
            notFound(ErrorCode.LEARNING_PLAN_NOT_FOUND)
        }

        val step = learningStepRepository.findById(stepId).orElse(null)
            ?: notFound(ErrorCode.LEARNING_STEP_NOT_FOUND)

        if (step.learningPlanId != planId) {
            notFound(ErrorCode.LEARNING_STEP_NOT_FOUND)
        }

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
        logger.info("Learning step completed: $stepId")

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
            startedAt = plan.startedAt
        )

        val savedPlan = learningPlanRepository.save(updatedPlan)
        logger.info("Learning plan progress updated: ${savedPlan.learningPlanId} -> $newProgress%")

        val steps = learningStepRepository.findByLearningPlanIdOrderByOrder(planId)
        return LearningPlanResponse.from(savedPlan, steps)
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
            backgroundAnalysis = generatedPlan.description,
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
                objectives = generatedStep.keyTopics.joinToString(", "),
                suggestedResources = generatedStep.resources.joinToString(", ")
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
