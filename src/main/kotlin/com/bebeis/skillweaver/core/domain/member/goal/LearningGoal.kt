package com.bebeis.skillweaver.core.domain.member.goal

import com.bebeis.skillweaver.core.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.LocalDate


@Entity
@Table(name = "learning_goal")
class LearningGoal(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_goal_id")
    val learningGoalId: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(nullable = false, length = 200)
    val title: String,

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(name = "due_date")
    val dueDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val priority: GoalPriority = GoalPriority.MEDIUM,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: GoalStatus = GoalStatus.ACTIVE,

    // === V5 추가 필드: Plan-Goal 연동 ===
    @Column(name = "learning_plan_id")
    val learningPlanId: Long? = null,  // FK to learning_plan

    @Column(name = "total_steps")
    val totalSteps: Int? = null,  // 전체 스텝 개수

    @Column(name = "completed_steps")
    val completedSteps: Int = 0,  // 완료된 스텝 개수

    @Column(name = "progress_percentage")
    val progressPercentage: Int = 0,  // 진행률 (0-100)

    // === Phase 4 추가 필드: 학습 스트릭 ===
    @Column(name = "current_streak")
    val currentStreak: Int = 0,  // 현재 연속 학습일수

    @Column(name = "longest_streak")
    val longestStreak: Int = 0,  // 최장 연속 학습일수

    @Column(name = "last_study_date")
    val lastStudyDate: LocalDate? = null  // 마지막 학습 날짜
) : BaseEntity() {
    init {
        require(title.isNotBlank()) { "제목은 비어있을 수 없습니다." }
        require(description.isNotBlank()) { "설명은 비어있을 수 없습니다." }
        require(progressPercentage in 0..100) { "진행률은 0~100 사이여야 합니다." }
        require(completedSteps >= 0) { "완료된 스텝은 0 이상이어야 합니다." }
        totalSteps?.let { require(it >= 0) { "전체 스텝은 0 이상이어야 합니다." } }
        totalSteps?.let { require(completedSteps <= it) { "완료된 스텝은 전체 스텝보다 클 수 없습니다." } }
        require(currentStreak >= 0) { "현재 스트릭은 0 이상이어야 합니다." }
        require(longestStreak >= 0) { "최장 스트릭은 0 이상이어야 합니다." }
    }

    fun isActive(): Boolean {
        return dueDate == null || !dueDate!!.isBefore(LocalDate.now())
    }
}
