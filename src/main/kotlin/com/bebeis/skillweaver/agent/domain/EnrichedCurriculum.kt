package com.bebeis.skillweaver.agent.domain

data class EnrichedCurriculum(
    val steps: List<EnrichedStep>
)

data class EnrichedStep(
    val order: Int,
    val title: String,
    val description: String,
    val estimatedHours: Int,
    val prerequisites: List<String>,
    val keyTopics: List<String>,
    val learningResources: List<LearningResource>
)

data class LearningResource(
    val type: String,
    val title: String,
    val url: String?,
    val description: String
)
