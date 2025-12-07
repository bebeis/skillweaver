package com.bebeis.skillweaver.core.storage.feedback

import com.bebeis.skillweaver.core.domain.feedback.FeedbackType
import com.bebeis.skillweaver.core.domain.feedback.LearningFeedback
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface LearningFeedbackRepository : JpaRepository<LearningFeedback, Long> {
    
    fun findByLearningPlanId(learningPlanId: Long): List<LearningFeedback>
    
    fun findByMemberId(memberId: Long): List<LearningFeedback>
    
    fun findByLearningPlanIdAndStepId(learningPlanId: Long, stepId: Long): List<LearningFeedback>
    
    @Query("SELECT AVG(f.rating) FROM LearningFeedback f WHERE f.learningPlanId = :planId")
    fun averageRatingByPlanId(planId: Long): Double?
    
    fun countByLearningPlanIdAndFeedbackType(planId: Long, feedbackType: FeedbackType): Long
}
