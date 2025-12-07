package com.bebeis.skillweaver.api.feedback

import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.api.common.auth.AuthUser
import com.bebeis.skillweaver.core.domain.feedback.FeedbackType
import com.bebeis.skillweaver.core.domain.feedback.LearningFeedback
import com.bebeis.skillweaver.core.storage.feedback.LearningFeedbackRepository
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/feedback")
class FeedbackController(
    private val feedbackRepository: LearningFeedbackRepository
) {
    
    @PostMapping
    fun submitFeedback(
        @AuthUser memberId: Long,
        @RequestBody request: SubmitFeedbackRequest
    ): ApiResponse<FeedbackResponse> {
        val feedback = LearningFeedback(
            learningPlanId = request.learningPlanId,
            stepId = request.stepId,
            memberId = memberId,
            rating = request.rating,
            feedbackType = request.feedbackType,
            comment = request.comment
        )
        
        val saved = feedbackRepository.save(feedback)
        
        return ApiResponse.success(
            data = FeedbackResponse(
                id = saved.id!!,
                learningPlanId = saved.learningPlanId,
                stepId = saved.stepId,
                rating = saved.rating,
                feedbackType = saved.feedbackType,
                comment = saved.comment
            ),
            message = "Feedback submitted successfully"
        )
    }
    
    @GetMapping("/plans/{planId}")
    fun getFeedbackByPlan(
        @AuthUser memberId: Long,
        @PathVariable planId: Long
    ): ApiResponse<List<FeedbackResponse>> {
        val feedbacks = feedbackRepository.findByLearningPlanId(planId)
            .map { it.toResponse() }
        
        return ApiResponse.success(data = feedbacks)
    }
    
    @GetMapping("/plans/{planId}/summary")
    fun getFeedbackSummary(
        @AuthUser memberId: Long,
        @PathVariable planId: Long
    ): ApiResponse<FeedbackSummary> {
        val avgRating = feedbackRepository.averageRatingByPlanId(planId) ?: 0.0
        val totalCount = feedbackRepository.findByLearningPlanId(planId).size
        
        val typeBreakdown = FeedbackType.entries.associateWith { type ->
            feedbackRepository.countByLearningPlanIdAndFeedbackType(planId, type)
        }
        
        return ApiResponse.success(
            data = FeedbackSummary(
                planId = planId,
                averageRating = avgRating,
                totalFeedbackCount = totalCount,
                typeBreakdown = typeBreakdown.mapKeys { it.key.name }
            )
        )
    }
    
    private fun LearningFeedback.toResponse() = FeedbackResponse(
        id = this.id!!,
        learningPlanId = this.learningPlanId,
        stepId = this.stepId,
        rating = this.rating,
        feedbackType = this.feedbackType,
        comment = this.comment
    )
}

data class SubmitFeedbackRequest(
    val learningPlanId: Long,
    val stepId: Long? = null,
    val rating: Int,
    val feedbackType: FeedbackType,
    val comment: String? = null
)

data class FeedbackResponse(
    val id: Long,
    val learningPlanId: Long,
    val stepId: Long?,
    val rating: Int,
    val feedbackType: FeedbackType,
    val comment: String?
)

data class FeedbackSummary(
    val planId: Long,
    val averageRating: Double,
    val totalFeedbackCount: Int,
    val typeBreakdown: Map<String, Long>
)
