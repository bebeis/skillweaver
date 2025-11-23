package com.bebeis.skillweaver.agent.domain

data class SimpleTechContext(
    val technologyKey: String,
    val displayName: String,
    val category: String,
    val briefDescription: String,
    val estimatedLearningWeeks: Int,
    val difficultyLevel: String // "EASY", "MEDIUM", "HARD"
)
