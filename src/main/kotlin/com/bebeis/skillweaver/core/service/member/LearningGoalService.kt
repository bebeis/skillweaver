package com.bebeis.skillweaver.core.service.member

import com.bebeis.skillweaver.api.common.exception.ErrorCode
import com.bebeis.skillweaver.api.common.exception.badRequest
import com.bebeis.skillweaver.api.common.exception.notFound
import com.bebeis.skillweaver.api.member.dto.CreateLearningGoalRequest
import com.bebeis.skillweaver.api.member.dto.LearningGoalResponse
import com.bebeis.skillweaver.api.member.dto.UpdateLearningGoalRequest
import com.bebeis.skillweaver.core.domain.member.goal.GoalPriority
import com.bebeis.skillweaver.core.domain.member.goal.GoalStatus
import com.bebeis.skillweaver.core.domain.member.goal.LearningGoal
import com.bebeis.skillweaver.core.storage.member.LearningGoalRepository
import com.bebeis.skillweaver.core.storage.member.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class LearningGoalService(
    private val learningGoalRepository: LearningGoalRepository,
    private val memberRepository: MemberRepository
) {
    private val logger = LoggerFactory.getLogger(LearningGoalService::class.java)

    @Transactional
    fun createGoal(memberId: Long, request: CreateLearningGoalRequest): LearningGoalResponse {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        request.dueDate?.let { dueDate ->
            if (dueDate.isBefore(LocalDate.now())) {
                badRequest("목표 기한은 과거일 수 없습니다")
            }
        }

        val goal = LearningGoal(
            memberId = memberId,
            title = request.title,
            description = request.description,
            dueDate = request.dueDate,
            priority = request.priority,
            status = GoalStatus.ACTIVE
        )

        val saved = learningGoalRepository.save(goal)
        logger.info("Learning goal created: ${saved.learningGoalId}")
        return LearningGoalResponse.from(saved)
    }

    fun getGoalsByMemberId(memberId: Long): List<LearningGoalResponse> {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        return learningGoalRepository.findByMemberId(memberId)
            .map { LearningGoalResponse.from(it) }
    }

    fun getGoalsByStatus(memberId: Long, status: GoalStatus): List<LearningGoalResponse> {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        return learningGoalRepository.findByMemberIdAndStatus(memberId, status)
            .map { LearningGoalResponse.from(it) }
    }

    fun getGoalsByPriority(memberId: Long, priority: GoalPriority): List<LearningGoalResponse> {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        return learningGoalRepository.findByMemberIdAndPriority(memberId, priority)
            .map { LearningGoalResponse.from(it) }
    }

    fun getGoalById(memberId: Long, goalId: Long): LearningGoalResponse {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        val goal = learningGoalRepository.findById(goalId).orElse(null)
            ?: notFound(ErrorCode.LEARNING_GOAL_NOT_FOUND)

        if (goal.memberId != memberId) {
            notFound(ErrorCode.LEARNING_GOAL_NOT_FOUND)
        }

        return LearningGoalResponse.from(goal)
    }

    @Transactional
    fun updateGoal(memberId: Long, goalId: Long, request: UpdateLearningGoalRequest): LearningGoalResponse {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        val goal = learningGoalRepository.findById(goalId).orElse(null)
            ?: notFound(ErrorCode.LEARNING_GOAL_NOT_FOUND)

        if (goal.memberId != memberId) {
            notFound(ErrorCode.LEARNING_GOAL_NOT_FOUND)
        }

        request.dueDate?.let { dueDate ->
            if (dueDate.isBefore(LocalDate.now())) {
                badRequest("목표 기한은 과거일 수 없습니다")
            }
        }

        val updated = LearningGoal(
            learningGoalId = goal.learningGoalId,
            memberId = goal.memberId,
            title = request.title ?: goal.title,
            description = request.description ?: goal.description,
            dueDate = request.dueDate ?: goal.dueDate,
            priority = request.priority ?: goal.priority,
            status = request.status ?: goal.status
        )

        val saved = learningGoalRepository.save(updated)
        logger.info("Learning goal updated: ${saved.learningGoalId}")
        return LearningGoalResponse.from(saved)
    }

    @Transactional
    fun deleteGoal(memberId: Long, goalId: Long) {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        val goal = learningGoalRepository.findById(goalId).orElse(null)
            ?: notFound(ErrorCode.LEARNING_GOAL_NOT_FOUND)

        if (goal.memberId != memberId) {
            notFound(ErrorCode.LEARNING_GOAL_NOT_FOUND)
        }

        learningGoalRepository.delete(goal)
        logger.info("Learning goal deleted: $goalId")
    }
}
