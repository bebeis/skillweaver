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

    @ElementCollection
    @CollectionTable(
        name = "learning_step_objective",
        joinColumns = [JoinColumn(name = "step_id")]
    )
    @Column(name = "objective", columnDefinition = "TEXT", nullable = false)
    val objectives: List<String> = emptyList(),

    @ElementCollection
    @CollectionTable(
        name = "learning_step_resource",
        joinColumns = [JoinColumn(name = "step_id")]
    )
    val suggestedResources: List<StepResource> = emptyList()
) {
    init {
        require(order > 0) { "순서는 0보다 커야 합니다." }
        require(estimatedHours > 0) { "예상 시간은 0보다 커야 합니다." }
        require(title.isNotBlank()) { "제목은 비어있을 수 없습니다." }
    }
}

@Embeddable
data class StepResource(
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 30)
    val type: ResourceType,

    @Column(name = "title", nullable = false, length = 200)
    val title: String,

    @Column(name = "url", nullable = false, length = 500)
    val url: String,

    @Column(name = "language", length = 20)
    val language: String? = null
)
