package com.bebeis.skillweaver.core.domain.learning

import com.bebeis.skillweaver.core.domain.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "learning_plan")
class LearningPlan(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_plan_id")
    val learningPlanId: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "target_technology", nullable = false, length = 200)
    val targetTechnology: String,

    @Column(name = "total_weeks", nullable = false)
    val totalWeeks: Int,

    @Column(name = "total_hours", nullable = false)
    val totalHours: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: LearningPlanStatus = LearningPlanStatus.ACTIVE,

    @Column(nullable = false)
    val progress: Int = 0,

    @Lob
    @Column(name = "background_analysis", columnDefinition = "TEXT")
    val backgroundAnalysis: String? = null,

    @Lob
    @Column(name = "daily_schedule", columnDefinition = "TEXT")
    val dailySchedule: String? = null,

    @Column(name = "started_at")
    val startedAt: LocalDateTime? = null
) : BaseEntity() {
    init {
        require(totalWeeks > 0) { "총 주수는 0보다 커야 합니다." }
        require(totalHours > 0) { "총 시간은 0보다 커야 합니다." }
        require(progress in 0..100) { "진행도는 0~100 사이여야 합니다." }
    }
}
