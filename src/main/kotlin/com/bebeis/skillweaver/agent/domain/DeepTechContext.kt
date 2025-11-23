package com.bebeis.skillweaver.agent.domain

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
)
