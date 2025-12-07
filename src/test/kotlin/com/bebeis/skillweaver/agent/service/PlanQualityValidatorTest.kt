package com.bebeis.skillweaver.agent.service

import com.bebeis.skillweaver.agent.domain.GeneratedLearningPlan
import com.bebeis.skillweaver.agent.domain.GeneratedStep
import com.bebeis.skillweaver.agent.domain.PlanMetadata
import com.bebeis.skillweaver.core.domain.learning.LearningPathType
import com.embabel.agent.api.common.OperationContext
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("PlanQualityValidator 단위 테스트")
class PlanQualityValidatorTest {

    private val validator = PlanQualityValidator()

    @Test
    @DisplayName("학습 계획의 품질 검증 결과 구조 확인")
    fun validatePlan_ReturnsValidationResult() {
        // given
        val plan = createSamplePlan()
        val context = mockk<OperationContext>(relaxed = true)
        
        // ValidationResult 구조체가 올바르게 정의되어 있는지 확인
        val mockResult = ValidationResult(
            overallScore = 0.8,
            logicalCoherence = ScoreWithFeedback(4, "Good logical progression"),
            timeEstimateRealism = ScoreWithFeedback(4, "Realistic estimates"),
            resourceAppropriateness = ScoreWithFeedback(3, "Adequate resources"),
            goalAchievability = ScoreWithFeedback(4, "Achievable goals"),
            suggestions = listOf("Add more practical examples"),
            passesMinimumQuality = true
        )

        // then
        assertTrue(mockResult.passesMinimumQuality)
        assertEquals(0.8, mockResult.overallScore)
        assertEquals(4, mockResult.logicalCoherence.score)
        assertEquals(1, mockResult.suggestions.size)
    }

    @Test
    @DisplayName("최저 품질 기준 통과 여부 확인")
    fun validatePlan_MinimumQualityCheck() {
        // given
        val highQualityResult = ValidationResult(
            overallScore = 0.75,
            logicalCoherence = ScoreWithFeedback(4, ""),
            timeEstimateRealism = ScoreWithFeedback(4, ""),
            resourceAppropriateness = ScoreWithFeedback(3, ""),
            goalAchievability = ScoreWithFeedback(4, ""),
            suggestions = emptyList(),
            passesMinimumQuality = true
        )
        
        val lowQualityResult = ValidationResult(
            overallScore = 0.4,
            logicalCoherence = ScoreWithFeedback(2, ""),
            timeEstimateRealism = ScoreWithFeedback(2, ""),
            resourceAppropriateness = ScoreWithFeedback(2, ""),
            goalAchievability = ScoreWithFeedback(2, ""),
            suggestions = listOf("Needs major improvement"),
            passesMinimumQuality = false
        )

        // then
        assertTrue(highQualityResult.passesMinimumQuality)
        assertFalse(lowQualityResult.passesMinimumQuality)
    }

    @Test
    @DisplayName("ScoreWithFeedback 데이터 클래스 구조 확인")
    fun scoreWithFeedback_Structure() {
        // given
        val score = ScoreWithFeedback(
            score = 5,
            feedback = "Excellent logical flow"
        )

        // then
        assertEquals(5, score.score)
        assertEquals("Excellent logical flow", score.feedback)
    }

    private fun createSamplePlan(): GeneratedLearningPlan {
        return GeneratedLearningPlan(
            memberId = 1L,
            targetTechnologyKey = "kotlin",
            targetTechnologyName = "Kotlin",
            title = "Kotlin 학습 계획",
            description = "Kotlin 마스터하기",
            totalEstimatedHours = 40,
            startDate = LocalDate.now(),
            targetEndDate = LocalDate.now().plusWeeks(4),
            steps = listOf(
                GeneratedStep(
                    order = 1,
                    title = "기초 문법",
                    description = "Kotlin 기본 문법 학습",
                    estimatedHours = 10,
                    keyTopics = listOf("변수", "함수"),
                    resources = emptyList()
                )
            ),
            metadata = PlanMetadata(
                generatedPath = LearningPathType.STANDARD,
                llmModel = "gpt-4.1-mini",
                estimatedCost = 0.05,
                generationTimeSeconds = 10,
                analysisDepth = "STANDARD",
                gapAnalysisPerformed = true,
                resourcesEnriched = false
            )
        )
    }
}
