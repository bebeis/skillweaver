package com.bebeis.skillweaver.api.feedback

import com.bebeis.skillweaver.core.domain.feedback.FeedbackType
import com.bebeis.skillweaver.core.domain.feedback.LearningFeedback
import com.bebeis.skillweaver.core.storage.feedback.LearningFeedbackRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@DisplayName("FeedbackController 단위 테스트")
class FeedbackControllerTest {

    @MockK
    private lateinit var feedbackRepository: LearningFeedbackRepository

    @InjectMockKs
    private lateinit var feedbackController: FeedbackController

    @Test
    @DisplayName("피드백 제출 - 성공")
    fun submitFeedback_Success() {
        // given
        val memberId = 1L
        val request = SubmitFeedbackRequest(
            learningPlanId = 100L,
            stepId = 1L,
            rating = 4,
            feedbackType = FeedbackType.HELPFUL,
            comment = "매우 도움이 됐습니다"
        )

        val savedFeedback = LearningFeedback(
            id = 1L,
            learningPlanId = 100L,
            stepId = 1L,
            memberId = memberId,
            rating = 4,
            feedbackType = FeedbackType.HELPFUL,
            comment = "매우 도움이 됐습니다"
        )

        every { feedbackRepository.save(any()) } returns savedFeedback

        // when
        val response = feedbackController.submitFeedback(memberId, request)

        // then
        assertTrue(response.success)
        assertNotNull(response.data)
        assertEquals(1L, response.data!!.id)
        assertEquals(100L, response.data!!.learningPlanId)
        assertEquals(4, response.data!!.rating)
        assertEquals(FeedbackType.HELPFUL, response.data!!.feedbackType)

        verify(exactly = 1) { feedbackRepository.save(any()) }
    }

    @Test
    @DisplayName("학습 계획별 피드백 조회 - 성공")
    fun getFeedbackByPlan_Success() {
        // given
        val memberId = 1L
        val planId = 100L
        
        val feedbacks = listOf(
            LearningFeedback(
                id = 1L,
                learningPlanId = planId,
                stepId = null,
                memberId = memberId,
                rating = 5,
                feedbackType = FeedbackType.HELPFUL,
                comment = "전체적으로 좋았습니다"
            ),
            LearningFeedback(
                id = 2L,
                learningPlanId = planId,
                stepId = 1L,
                memberId = memberId,
                rating = 3,
                feedbackType = FeedbackType.TOO_HARD,
                comment = "이 단계가 조금 어려웠습니다"
            )
        )

        every { feedbackRepository.findByLearningPlanId(planId) } returns feedbacks

        // when
        val response = feedbackController.getFeedbackByPlan(memberId, planId)

        // then
        assertTrue(response.success)
        assertEquals(2, response.data!!.size)
        assertEquals(FeedbackType.HELPFUL, response.data!![0].feedbackType)
        assertEquals(FeedbackType.TOO_HARD, response.data!![1].feedbackType)
    }

    @Test
    @DisplayName("피드백 요약 조회 - 성공")
    fun getFeedbackSummary_Success() {
        // given
        val memberId = 1L
        val planId = 100L

        every { feedbackRepository.averageRatingByPlanId(planId) } returns 4.2
        every { feedbackRepository.findByLearningPlanId(planId) } returns listOf(
            LearningFeedback(id = 1L, learningPlanId = planId, memberId = memberId, 
                rating = 4, feedbackType = FeedbackType.HELPFUL),
            LearningFeedback(id = 2L, learningPlanId = planId, memberId = memberId, 
                rating = 5, feedbackType = FeedbackType.HELPFUL)
        )
        
        FeedbackType.entries.forEach { type ->
            every { feedbackRepository.countByLearningPlanIdAndFeedbackType(planId, type) } returns 
                if (type == FeedbackType.HELPFUL) 2L else 0L
        }

        // when
        val response = feedbackController.getFeedbackSummary(memberId, planId)

        // then
        assertTrue(response.success)
        assertEquals(planId, response.data!!.planId)
        assertEquals(4.2, response.data!!.averageRating)
        assertEquals(2, response.data!!.totalFeedbackCount)
        assertEquals(2L, response.data!!.typeBreakdown["HELPFUL"])
    }
}
