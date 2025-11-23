package com.bebeis.skillweaver.core.storage.member

import com.bebeis.skillweaver.core.domain.member.goal.GoalPriority
import com.bebeis.skillweaver.core.domain.member.goal.GoalStatus
import com.bebeis.skillweaver.core.domain.member.goal.LearningGoal
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LearningGoalRepository : JpaRepository<LearningGoal, Long> {
    fun findByMemberId(memberId: Long): List<LearningGoal>
    fun findByMemberIdAndStatus(memberId: Long, status: GoalStatus): List<LearningGoal>
    fun findByMemberIdAndPriority(memberId: Long, priority: GoalPriority): List<LearningGoal>
}
