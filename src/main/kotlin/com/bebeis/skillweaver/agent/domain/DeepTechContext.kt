package com.bebeis.skillweaver.agent.domain

import org.springframework.ai.tool.annotation.Tool

data class DeepTechContext(
    val technologyKey: String,
    val displayName: String,
    val category: String,
    val detailedDescription: String,
    val ecosystem: String,
    val prerequisites: List<String>,
    val relatedTechnologies: List<String>,
    val commonUseCases: List<String>,
    val estimatedLearningWeeks: Int,
    val difficultyLevel: String,
    val marketDemand: String
) {
    @Tool(description = "Get the full display name of the technology")
    fun getFullDisplayName(): String = "$displayName ($technologyKey)"

    @Tool(description = "Check if this technology has prerequisites")
    fun hasPrerequisites(): Boolean = prerequisites.isNotEmpty()

    @Tool(description = "Get the number of prerequisites required")
    fun getPrerequisiteCount(): Int = prerequisites.size

    @Tool(description = "Check if this technology is in the JVM ecosystem")
    fun isJvmEcosystem(): Boolean = ecosystem.contains("JVM", ignoreCase = true) || ecosystem.contains("Java", ignoreCase = true)

    @Tool(description = "Check if this is a difficult technology to learn")
    fun isDifficultTech(): Boolean = difficultyLevel.equals("HARD", ignoreCase = true)

    @Tool(description = "Check if this technology has high market demand")
    fun hasHighMarketDemand(): Boolean = marketDemand.contains("high", ignoreCase = true)

    @Tool(description = "Check if learning this technology requires long-term commitment (>8 weeks)")
    fun isLongTermLearning(): Boolean = estimatedLearningWeeks > 8

    @Tool(description = "Get estimated learning time in months")
    fun getEstimatedMonths(): Double = estimatedLearningWeeks / 4.0

    @Tool(description = "Check if this technology has multiple use cases")
    fun hasMultipleUseCases(): Boolean = commonUseCases.size > 1
}
