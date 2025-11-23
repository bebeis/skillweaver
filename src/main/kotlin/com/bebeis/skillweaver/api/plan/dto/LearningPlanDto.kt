package com.bebeis.skillweaver.api.plan.dto

import com.bebeis.skillweaver.core.domain.learning.LearningPlan
import com.bebeis.skillweaver.core.domain.learning.LearningPlanStatus
import com.bebeis.skillweaver.core.domain.learning.LearningStep
import com.bebeis.skillweaver.core.domain.learning.StepDifficulty
import java.time.LocalDateTime

/**
 * 학습 계획 응답 DTO
 */
data class LearningPlanResponse(
    val learningPlanId: Long,
    val memberId: Long,
    val targetTechnology: String,
    val totalWeeks: Int,
    val totalHours: Int,
    val status: LearningPlanStatus,
    val progress: Int,
    val backgroundAnalysis: String?,
    val startedAt: LocalDateTime?,
    val steps: List<LearningStepResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(plan: LearningPlan, steps: List<LearningStep>): LearningPlanResponse {
            return LearningPlanResponse(
                learningPlanId = plan.learningPlanId!!,
                memberId = plan.memberId,
                targetTechnology = plan.targetTechnology,
                totalWeeks = plan.totalWeeks,
                totalHours = plan.totalHours,
                status = plan.status,
                progress = plan.progress,
                backgroundAnalysis = plan.backgroundAnalysis,
                startedAt = plan.startedAt,
                steps = steps.map { LearningStepResponse.from(it) },
                createdAt = plan.createdAt,
                updatedAt = plan.updatedAt
            )
        }
    }
}

/**
 * 학습 단계 응답 DTO
 */
data class LearningStepResponse(
    val stepId: Long,
    val learningPlanId: Long,
    val order: Int,
    val title: String,
    val description: String,
    val estimatedHours: Int,
    val difficulty: StepDifficulty,
    val completed: Boolean,
    val objectives: String?,
    val suggestedResources: String?
) {
    companion object {
        fun from(step: LearningStep): LearningStepResponse {
            return LearningStepResponse(
                stepId = step.stepId!!,
                learningPlanId = step.learningPlanId,
                order = step.order,
                title = step.title,
                description = step.description,
                estimatedHours = step.estimatedHours,
                difficulty = step.difficulty,
                completed = step.completed,
                objectives = step.objectives,
                suggestedResources = step.suggestedResources
            )
        }
    }
}

/**
 * 학습 계획 생성 요청 DTO
 */
data class CreateLearningPlanRequest(
    val targetTechnology: String,
    val totalWeeks: Int,
    val totalHours: Int,
    val backgroundAnalysis: String? = null,
    val steps: List<CreateStepRequest>
) {
    init {
        require(targetTechnology.isNotBlank()) { "목표 기술은 필수입니다" }
        require(totalWeeks > 0) { "총 주수는 0보다 커야 합니다" }
        require(totalHours > 0) { "총 시간은 0보다 커야 합니다" }
        require(steps.isNotEmpty()) { "최소 1개 이상의 학습 단계가 필요합니다" }
    }
}

/**
 * 학습 단계 생성 요청 DTO
 */
data class CreateStepRequest(
    val title: String,
    val description: String,
    val estimatedHours: Int,
    val difficulty: StepDifficulty,
    val objectives: String? = null,
    val suggestedResources: String? = null
) {
    init {
        require(title.isNotBlank()) { "단계 제목은 필수입니다" }
        require(description.isNotBlank()) { "단계 설명은 필수입니다" }
        require(estimatedHours > 0) { "예상 시간은 0보다 커야 합니다" }
    }
}

/**
 * 학습 계획 상태 수정 요청 DTO
 */
data class UpdatePlanStatusRequest(
    val status: LearningPlanStatus
)

/**
 * 학습 진행률 응답 DTO
 */
data class PlanProgressResponse(
    val planId: Long,
    val targetTechnology: String,
    val totalSteps: Int,
    val completedSteps: Int,
    val progress: Int,
    val status: LearningPlanStatus,
    val steps: List<StepProgressResponse>
)

/**
 * 단계별 진행률 응답 DTO
 */
data class StepProgressResponse(
    val stepId: Long,
    val order: Int,
    val title: String,
    val completed: Boolean,
    val estimatedHours: Int,
    val difficulty: StepDifficulty
) {
    companion object {
        fun from(step: LearningStep): StepProgressResponse {
            return StepProgressResponse(
                stepId = step.stepId!!,
                order = step.order,
                title = step.title,
                completed = step.completed,
                estimatedHours = step.estimatedHours,
                difficulty = step.difficulty
            )
        }
    }
}
