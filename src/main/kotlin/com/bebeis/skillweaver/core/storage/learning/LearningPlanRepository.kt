package com.bebeis.skillweaver.core.storage.learning

import com.bebeis.skillweaver.core.domain.learning.LearningPlan
import com.bebeis.skillweaver.core.domain.learning.LearningPlanStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LearningPlanRepository : JpaRepository<LearningPlan, Long> {
    fun findByMemberId(memberId: Long): List<LearningPlan>
    fun findByMemberIdAndStatus(memberId: Long, status: LearningPlanStatus): List<LearningPlan>
    fun findByMemberIdAndTargetTechnology(memberId: Long, targetTechnology: String): List<LearningPlan>
    fun findByMemberId(memberId: Long, pageable: Pageable): Page<LearningPlan>
    fun findByMemberIdAndStatus(memberId: Long, status: LearningPlanStatus, pageable: Pageable): Page<LearningPlan>
}
