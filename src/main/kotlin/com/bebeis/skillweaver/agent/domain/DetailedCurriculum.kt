package com.bebeis.skillweaver.agent.domain

data class DetailedCurriculum(
    val steps: List<StepBlueprint>
) {
    init {
        require(steps.size in 8..12) { "Detailed curriculum must have 8-12 steps, got ${steps.size}" }
    }
}
