package com.bebeis.skillweaver.api.agent.dto

data class CreateLearningPlanWithAgentRequest(
    val targetTechnologyKey: String
)

data class AgentLearningPlanResponse(
    val planId: Long,
    val title: String,
    val targetTechnology: String,
    val description: String,
    val totalEstimatedHours: Int,
    val totalSteps: Int,
    val startDate: String,
    val targetEndDate: String,
    val generationMetadata: GenerationMetadataResponse
)

data class GenerationMetadataResponse(
    val generatedPath: String, // "QUICK", "STANDARD", "DETAILED"
    val llmModel: String,
    val estimatedCost: Double,
    val generationTimeSeconds: Int,
    val analysisDepth: String,
    val gapAnalysisPerformed: Boolean,
    val resourcesEnriched: Boolean
)
