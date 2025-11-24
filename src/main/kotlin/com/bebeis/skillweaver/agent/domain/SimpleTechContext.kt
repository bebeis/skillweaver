package com.bebeis.skillweaver.agent.domain

import org.springframework.ai.tool.annotation.Tool

data class SimpleTechContext(
    val technologyKey: String,
    val displayName: String,
    val category: String,
    val briefDescription: String,
    val estimatedLearningWeeks: Int,
    val difficultyLevel: String // "EASY", "MEDIUM", "HARD"
) {
    @Tool(description = "Get the full display name of the technology")
    fun getFullDisplayName(): String = "$displayName ($technologyKey)"

    @Tool(description = "Check if this is a difficult technology to learn")
    fun isDifficultTech(): Boolean = difficultyLevel.equals("HARD", ignoreCase = true)

    @Tool(description = "Check if this is an easy technology to learn")
    fun isEasyTech(): Boolean = difficultyLevel.equals("EASY", ignoreCase = true)

    @Tool(description = "Check if learning this technology requires long-term commitment (>8 weeks)")
    fun isLongTermLearning(): Boolean = estimatedLearningWeeks > 8

    @Tool(description = "Get estimated learning time in months")
    fun getEstimatedMonths(): Double = estimatedLearningWeeks / 4.0
}
