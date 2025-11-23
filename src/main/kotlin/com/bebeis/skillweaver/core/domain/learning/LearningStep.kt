package com.bebeis.skillweaver.core.domain.learning

import jakarta.persistence.*

@Entity
@Table(name = "learning_step")
class LearningStep(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "step_id")
    val stepId: Long? = null,

    @Column(name = "learning_plan_id", nullable = false)
    val learningPlanId: Long,

    @Column(name = "step_order", nullable = false)
    val order: Int,

    @Column(nullable = false, length = 200)
    val title: String,

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(name = "estimated_hours", nullable = false)
    val estimatedHours: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val difficulty: StepDifficulty,

    @Column(nullable = false)
    val completed: Boolean = false,

    @Lob
    @Column(columnDefinition = "TEXT")
    val objectives: String? = null,

    @Lob
    @Column(name = "suggested_resources", columnDefinition = "TEXT")
    val suggestedResources: String? = null
) {
    init {
        require(order > 0) { "순서는 0보다 커야 합니다." }
        require(estimatedHours > 0) { "예상 시간은 0보다 커야 합니다." }
        require(title.isNotBlank()) { "제목은 비어있을 수 없습니다." }
    }
}
