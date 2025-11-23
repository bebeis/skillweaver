package com.bebeis.skillweaver.agent.domain

data class StandardCurriculum(
    val steps: List<StepBlueprint>
) {
    init {
        require(steps.size in 5..7) { "Standard curriculum must have 5-7 steps, got ${steps.size}" }
    }
}
