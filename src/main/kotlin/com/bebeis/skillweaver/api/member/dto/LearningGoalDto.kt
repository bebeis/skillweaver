package com.bebeis.skillweaver.api.member.dto

import com.bebeis.skillweaver.core.domain.member.goal.GoalPriority
import com.bebeis.skillweaver.core.domain.member.goal.GoalStatus
import com.bebeis.skillweaver.core.domain.member.goal.LearningGoal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 학습 목표 응답 DTO (V5 확장)
 */
data class LearningGoalResponse(
    val learningGoalId: Long,
    val title: String,
    val description: String,
    val dueDate: LocalDate?,
    val priority: GoalPriority,
    val status: GoalStatus,
    // V5: Plan-Goal 연동 필드
    val learningPlanId: Long? = null,
    val totalSteps: Int? = null,
    val completedSteps: Int = 0,
    val progressPercentage: Int = 0,
    // V5 Phase 4: 스트릭 필드
    val streakInfo: StreakInfo? = null,
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
                // V5 필드
                learningPlanId = goal.learningPlanId,
                totalSteps = goal.totalSteps,
                completedSteps = goal.completedSteps,
                progressPercentage = goal.progressPercentage,
                // Phase 4 스트릭
                streakInfo = if (goal.learningPlanId != null) {
                    StreakInfo(
                        currentStreak = goal.currentStreak,
                        longestStreak = goal.longestStreak,
                        lastStudyDate = goal.lastStudyDate
                    )
                } else null,
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

// =========================================================================
// Phase 4: 학습 스트릭 DTO
// =========================================================================

/**
 * 스트릭 정보 (LearningGoalResponse 내장)
 */
data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastStudyDate: LocalDate?
)

/**
 * 스트릭 상세 조회 응답
 * GET /api/v1/members/{memberId}/goals/{goalId}/streak
 */
data class StreakResponse(
    val learningGoalId: Long,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastStudyDate: LocalDate?,
    val isActiveToday: Boolean,
    val streakStatus: StreakStatus,
    val message: String
)

enum class StreakStatus {
    ACTIVE,      // 오늘 학습 완료
    AT_RISK,     // 오늘 학습 안 함 (끊기기 직전)
    BROKEN,      // 스트릭 끊어짐
    NEW          // 스트릭 시작 전
}

// =========================================================================
// Phase 4: 학습 리포트 DTO
// =========================================================================

/**
 * 주간 리포트 응답
 * GET /api/v1/members/{memberId}/goals/{goalId}/reports/weekly
 */
data class WeeklyReportResponse(
    val learningGoalId: Long,
    val weekStartDate: LocalDate,
    val weekEndDate: LocalDate,
    val completedSteps: Int,
    val totalLearningHours: Int,
    val learningDays: Int,
    val averageDailyHours: Double,
    val progressChange: Int,  // 주간 진행률 변화 (%)
    val milestones: List<MilestoneAchievement>,
    val comparisonWithLastWeek: ComparisonInfo?
)

/**
 * 월간 리포트 응답
 * GET /api/v1/members/{memberId}/goals/{goalId}/reports/monthly
 */
data class MonthlyReportResponse(
    val learningGoalId: Long,
    val month: String,  // "2025-12"
    val completedSteps: Int,
    val totalLearningHours: Int,
    val learningDays: Int,
    val averageDailyHours: Double,
    val longestStreakInMonth: Int,
    val progressChange: Int,  // 월간 진행률 변화 (%)
    val milestones: List<MilestoneAchievement>,
    val weeklyBreakdown: List<WeeklySummary>
)

data class MilestoneAchievement(
    val milestone: String,
    val achievedAt: LocalDateTime?,
    val achieved: Boolean
)

data class ComparisonInfo(
    val stepsChange: Int,
    val hoursChange: Int,
    val daysChange: Int
)

data class WeeklySummary(
    val weekNumber: Int,
    val completedSteps: Int,
    val learningHours: Int,
    val learningDays: Int
)

// =========================================================================
// 기존 DTO (유지)
// =========================================================================

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

