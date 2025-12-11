package com.bebeis.skillweaver.core.service.member

import com.bebeis.skillweaver.api.common.exception.BusinessException
import com.bebeis.skillweaver.api.member.dto.CreateLearningGoalRequest
import com.bebeis.skillweaver.api.member.dto.StreakStatus
import com.bebeis.skillweaver.core.domain.member.goal.GoalPriority
import com.bebeis.skillweaver.core.domain.member.goal.GoalStatus
import com.bebeis.skillweaver.core.domain.member.goal.LearningGoal
import com.bebeis.skillweaver.core.storage.member.LearningGoalRepository
import com.bebeis.skillweaver.core.storage.member.MemberRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
@DisplayName("LearningGoalService 단위 테스트")
class LearningGoalServiceTest {

    @MockK
    private lateinit var learningGoalRepository: LearningGoalRepository

    @MockK
    private lateinit var memberRepository: MemberRepository

    @InjectMockKs
    private lateinit var learningGoalService: LearningGoalService

    // =========================================================================
    // V5: 학습 플랜 연동 테스트
    // =========================================================================

    @Nested
    @DisplayName("createGoalFromPlan - 학습 플랜에서 목표 자동 생성")
    inner class CreateGoalFromPlanTest {

        @Test
        @DisplayName("성공 - 기본 제목/설명 자동 생성")
        fun createGoalFromPlan_Success_WithDefaultTitle() {
            // given
            val memberId = 1L
            val planId = 10L
            val targetTechnology = "Kotlin Coroutines"
            val totalSteps = 7

            val goalSlot = slot<LearningGoal>()
            
            every { memberRepository.existsById(memberId) } returns true
            every { learningGoalRepository.save(capture(goalSlot)) } answers { 
                goalSlot.captured.copy(learningGoalId = 100L)
            }

            // when
            val result = learningGoalService.createGoalFromPlan(
                memberId = memberId,
                planId = planId,
                targetTechnology = targetTechnology,
                totalSteps = totalSteps
            )

            // then
            assertNotNull(result)
            assertEquals(100L, result.learningGoalId)
            assertEquals("Kotlin Coroutines 학습하기", goalSlot.captured.title)
            assertTrue(goalSlot.captured.description.contains("Kotlin Coroutines"))
            assertEquals(GoalStatus.IN_PROGRESS, goalSlot.captured.status)
            assertEquals(planId, goalSlot.captured.learningPlanId)
            assertEquals(totalSteps, goalSlot.captured.totalSteps)
            assertEquals(0, goalSlot.captured.completedSteps)
            assertEquals(0, goalSlot.captured.progressPercentage)

            verify(exactly = 1) { memberRepository.existsById(memberId) }
            verify(exactly = 1) { learningGoalRepository.save(any()) }
        }

        @Test
        @DisplayName("성공 - 커스텀 제목/설명 사용")
        fun createGoalFromPlan_Success_WithCustomTitle() {
            // given
            val memberId = 1L
            val planId = 10L
            val targetTechnology = "Kotlin"
            val totalSteps = 5
            val customTitle = "나만의 Kotlin 마스터 플랜"
            val customDescription = "4주 완성 코루틴 학습"

            val goalSlot = slot<LearningGoal>()
            
            every { memberRepository.existsById(memberId) } returns true
            every { learningGoalRepository.save(capture(goalSlot)) } answers { 
                goalSlot.captured.copy(learningGoalId = 101L)
            }

            // when
            val result = learningGoalService.createGoalFromPlan(
                memberId = memberId,
                planId = planId,
                targetTechnology = targetTechnology,
                totalSteps = totalSteps,
                goalTitle = customTitle,
                goalDescription = customDescription,
                priority = GoalPriority.HIGH
            )

            // then
            assertEquals(customTitle, goalSlot.captured.title)
            assertEquals(customDescription, goalSlot.captured.description)
            assertEquals(GoalPriority.HIGH, goalSlot.captured.priority)
        }

        @Test
        @DisplayName("실패 - 회원 없음")
        fun createGoalFromPlan_MemberNotFound() {
            // given
            val memberId = 999L

            every { memberRepository.existsById(memberId) } returns false

            // when & then
            assertThrows<BusinessException> {
                learningGoalService.createGoalFromPlan(
                    memberId = memberId,
                    planId = 10L,
                    targetTechnology = "Kotlin",
                    totalSteps = 5
                )
            }

            verify(exactly = 1) { memberRepository.existsById(memberId) }
            verify(exactly = 0) { learningGoalRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("updateProgress - 진행률 업데이트")
    inner class UpdateProgressTest {

        @Test
        @DisplayName("성공 - 진행률 50%")
        fun updateProgress_Success_50Percent() {
            // given
            val goalId = 100L
            val existingGoal = LearningGoal(
                learningGoalId = goalId,
                memberId = 1L,
                title = "Test Goal",
                description = "Test",
                status = GoalStatus.IN_PROGRESS,
                learningPlanId = 10L,
                totalSteps = 10,
                completedSteps = 0,
                progressPercentage = 0,
                currentStreak = 0,
                longestStreak = 0,
                lastStudyDate = null
            )

            val goalSlot = slot<LearningGoal>()

            every { learningGoalRepository.findById(goalId) } returns Optional.of(existingGoal)
            every { learningGoalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

            // when
            val result = learningGoalService.updateProgress(
                goalId = goalId,
                completedSteps = 5,
                totalSteps = 10
            )

            // then
            assertEquals(5, goalSlot.captured.completedSteps)
            assertEquals(50, goalSlot.captured.progressPercentage)
            assertEquals(GoalStatus.IN_PROGRESS, goalSlot.captured.status)
            assertEquals(1, goalSlot.captured.currentStreak)  // 첫 학습
            assertEquals(LocalDate.now(), goalSlot.captured.lastStudyDate)
        }

        @Test
        @DisplayName("성공 - 진행률 100% (목표 완료)")
        fun updateProgress_Success_100Percent_Completed() {
            // given
            val goalId = 100L
            val yesterday = LocalDate.now().minusDays(1)
            val existingGoal = LearningGoal(
                learningGoalId = goalId,
                memberId = 1L,
                title = "Test Goal",
                description = "Test",
                status = GoalStatus.IN_PROGRESS,
                learningPlanId = 10L,
                totalSteps = 5,
                completedSteps = 4,
                progressPercentage = 80,
                currentStreak = 5,
                longestStreak = 5,
                lastStudyDate = yesterday
            )

            val goalSlot = slot<LearningGoal>()

            every { learningGoalRepository.findById(goalId) } returns Optional.of(existingGoal)
            every { learningGoalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

            // when
            val result = learningGoalService.updateProgress(
                goalId = goalId,
                completedSteps = 5,
                totalSteps = 5
            )

            // then
            assertEquals(100, goalSlot.captured.progressPercentage)
            assertEquals(GoalStatus.COMPLETED, goalSlot.captured.status)
            assertEquals(6, goalSlot.captured.currentStreak)  // 어제 학습 → 오늘 +1
            assertEquals(6, goalSlot.captured.longestStreak)
        }

        @Test
        @DisplayName("실패 - 목표 없음")
        fun updateProgress_GoalNotFound() {
            // given
            val goalId = 999L

            every { learningGoalRepository.findById(goalId) } returns Optional.empty()

            // when & then
            assertThrows<BusinessException> {
                learningGoalService.updateProgress(goalId, 1, 5)
            }
        }
    }

    // =========================================================================
    // Phase 4: 스트릭 테스트
    // =========================================================================

    @Nested
    @DisplayName("calculateStreak - 스트릭 계산 로직")
    inner class CalculateStreakTest {

        @Test
        @DisplayName("첫 학습 - 스트릭 1로 시작")
        fun calculateStreak_FirstStudy() {
            // given
            val goalId = 100L
            val existingGoal = LearningGoal(
                learningGoalId = goalId,
                memberId = 1L,
                title = "Test",
                description = "Test",
                status = GoalStatus.IN_PROGRESS,
                learningPlanId = 10L,
                totalSteps = 5,
                completedSteps = 0,
                progressPercentage = 0,
                currentStreak = 0,
                longestStreak = 0,
                lastStudyDate = null  // 첫 학습
            )

            val goalSlot = slot<LearningGoal>()

            every { learningGoalRepository.findById(goalId) } returns Optional.of(existingGoal)
            every { learningGoalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

            // when
            learningGoalService.updateProgress(goalId, 1, 5)

            // then
            assertEquals(1, goalSlot.captured.currentStreak)
            assertEquals(1, goalSlot.captured.longestStreak)
            assertEquals(LocalDate.now(), goalSlot.captured.lastStudyDate)
        }

        @Test
        @DisplayName("연속 학습 - 스트릭 증가")
        fun calculateStreak_ConsecutiveStudy() {
            // given
            val goalId = 100L
            val yesterday = LocalDate.now().minusDays(1)
            val existingGoal = LearningGoal(
                learningGoalId = goalId,
                memberId = 1L,
                title = "Test",
                description = "Test",
                status = GoalStatus.IN_PROGRESS,
                learningPlanId = 10L,
                totalSteps = 5,
                completedSteps = 1,
                progressPercentage = 20,
                currentStreak = 3,
                longestStreak = 5,
                lastStudyDate = yesterday  // 어제 학습함
            )

            val goalSlot = slot<LearningGoal>()

            every { learningGoalRepository.findById(goalId) } returns Optional.of(existingGoal)
            every { learningGoalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

            // when
            learningGoalService.updateProgress(goalId, 2, 5)

            // then
            assertEquals(4, goalSlot.captured.currentStreak)  // 3 + 1
            assertEquals(5, goalSlot.captured.longestStreak)  // 유지
        }

        @Test
        @DisplayName("같은 날 재학습 - 스트릭 유지")
        fun calculateStreak_SameDayStudy() {
            // given
            val goalId = 100L
            val today = LocalDate.now()
            val existingGoal = LearningGoal(
                learningGoalId = goalId,
                memberId = 1L,
                title = "Test",
                description = "Test",
                status = GoalStatus.IN_PROGRESS,
                learningPlanId = 10L,
                totalSteps = 5,
                completedSteps = 1,
                progressPercentage = 20,
                currentStreak = 3,
                longestStreak = 5,
                lastStudyDate = today  // 오늘 이미 학습함
            )

            val goalSlot = slot<LearningGoal>()

            every { learningGoalRepository.findById(goalId) } returns Optional.of(existingGoal)
            every { learningGoalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

            // when
            learningGoalService.updateProgress(goalId, 2, 5)

            // then
            assertEquals(3, goalSlot.captured.currentStreak)  // 유지
            assertEquals(5, goalSlot.captured.longestStreak)  // 유지
        }

        @Test
        @DisplayName("스트릭 끊김 - 리셋")
        fun calculateStreak_StreakBroken() {
            // given
            val goalId = 100L
            val threeDaysAgo = LocalDate.now().minusDays(3)
            val existingGoal = LearningGoal(
                learningGoalId = goalId,
                memberId = 1L,
                title = "Test",
                description = "Test",
                status = GoalStatus.IN_PROGRESS,
                learningPlanId = 10L,
                totalSteps = 5,
                completedSteps = 1,
                progressPercentage = 20,
                currentStreak = 10,
                longestStreak = 10,
                lastStudyDate = threeDaysAgo  // 3일 전 학습 (끊김)
            )

            val goalSlot = slot<LearningGoal>()

            every { learningGoalRepository.findById(goalId) } returns Optional.of(existingGoal)
            every { learningGoalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

            // when
            learningGoalService.updateProgress(goalId, 2, 5)

            // then
            assertEquals(1, goalSlot.captured.currentStreak)   // 리셋
            assertEquals(10, goalSlot.captured.longestStreak)  // 유지
        }

        @Test
        @DisplayName("새 최장 기록 갱신")
        fun calculateStreak_NewLongestStreak() {
            // given
            val goalId = 100L
            val yesterday = LocalDate.now().minusDays(1)
            val existingGoal = LearningGoal(
                learningGoalId = goalId,
                memberId = 1L,
                title = "Test",
                description = "Test",
                status = GoalStatus.IN_PROGRESS,
                learningPlanId = 10L,
                totalSteps = 5,
                completedSteps = 1,
                progressPercentage = 20,
                currentStreak = 5,
                longestStreak = 5,  // 현재 최장 기록과 동일
                lastStudyDate = yesterday
            )

            val goalSlot = slot<LearningGoal>()

            every { learningGoalRepository.findById(goalId) } returns Optional.of(existingGoal)
            every { learningGoalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

            // when
            learningGoalService.updateProgress(goalId, 2, 5)

            // then
            assertEquals(6, goalSlot.captured.currentStreak)
            assertEquals(6, goalSlot.captured.longestStreak)  // 갱신!
        }
    }

    @Nested
    @DisplayName("getStreakInfo - 스트릭 정보 조회")
    inner class GetStreakInfoTest {

        @Test
        @DisplayName("ACTIVE 상태 - 오늘 학습 완료")
        fun getStreakInfo_Active() {
            // given
            val memberId = 1L
            val goalId = 100L
            val today = LocalDate.now()
            val goal = LearningGoal(
                learningGoalId = goalId,
                memberId = memberId,
                title = "Test",
                description = "Test",
                status = GoalStatus.IN_PROGRESS,
                currentStreak = 7,
                longestStreak = 14,
                lastStudyDate = today
            )

            every { learningGoalRepository.findById(goalId) } returns Optional.of(goal)

            // when
            val result = learningGoalService.getStreakInfo(memberId, goalId)

            // then
            assertEquals(StreakStatus.ACTIVE, result.streakStatus)
            assertTrue(result.isActiveToday)
            assertEquals(7, result.currentStreak)
            assertTrue(result.message.contains("연속 학습"))
        }

        @Test
        @DisplayName("AT_RISK 상태 - 오늘 학습 안 함")
        fun getStreakInfo_AtRisk() {
            // given
            val memberId = 1L
            val goalId = 100L
            val yesterday = LocalDate.now().minusDays(1)
            val goal = LearningGoal(
                learningGoalId = goalId,
                memberId = memberId,
                title = "Test",
                description = "Test",
                status = GoalStatus.IN_PROGRESS,
                currentStreak = 5,
                longestStreak = 10,
                lastStudyDate = yesterday
            )

            every { learningGoalRepository.findById(goalId) } returns Optional.of(goal)

            // when
            val result = learningGoalService.getStreakInfo(memberId, goalId)

            // then
            assertEquals(StreakStatus.AT_RISK, result.streakStatus)
            assertFalse(result.isActiveToday)
            assertTrue(result.message.contains("이어갈 수 있어요"))
        }

        @Test
        @DisplayName("BROKEN 상태 - 스트릭 끊김")
        fun getStreakInfo_Broken() {
            // given
            val memberId = 1L
            val goalId = 100L
            val threeDaysAgo = LocalDate.now().minusDays(3)
            val goal = LearningGoal(
                learningGoalId = goalId,
                memberId = memberId,
                title = "Test",
                description = "Test",
                status = GoalStatus.IN_PROGRESS,
                currentStreak = 5,
                longestStreak = 10,
                lastStudyDate = threeDaysAgo
            )

            every { learningGoalRepository.findById(goalId) } returns Optional.of(goal)

            // when
            val result = learningGoalService.getStreakInfo(memberId, goalId)

            // then
            assertEquals(StreakStatus.BROKEN, result.streakStatus)
            assertFalse(result.isActiveToday)
            assertTrue(result.message.contains("끊어졌어요"))
        }

        @Test
        @DisplayName("NEW 상태 - 학습 시작 전")
        fun getStreakInfo_New() {
            // given
            val memberId = 1L
            val goalId = 100L
            val goal = LearningGoal(
                learningGoalId = goalId,
                memberId = memberId,
                title = "Test",
                description = "Test",
                status = GoalStatus.IN_PROGRESS,
                currentStreak = 0,
                longestStreak = 0,
                lastStudyDate = null  // 아직 학습 안 함
            )

            every { learningGoalRepository.findById(goalId) } returns Optional.of(goal)

            // when
            val result = learningGoalService.getStreakInfo(memberId, goalId)

            // then
            assertEquals(StreakStatus.NEW, result.streakStatus)
            assertFalse(result.isActiveToday)
            assertTrue(result.message.contains("시작하지 않았습니다"))
        }

        @Test
        @DisplayName("실패 - 다른 회원의 목표 조회")
        fun getStreakInfo_WrongMember() {
            // given
            val ownerId = 1L
            val otherMemberId = 2L
            val goalId = 100L
            val goal = LearningGoal(
                learningGoalId = goalId,
                memberId = ownerId,
                title = "Test",
                description = "Test",
                status = GoalStatus.IN_PROGRESS
            )

            every { learningGoalRepository.findById(goalId) } returns Optional.of(goal)

            // when & then
            assertThrows<BusinessException> {
                learningGoalService.getStreakInfo(otherMemberId, goalId)
            }
        }
    }

    @Nested
    @DisplayName("getWeeklyReport - 주간 리포트")
    inner class GetWeeklyReportTest {

        @Test
        @DisplayName("성공 - 주간 리포트 생성")
        fun getWeeklyReport_Success() {
            // given
            val memberId = 1L
            val goalId = 100L
            val goal = LearningGoal(
                learningGoalId = goalId,
                memberId = memberId,
                title = "Test",
                description = "Test",
                status = GoalStatus.IN_PROGRESS,
                learningPlanId = 10L,
                totalSteps = 10,
                completedSteps = 5,
                progressPercentage = 50,
                currentStreak = 5,
                longestStreak = 7,
                lastStudyDate = LocalDate.now()
            )

            every { learningGoalRepository.findById(goalId) } returns Optional.of(goal)

            // when
            val result = learningGoalService.getWeeklyReport(memberId, goalId)

            // then
            assertEquals(goalId, result.learningGoalId)
            assertEquals(5, result.completedSteps)
            assertTrue(result.learningDays > 0)
            assertTrue(result.milestones.isNotEmpty())
            assertTrue(result.milestones.any { it.milestone == "목표 50% 달성" && it.achieved })
        }
    }

    @Nested
    @DisplayName("getMonthlyReport - 월간 리포트")
    inner class GetMonthlyReportTest {

        @Test
        @DisplayName("성공 - 월간 리포트 생성")
        fun getMonthlyReport_Success() {
            // given
            val memberId = 1L
            val goalId = 100L
            val goal = LearningGoal(
                learningGoalId = goalId,
                memberId = memberId,
                title = "Test",
                description = "Test",
                status = GoalStatus.IN_PROGRESS,
                learningPlanId = 10L,
                totalSteps = 10,
                completedSteps = 7,
                progressPercentage = 70,
                currentStreak = 14,
                longestStreak = 14,
                lastStudyDate = LocalDate.now()
            )

            every { learningGoalRepository.findById(goalId) } returns Optional.of(goal)

            // when
            val result = learningGoalService.getMonthlyReport(memberId, goalId)

            // then
            assertEquals(goalId, result.learningGoalId)
            assertEquals(7, result.completedSteps)
            assertEquals(14, result.longestStreakInMonth)
            assertTrue(result.weeklyBreakdown.isNotEmpty())
            assertTrue(result.milestones.any { it.milestone == "7일 연속 학습" && it.achieved })
        }
    }
}
