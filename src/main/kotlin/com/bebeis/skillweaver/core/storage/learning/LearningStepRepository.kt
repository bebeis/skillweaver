package com.bebeis.skillweaver.core.storage.learning

import com.bebeis.skillweaver.core.domain.learning.LearningStep
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LearningStepRepository : JpaRepository<LearningStep, Long> {
    fun findByLearningPlanIdOrderByOrder(learningPlanId: Long): List<LearningStep>
    fun findByLearningPlanIdAndCompleted(learningPlanId: Long, completed: Boolean): List<LearningStep>
    fun countByLearningPlanId(learningPlanId: Long): Int
    fun countByLearningPlanIdAndCompleted(learningPlanId: Long, completed: Boolean): Int
    fun findByLearningPlanIdAndOrder(learningPlanId: Long, order: Int): LearningStep?
}
