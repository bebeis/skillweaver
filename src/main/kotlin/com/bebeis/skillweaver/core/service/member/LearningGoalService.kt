package com.bebeis.skillweaver.core.service.member

import com.bebeis.skillweaver.api.common.exception.ErrorCode
import com.bebeis.skillweaver.api.common.exception.badRequest
import com.bebeis.skillweaver.api.common.exception.notFound
import com.bebeis.skillweaver.api.member.dto.CreateLearningGoalRequest
import com.bebeis.skillweaver.api.member.dto.LearningGoalResponse
import com.bebeis.skillweaver.api.member.dto.UpdateLearningGoalRequest
// Phase 4: Streak & Report DTOs
import com.bebeis.skillweaver.api.member.dto.StreakResponse
import com.bebeis.skillweaver.api.member.dto.StreakStatus
import com.bebeis.skillweaver.api.member.dto.WeeklyReportResponse
import com.bebeis.skillweaver.api.member.dto.MonthlyReportResponse
import com.bebeis.skillweaver.api.member.dto.MilestoneAchievement
import com.bebeis.skillweaver.api.member.dto.WeeklySummary
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

    // =========================================================================
    // V5: 학습 플랜 연동 메서드
    // =========================================================================

    /**
     * 학습 플랜으로부터 자동으로 학습 목표를 생성합니다.
     * 
     * @param memberId 회원 ID
     * @param planId 연결할 학습 플랜 ID
     * @param targetTechnology 목표 기술명
     * @param totalSteps 전체 스텝 개수
     * @param goalTitle 목표 제목 (null이면 자동 생성)
     * @param goalDescription 목표 설명 (null이면 자동 생성)
     * @param dueDate 목표 기한 (optional)
     * @param priority 우선순위 (default: MEDIUM)
     * @return 생성된 학습 목표
     */
    @Transactional
    fun createGoalFromPlan(
        memberId: Long,
        planId: Long,
        targetTechnology: String,
        totalSteps: Int,
        goalTitle: String? = null,
        goalDescription: String? = null,
        dueDate: LocalDate? = null,
        priority: GoalPriority = GoalPriority.MEDIUM
    ): LearningGoal {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        val title = goalTitle ?: "${targetTechnology} 학습하기"
        val description = goalDescription ?: "AI 추천 학습 플랜을 통해 ${targetTechnology}을(를) 학습합니다."

        val goal = LearningGoal(
            memberId = memberId,
            title = title,
            description = description,
            dueDate = dueDate,
            priority = priority,
            status = GoalStatus.IN_PROGRESS,
            learningPlanId = planId,
            totalSteps = totalSteps,
            completedSteps = 0,
            progressPercentage = 0
        )

        val saved = learningGoalRepository.save(goal)
        logger.info("Learning goal created from plan: goalId=${saved.learningGoalId}, planId=$planId")
        return saved
    }

    /**
     * 학습 목표의 진행률을 업데이트합니다.
     * 
     * @param goalId 학습 목표 ID
     * @param completedSteps 완료된 스텝 개수
     * @param totalSteps 전체 스텝 개수
     * @return 업데이트된 학습 목표
     */
    @Transactional
    fun updateProgress(
        goalId: Long,
        completedSteps: Int,
        totalSteps: Int
    ): LearningGoal {
        val goal = learningGoalRepository.findById(goalId).orElse(null)
            ?: notFound(ErrorCode.LEARNING_GOAL_NOT_FOUND)

        val progressPercentage = if (totalSteps > 0) {
            ((completedSteps.toDouble() / totalSteps) * 100).toInt()
        } else 0

        val newStatus = when {
            progressPercentage >= 100 -> GoalStatus.COMPLETED
            progressPercentage > 0 -> GoalStatus.IN_PROGRESS
            else -> goal.status
        }

        // 스트릭 계산
        val today = LocalDate.now()
        val (newCurrentStreak, newLongestStreak) = calculateStreak(
            lastStudyDate = goal.lastStudyDate,
            currentStreak = goal.currentStreak,
            longestStreak = goal.longestStreak,
            today = today
        )

        val updated = LearningGoal(
            learningGoalId = goal.learningGoalId,
            memberId = goal.memberId,
            title = goal.title,
            description = goal.description,
            dueDate = goal.dueDate,
            priority = goal.priority,
            status = newStatus,
            learningPlanId = goal.learningPlanId,
            totalSteps = totalSteps,
            completedSteps = completedSteps,
            progressPercentage = progressPercentage,
            currentStreak = newCurrentStreak,
            longestStreak = newLongestStreak,
            lastStudyDate = today
        )

        val saved = learningGoalRepository.save(updated)
        logger.info("Learning goal progress updated: goalId=$goalId, progress=$progressPercentage%, streak=$newCurrentStreak")
        return saved
    }

    /**
     * 학습 플랜 ID로 연결된 목표를 조회합니다.
     */
    fun findByLearningPlanId(planId: Long): LearningGoal? {
        return learningGoalRepository.findByLearningPlanId(planId)
    }

    /**
     * 스트릭을 계산합니다.
     */
    private fun calculateStreak(
        lastStudyDate: LocalDate?,
        currentStreak: Int,
        longestStreak: Int,
        today: LocalDate
    ): Pair<Int, Int> {
        val newCurrentStreak = when {
            lastStudyDate == null -> 1
            lastStudyDate == today -> currentStreak  // 오늘 이미 학습함
            lastStudyDate == today.minusDays(1) -> currentStreak + 1  // 어제 학습함
            else -> 1  // 스트릭 리셋
        }
        val newLongestStreak = maxOf(longestStreak, newCurrentStreak)
        return newCurrentStreak to newLongestStreak
    }

    // =========================================================================
    // Phase 4: 스트릭 및 리포트 조회 메서드
    // =========================================================================

    /**
     * 학습 목표의 스트릭 정보를 조회합니다.
     */
    fun getStreakInfo(memberId: Long, goalId: Long): StreakResponse {
        val goal = learningGoalRepository.findById(goalId).orElse(null)
            ?: notFound(ErrorCode.LEARNING_GOAL_NOT_FOUND)
        
        if (goal.memberId != memberId) {
            notFound(ErrorCode.LEARNING_GOAL_NOT_FOUND)
        }

        val today = LocalDate.now()
        val isActiveToday = goal.lastStudyDate == today

        val (streakStatus, message) = when {
            goal.lastStudyDate == null -> {
                StreakStatus.NEW to "아직 학습을 시작하지 않았습니다. 지금 시작해보세요!"
            }
            isActiveToday -> {
                StreakStatus.ACTIVE to "오늘 학습 완료! ${goal.currentStreak}일 연속 학습 중입니다 🔥"
            }
            goal.lastStudyDate == today.minusDays(1) -> {
                StreakStatus.AT_RISK to "오늘 학습하면 ${goal.currentStreak + 1}일 스트릭을 이어갈 수 있어요!"
            }
            else -> {
                StreakStatus.BROKEN to "스트릭이 끊어졌어요. 다시 시작해봐요! 최고 기록은 ${goal.longestStreak}일입니다."
            }
        }

        return StreakResponse(
            learningGoalId = goalId,
            currentStreak = goal.currentStreak,
            longestStreak = goal.longestStreak,
            lastStudyDate = goal.lastStudyDate,
            isActiveToday = isActiveToday,
            streakStatus = streakStatus,
            message = message
        )
    }

    /**
     * 주간 학습 리포트를 생성합니다.
     */
    fun getWeeklyReport(memberId: Long, goalId: Long): WeeklyReportResponse {
        val goal = learningGoalRepository.findById(goalId).orElse(null)
            ?: notFound(ErrorCode.LEARNING_GOAL_NOT_FOUND)
        
        if (goal.memberId != memberId) {
            notFound(ErrorCode.LEARNING_GOAL_NOT_FOUND)
        }

        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val weekEnd = weekStart.plusDays(6)

        // 간단한 리포트 데이터 (실제 구현에서는 step 완료 이력을 조회해야 함)
        val completedSteps = goal.completedSteps
        val learningDays = if (goal.lastStudyDate != null && goal.lastStudyDate!! >= weekStart) {
            minOf(goal.currentStreak, 7)
        } else 0
        val totalHours = completedSteps * 2  // 예상: 스텝당 평균 2시간
        val avgHours = if (learningDays > 0) totalHours.toDouble() / learningDays else 0.0

        val milestones = checkMilestones(goal)

        return WeeklyReportResponse(
            learningGoalId = goalId,
            weekStartDate = weekStart,
            weekEndDate = weekEnd,
            completedSteps = completedSteps,
            totalLearningHours = totalHours,
            learningDays = learningDays,
            averageDailyHours = String.format("%.1f", avgHours).toDouble(),
            progressChange = goal.progressPercentage,  // 주간 변화 (간소화)
            milestones = milestones,
            comparisonWithLastWeek = null  // 이전 주 데이터 필요 (추후 구현)
        )
    }

    /**
     * 월간 학습 리포트를 생성합니다.
     */
    fun getMonthlyReport(memberId: Long, goalId: Long): MonthlyReportResponse {
        val goal = learningGoalRepository.findById(goalId).orElse(null)
            ?: notFound(ErrorCode.LEARNING_GOAL_NOT_FOUND)
        
        if (goal.memberId != memberId) {
            notFound(ErrorCode.LEARNING_GOAL_NOT_FOUND)
        }

        val today = LocalDate.now()
        val month = "${today.year}-${String.format("%02d", today.monthValue)}"

        val completedSteps = goal.completedSteps
        val learningDays = minOf(goal.currentStreak, 30)
        val totalHours = completedSteps * 2
        val avgHours = if (learningDays > 0) totalHours.toDouble() / learningDays else 0.0

        val milestones = checkMilestones(goal)

        // 주별 요약 (간소화: 현재 주만)
        val weeklySummary = listOf(
            WeeklySummary(
                weekNumber = 1,
                completedSteps = completedSteps,
                learningHours = totalHours,
                learningDays = learningDays
            )
        )

        return MonthlyReportResponse(
            learningGoalId = goalId,
            month = month,
            completedSteps = completedSteps,
            totalLearningHours = totalHours,
            learningDays = learningDays,
            averageDailyHours = String.format("%.1f", avgHours).toDouble(),
            longestStreakInMonth = goal.longestStreak,
            progressChange = goal.progressPercentage,
            milestones = milestones,
            weeklyBreakdown = weeklySummary
        )
    }

    /**
     * 마일스톤 달성 여부를 확인합니다.
     */
    private fun checkMilestones(goal: LearningGoal): List<MilestoneAchievement> {
        return listOf(
            MilestoneAchievement(
                milestone = "첫 스텝 완료",
                achievedAt = if (goal.completedSteps > 0) goal.updatedAt else null,
                achieved = goal.completedSteps > 0
            ),
            MilestoneAchievement(
                milestone = "목표 25% 달성",
                achievedAt = if (goal.progressPercentage >= 25) goal.updatedAt else null,
                achieved = goal.progressPercentage >= 25
            ),
            MilestoneAchievement(
                milestone = "목표 50% 달성",
                achievedAt = if (goal.progressPercentage >= 50) goal.updatedAt else null,
                achieved = goal.progressPercentage >= 50
            ),
            MilestoneAchievement(
                milestone = "3일 연속 학습",
                achievedAt = if (goal.longestStreak >= 3) goal.updatedAt else null,
                achieved = goal.longestStreak >= 3
            ),
            MilestoneAchievement(
                milestone = "7일 연속 학습",
                achievedAt = if (goal.longestStreak >= 7) goal.updatedAt else null,
                achieved = goal.longestStreak >= 7
            ),
            MilestoneAchievement(
                milestone = "목표 완료",
                achievedAt = if (goal.status == GoalStatus.COMPLETED) goal.updatedAt else null,
                achieved = goal.status == GoalStatus.COMPLETED
            )
        )
    }
}

