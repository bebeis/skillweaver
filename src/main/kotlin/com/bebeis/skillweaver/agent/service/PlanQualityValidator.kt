package com.bebeis.skillweaver.agent.service

import com.bebeis.skillweaver.agent.domain.GeneratedLearningPlan
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.common.ai.model.LlmOptions
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 학습 계획 품질 검증 서비스
 * LLM-as-Judge 패턴을 사용하여 생성된 계획의 품질을 자동 평가
 */
@Service
class PlanQualityValidator {
    
    private val logger = LoggerFactory.getLogger(PlanQualityValidator::class.java)
    
    private val evaluatorLlm = LlmOptions(
        model = "gpt-4.1-mini",
        temperature = 0.2 // 일관된 평가를 위해 낮은 temperature
    )
    
    /**
     * 생성된 학습 계획을 평가합니다.
     */
    fun validatePlan(
        plan: GeneratedLearningPlan,
        context: OperationContext
    ): ValidationResult {
        logger.info("Validating learning plan for: {}", plan.targetTechnologyName)
        
        val prompt = buildValidationPrompt(plan)
        
        return try {
            context.ai()
                .withLlm(evaluatorLlm)
                .create<ValidationResult>(prompt)
        } catch (e: Exception) {
            logger.error("Plan validation failed", e)
            ValidationResult(
                overallScore = 0.0,
                logicalCoherence = ScoreWithFeedback(0, "Validation failed"),
                timeEstimateRealism = ScoreWithFeedback(0, "Validation failed"),
                resourceAppropriateness = ScoreWithFeedback(0, "Validation failed"),
                goalAchievability = ScoreWithFeedback(0, "Validation failed"),
                suggestions = listOf("Plan validation encountered an error: ${e.message}"),
                passesMinimumQuality = false
            )
        }
    }
    
    private fun buildValidationPrompt(plan: GeneratedLearningPlan): String {
        val stepsInfo = plan.steps.mapIndexed { index, step ->
            """
            Step ${index + 1}: ${step.title}
            - Description: ${step.description}
            - Estimated Hours: ${step.estimatedHours}
            - Topics: ${step.keyTopics.joinToString(", ")}
            - Resources: ${step.resources.size} items
            """.trimIndent()
        }.joinToString("\n\n")
        
        return """
            You are a learning curriculum quality evaluator. Assess the following learning plan objectively.
            
            === Learning Plan to Evaluate ===
            Title: ${plan.title}
            Target Technology: ${plan.targetTechnologyName}
            Total Estimated Hours: ${plan.totalEstimatedHours}
            Duration: ${plan.startDate} to ${plan.targetEndDate}
            
            Steps:
            $stepsInfo
            
            === Evaluation Criteria ===
            Rate each criterion from 1 to 5:
            
            1. Logical Coherence (1-5): Do the steps follow a logical learning progression?
               - Prerequisites are respected
               - Difficulty increases gradually
               - Topics build on previous knowledge
               
            2. Time Estimate Realism (1-5): Are the time estimates realistic?
               - Hours per step are reasonable
               - Total hours match the technology complexity
               - Considers learner's experience level
               
            3. Resource Appropriateness (1-5): Are the resources and topics suitable?
               - Topics are specific and actionable
               - Coverage matches learning goals
               - Mix of theory and practice is balanced
               
            4. Goal Achievability (1-5): Can a learner realistically achieve the goal?
               - Clear milestones exist
               - Scope is manageable
               - Success criteria are implicit
            
            === Output Format ===
            Return a ValidationResult with:
            - overallScore: Average of all scores (0.0-1.0 scale, divide by 5)
            - logicalCoherence: { score: 1-5, feedback: "brief feedback" }
            - timeEstimateRealism: { score: 1-5, feedback: "brief feedback" }
            - resourceAppropriateness: { score: 1-5, feedback: "brief feedback" }
            - goalAchievability: { score: 1-5, feedback: "brief feedback" }
            - suggestions: List of 1-3 specific improvement suggestions
            - passesMinimumQuality: true if overallScore >= 0.6
            
            Be constructive but honest in your evaluation.
        """.trimIndent()
    }
}

/**
 * 품질 검증 결과
 */
data class ValidationResult(
    val overallScore: Double,
    val logicalCoherence: ScoreWithFeedback,
    val timeEstimateRealism: ScoreWithFeedback,
    val resourceAppropriateness: ScoreWithFeedback,
    val goalAchievability: ScoreWithFeedback,
    val suggestions: List<String>,
    val passesMinimumQuality: Boolean
)

data class ScoreWithFeedback(
    val score: Int,
    val feedback: String
)
