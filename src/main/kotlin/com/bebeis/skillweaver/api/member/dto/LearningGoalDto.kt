package com.bebeis.skillweaver.api.member.dto

import com.bebeis.skillweaver.core.domain.member.goal.GoalPriority
import com.bebeis.skillweaver.core.domain.member.goal.GoalStatus
import com.bebeis.skillweaver.core.domain.member.goal.LearningGoal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 학습 목표 응답 DTO
 */
data class LearningGoalResponse(
    val learningGoalId: Long,
    val title: String,
    val description: String,
    val dueDate: LocalDate?,
    val priority: GoalPriority,
    val status: GoalStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(goal: LearningGoal): LearningGoalResponse {
            return LearningGoalResponse(
                learningGoalId = goal.learningGoalId!!,
                title = goal.title,
                description = goal.description,
                dueDate = goal.dueDate,
                priority = goal.priority,
                status = goal.status,
                createdAt = goal.createdAt,
                updatedAt = goal.updatedAt
            )
        }
    }
}

data class LearningGoalListResponse(
    val goals: List<LearningGoalResponse>,
    val totalCount: Int
)

/**
 * 학습 목표 생성 요청 DTO
 */
data class CreateLearningGoalRequest(
    val title: String,
    val description: String,
    val dueDate: LocalDate? = null,
    val priority: GoalPriority = GoalPriority.MEDIUM
) {
    init {
        require(title.isNotBlank()) { "목표 제목은 필수입니다" }
        require(description.isNotBlank()) { "목표 설명은 필수입니다" }
    }
}

/**
 * 학습 목표 수정 요청 DTO
 */
data class UpdateLearningGoalRequest(
    val title: String? = null,
    val description: String? = null,
    val dueDate: LocalDate? = null,
    val priority: GoalPriority? = null,
    val status: GoalStatus? = null
) {
    init {
        title?.let { require(it.isNotBlank()) { "목표 제목은 비어있을 수 없습니다" } }
        description?.let { require(it.isNotBlank()) { "목표 설명은 비어있을 수 없습니다" } }
    }
}
