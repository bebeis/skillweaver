package com.bebeis.skillweaver.agent.domain

data class BasicCurriculum(
    val steps: List<StepBlueprint>
) {
    init {
        require(steps.size in 3..4) { "BasicCurriculum must have 3-4 steps, got ${steps.size}" }
    }
}

data class StepBlueprint(
    val order: Int,
    val title: String,
    val description: String,
    val estimatedHours: Int,
    val prerequisites: List<String> = emptyList(),
    val keyTopics: List<String> = emptyList()
)
