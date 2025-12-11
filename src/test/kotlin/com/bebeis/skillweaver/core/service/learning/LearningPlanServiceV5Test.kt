package com.bebeis.skillweaver.core.service.learning

import com.bebeis.skillweaver.api.common.exception.BusinessException
import com.bebeis.skillweaver.api.plan.dto.StartLearningPlanRequest
import com.bebeis.skillweaver.core.domain.learning.LearningPlan
import com.bebeis.skillweaver.core.domain.learning.LearningPlanStatus
import com.bebeis.skillweaver.core.domain.learning.LearningStep
import com.bebeis.skillweaver.core.domain.learning.StepDifficulty
import com.bebeis.skillweaver.core.domain.member.goal.GoalPriority
import com.bebeis.skillweaver.core.domain.member.goal.GoalStatus
import com.bebeis.skillweaver.core.domain.member.goal.LearningGoal
import com.bebeis.skillweaver.core.service.member.LearningGoalService
import com.bebeis.skillweaver.core.storage.learning.LearningPlanRepository
import com.bebeis.skillweaver.core.storage.learning.LearningStepRepository
import com.bebeis.skillweaver.core.storage.member.MemberRepository
import com.fasterxml.jackson.databind.ObjectMapper
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
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
@DisplayName("LearningPlanService V5 단위 테스트")
class LearningPlanServiceV5Test {

    @MockK
    private lateinit var learningPlanRepository: LearningPlanRepository

    @MockK
    private lateinit var learningStepRepository: LearningStepRepository

    @MockK
    private lateinit var memberRepository: MemberRepository

    @MockK
    private lateinit var objectMapper: ObjectMapper

    @MockK
    private lateinit var learningGoalService: LearningGoalService

    @InjectMockKs
    private lateinit var learningPlanService: LearningPlanService

    // =========================================================================
    // V5: startLearningPlan 테스트
    // =========================================================================

    @Nested
    @DisplayName("startLearningPlan - 학습 플랜 시작")
    inner class StartLearningPlanTest {

        @Test
        @DisplayName("성공 - 기본 요청으로 학습 시작")
        fun startLearningPlan_Success_DefaultRequest() {
            // given
            val memberId = 1L
            val planId = 10L
            val plan = LearningPlan(
                learningPlanId = planId,
                memberId = memberId,
                targetTechnology = "Kotlin Coroutines",
                totalWeeks = 4,
                totalHours = 42,
                status = LearningPlanStatus.DRAFT,
                progress = 0
            )
            
            val steps = listOf(
                createStep(1, planId, 1, "기초"),
                createStep(2, planId, 2, "심화"),
                createStep(3, planId, 3, "실습")
            )

            val createdGoal = LearningGoal(
                learningGoalId = 100L,
                memberId = memberId,
                title = "Kotlin Coroutines 학습하기",
                description = "AI 추천 학습 플랜을 통해 학습합니다.",
                status = GoalStatus.IN_PROGRESS,
                learningPlanId = planId,
                totalSteps = 3,
                completedSteps = 0,
                progressPercentage = 0
            )

            val request = StartLearningPlanRequest()
            val planSlot = slot<LearningPlan>()

            every { memberRepository.existsById(memberId) } returns true
            every { learningPlanRepository.findById(planId) } returns Optional.of(plan)
            every { learningStepRepository.findByLearningPlanIdOrderByOrder(planId) } returns steps
            every { learningPlanRepository.save(capture(planSlot)) } answers { planSlot.captured }
            every { 
                learningGoalService.createGoalFromPlan(
                    memberId = memberId,
                    planId = planId,
                    targetTechnology = "Kotlin Coroutines",
                    totalSteps = 3,
                    goalTitle = null,
                    goalDescription = null,
                    dueDate = null,
                    priority = GoalPriority.MEDIUM
                )
            } returns createdGoal

            // when
            val result = learningPlanService.startLearningPlan(memberId, planId, request)

            // then
            assertNotNull(result)
            assertEquals(100L, result.learningGoalId)
            assertEquals(planId, result.learningPlanId)
            assertEquals("Kotlin Coroutines 학습하기", result.title)
            assertEquals(GoalStatus.IN_PROGRESS, result.status)
            assertEquals(3, result.totalSteps)
            assertEquals(0, result.completedSteps)
            assertEquals(0, result.progressPercentage)

            // Plan이 ACTIVE로 변경되었는지 확인
            assertEquals(LearningPlanStatus.ACTIVE, planSlot.captured.status)
            assertNotNull(planSlot.captured.startedAt)

            verify(exactly = 1) { learningGoalService.createGoalFromPlan(any(), any(), any(), any(), any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("성공 - 커스텀 제목과 우선순위로 학습 시작")
        fun startLearningPlan_Success_CustomRequest() {
            // given
            val memberId = 1L
            val planId = 10L
            val plan = LearningPlan(
                learningPlanId = planId,
                memberId = memberId,
                targetTechnology = "Spring Boot",
                totalWeeks = 6,
                totalHours = 60,
                status = LearningPlanStatus.DRAFT,
                progress = 0
            )
            
            val steps = listOf(
                createStep(1, planId, 1, "기초"),
                createStep(2, planId, 2, "심화")
            )

            val createdGoal = LearningGoal(
                learningGoalId = 101L,
                memberId = memberId,
                title = "Spring Boot 마스터",
                description = "6주 완성",
                status = GoalStatus.IN_PROGRESS,
                priority = GoalPriority.HIGH,
                learningPlanId = planId,
                totalSteps = 2,
                completedSteps = 0,
                progressPercentage = 0
            )

            val request = StartLearningPlanRequest(
                goalTitle = "Spring Boot 마스터",
                goalDescription = "6주 완성",
                dueDate = LocalDate.of(2026, 1, 31),
                priority = GoalPriority.HIGH
            )
            val planSlot = slot<LearningPlan>()

            every { memberRepository.existsById(memberId) } returns true
            every { learningPlanRepository.findById(planId) } returns Optional.of(plan)
            every { learningStepRepository.findByLearningPlanIdOrderByOrder(planId) } returns steps
            every { learningPlanRepository.save(capture(planSlot)) } answers { planSlot.captured }
            every { 
                learningGoalService.createGoalFromPlan(
                    memberId = memberId,
                    planId = planId,
                    targetTechnology = "Spring Boot",
                    totalSteps = 2,
                    goalTitle = "Spring Boot 마스터",
                    goalDescription = "6주 완성",
                    dueDate = LocalDate.of(2026, 1, 31),
                    priority = GoalPriority.HIGH
                )
            } returns createdGoal

            // when
            val result = learningPlanService.startLearningPlan(memberId, planId, request)

            // then
            assertEquals("Spring Boot 마스터", result.title)
            assertEquals(GoalPriority.HIGH, result.priority)
        }

        @Test
        @DisplayName("실패 - 플랜 없음")
        fun startLearningPlan_PlanNotFound() {
            // given
            val memberId = 1L
            val planId = 999L

            every { memberRepository.existsById(memberId) } returns true
            every { learningPlanRepository.findById(planId) } returns Optional.empty()

            // when & then
            assertThrows<BusinessException> {
                learningPlanService.startLearningPlan(memberId, planId, StartLearningPlanRequest())
            }

            verify(exactly = 0) { learningGoalService.createGoalFromPlan(any(), any(), any(), any(), any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("실패 - 다른 회원의 플랜")
        fun startLearningPlan_WrongMember() {
            // given
            val memberId = 1L
            val otherMemberId = 2L
            val planId = 10L
            val plan = LearningPlan(
                learningPlanId = planId,
                memberId = otherMemberId,  // 다른 회원의 플랜
                targetTechnology = "Kotlin",
                totalWeeks = 4,
                totalHours = 40,
                status = LearningPlanStatus.DRAFT,
                progress = 0
            )

            every { memberRepository.existsById(memberId) } returns true
            every { learningPlanRepository.findById(planId) } returns Optional.of(plan)

            // when & then
            assertThrows<BusinessException> {
                learningPlanService.startLearningPlan(memberId, planId, StartLearningPlanRequest())
            }
        }
    }

    // =========================================================================
    // V5: completeStep with Goal sync 테스트
    // =========================================================================

    @Nested
    @DisplayName("completeStep - 스텝 완료 및 Goal 동기화")
    inner class CompleteStepTest {

        @Test
        @DisplayName("성공 - 스텝 완료 시 연결된 Goal 진행률 동기화")
        fun completeStep_Success_WithGoalSync() {
            // given
            val memberId = 1L
            val planId = 10L
            val stepOrder = 1
            
            val plan = LearningPlan(
                learningPlanId = planId,
                memberId = memberId,
                targetTechnology = "Kotlin",
                totalWeeks = 4,
                totalHours = 40,
                status = LearningPlanStatus.ACTIVE,
                progress = 0
            )

            val step = createStep(1, planId, stepOrder, "첫 번째 스텝", completed = false)
            val steps = listOf(
                step,
                createStep(2, planId, 2, "두 번째 스텝", completed = false),
                createStep(3, planId, 3, "세 번째 스텝", completed = false)
            )

            val linkedGoal = LearningGoal(
                learningGoalId = 100L,
                memberId = memberId,
                title = "Test Goal",
                description = "Test",
                status = GoalStatus.IN_PROGRESS,
                learningPlanId = planId,
                totalSteps = 3,
                completedSteps = 0,
                progressPercentage = 0
            )

            val updatedGoal = linkedGoal.copy(
                completedSteps = 1,
                progressPercentage = 33
            )

            every { memberRepository.existsById(memberId) } returns true
            every { learningPlanRepository.findById(planId) } returns Optional.of(plan)
            every { learningStepRepository.findByLearningPlanIdAndOrder(planId, stepOrder) } returns step
            every { learningStepRepository.save(any()) } answers { firstArg() }
            every { learningStepRepository.countByLearningPlanId(planId) } returns 3L
            every { learningStepRepository.countByLearningPlanIdAndCompleted(planId, true) } returns 1L
            every { learningPlanRepository.save(any()) } answers { firstArg() }
            every { learningStepRepository.findByLearningPlanIdOrderByOrder(planId) } returns steps
            every { objectMapper.readValue(any<String>(), any<Class<*>>()) } returns null
            
            // V5: Goal 동기화
            every { learningGoalService.findByLearningPlanId(planId) } returns linkedGoal
            every { learningGoalService.updateProgress(100L, 1, 3) } returns updatedGoal

            // when
            val result = learningPlanService.completeStep(memberId, planId, stepOrder)

            // then
            assertNotNull(result)
            
            // Goal 진행률 동기화 확인
            verify(exactly = 1) { learningGoalService.findByLearningPlanId(planId) }
            verify(exactly = 1) { learningGoalService.updateProgress(100L, 1, 3) }
        }

        @Test
        @DisplayName("성공 - 연결된 Goal 없이 스텝 완료")
        fun completeStep_Success_WithoutGoal() {
            // given
            val memberId = 1L
            val planId = 10L
            val stepOrder = 1
            
            val plan = LearningPlan(
                learningPlanId = planId,
                memberId = memberId,
                targetTechnology = "Kotlin",
                totalWeeks = 4,
                totalHours = 40,
                status = LearningPlanStatus.ACTIVE,
                progress = 0
            )

            val step = createStep(1, planId, stepOrder, "스텝", completed = false)
            val steps = listOf(step)

            every { memberRepository.existsById(memberId) } returns true
            every { learningPlanRepository.findById(planId) } returns Optional.of(plan)
            every { learningStepRepository.findByLearningPlanIdAndOrder(planId, stepOrder) } returns step
            every { learningStepRepository.save(any()) } answers { firstArg() }
            every { learningStepRepository.countByLearningPlanId(planId) } returns 1L
            every { learningStepRepository.countByLearningPlanIdAndCompleted(planId, true) } returns 1L
            every { learningPlanRepository.save(any()) } answers { firstArg() }
            every { learningStepRepository.findByLearningPlanIdOrderByOrder(planId) } returns steps
            every { objectMapper.readValue(any<String>(), any<Class<*>>()) } returns null
            
            // V5: Goal이 없는 경우
            every { learningGoalService.findByLearningPlanId(planId) } returns null

            // when
            val result = learningPlanService.completeStep(memberId, planId, stepOrder)

            // then
            assertNotNull(result)
            
            // Goal 동기화가 호출되지 않아야 함
            verify(exactly = 1) { learningGoalService.findByLearningPlanId(planId) }
            verify(exactly = 0) { learningGoalService.updateProgress(any(), any(), any()) }
        }

        @Test
        @DisplayName("성공 - 모든 스텝 완료 시 플랜 COMPLETED")
        fun completeStep_Success_AllStepsCompleted() {
            // given
            val memberId = 1L
            val planId = 10L
            val stepOrder = 3  // 마지막 스텝
            
            val plan = LearningPlan(
                learningPlanId = planId,
                memberId = memberId,
                targetTechnology = "Kotlin",
                totalWeeks = 4,
                totalHours = 40,
                status = LearningPlanStatus.ACTIVE,
                progress = 66
            )

            val step = createStep(3, planId, stepOrder, "마지막 스텝", completed = false)
            val steps = listOf(
                createStep(1, planId, 1, "첫 번째", completed = true),
                createStep(2, planId, 2, "두 번째", completed = true),
                step
            )
            
            val planSlot = slot<LearningPlan>()

            every { memberRepository.existsById(memberId) } returns true
            every { learningPlanRepository.findById(planId) } returns Optional.of(plan)
            every { learningStepRepository.findByLearningPlanIdAndOrder(planId, stepOrder) } returns step
            every { learningStepRepository.save(any()) } answers { firstArg() }
            every { learningStepRepository.countByLearningPlanId(planId) } returns 3L
            every { learningStepRepository.countByLearningPlanIdAndCompleted(planId, true) } returns 3L  // 모두 완료
            every { learningPlanRepository.save(capture(planSlot)) } answers { planSlot.captured }
            every { learningStepRepository.findByLearningPlanIdOrderByOrder(planId) } returns steps
            every { objectMapper.readValue(any<String>(), any<Class<*>>()) } returns null
            every { learningGoalService.findByLearningPlanId(planId) } returns null

            // when
            val result = learningPlanService.completeStep(memberId, planId, stepOrder)

            // then
            assertEquals(LearningPlanStatus.COMPLETED, planSlot.captured.status)
            assertEquals(100, planSlot.captured.progress)
        }
    }

    // =========================================================================
    // Helper 메서드
    // =========================================================================

    private fun createStep(
        stepId: Long,
        planId: Long,
        order: Int,
        title: String,
        completed: Boolean = false
    ): LearningStep {
        return LearningStep(
            stepId = stepId,
            learningPlanId = planId,
            order = order,
            title = title,
            description = "Description for $title",
            estimatedHours = 2,
            difficulty = StepDifficulty.INTERMEDIATE,
            completed = completed,
            objectives = "[]",
            suggestedResources = "[]"
        )
    }
}
