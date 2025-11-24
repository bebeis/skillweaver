package com.bebeis.skillweaver.core.service.agent

import com.bebeis.skillweaver.api.common.exception.BusinessException
import com.bebeis.skillweaver.core.domain.agent.AgentRun
import com.bebeis.skillweaver.core.domain.agent.AgentRunStatus
import com.bebeis.skillweaver.core.domain.agent.AgentType
import com.bebeis.skillweaver.core.storage.agent.AgentRunRepository
import com.bebeis.skillweaver.core.storage.member.MemberRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
@DisplayName("AgentRunService 단위 테스트")
class AgentRunServiceTest {

    @MockK
    private lateinit var agentRunRepository: AgentRunRepository

    @MockK
    private lateinit var memberRepository: MemberRepository

    @InjectMockKs
    private lateinit var agentRunService: AgentRunService

    @Test
    @DisplayName("AgentRun 생성 - 성공")
    fun createRun_Success() {
        // given
        val memberId = 1L
        val agentType = AgentType.LEARNING_PLAN
        val parameters = """{"targetTechnology":"Kotlin"}"""
        
        val savedAgentRun = AgentRun(
            agentRunId = 1L,
            memberId = memberId,
            agentType = agentType,
            status = AgentRunStatus.PENDING,
            parameters = parameters
        )

        every { memberRepository.existsById(memberId) } returns true
        every { agentRunRepository.save(any()) } returns savedAgentRun

        // when
        val result = agentRunService.createRun(memberId, agentType, parameters)

        // then
        assertNotNull(result)
        assertEquals(1L, result.agentRunId)
        assertEquals(memberId, result.memberId)
        assertEquals(agentType, result.agentType)
        assertEquals(AgentRunStatus.PENDING, result.status)
        assertEquals(parameters, result.parameters)

        verify(exactly = 1) { memberRepository.existsById(memberId) }
        verify(exactly = 1) { agentRunRepository.save(any()) }
    }

    @Test
    @DisplayName("AgentRun 생성 - 회원 없음 실패")
    fun createRun_MemberNotFound() {
        // given
        val memberId = 999L
        val agentType = AgentType.LEARNING_PLAN
        val parameters = """{"targetTechnology":"Kotlin"}"""

        every { memberRepository.existsById(memberId) } returns false

        // when & then
        assertThrows<BusinessException> {
            agentRunService.createRun(memberId, agentType, parameters)
        }

        verify(exactly = 1) { memberRepository.existsById(memberId) }
        verify(exactly = 0) { agentRunRepository.save(any()) }
    }

    @Test
    @DisplayName("AgentRun 시작 - PENDING → RUNNING")
    fun startRun_Success() {
        // given
        val agentRunId = 1L
        val agentRun = AgentRun(
            agentRunId = agentRunId,
            memberId = 1L,
            agentType = AgentType.LEARNING_PLAN,
            status = AgentRunStatus.PENDING,
            parameters = "{}"
        )

        every { agentRunRepository.findById(agentRunId) } returns Optional.of(agentRun)

        // when
        val result = agentRunService.startRun(agentRunId)

        // then
        assertEquals(AgentRunStatus.RUNNING, result.status)
        assertNotNull(result.startedAt)

        verify(exactly = 1) { agentRunRepository.findById(agentRunId) }
    }

    @Test
    @DisplayName("AgentRun 시작 - 존재하지 않는 ID")
    fun startRun_NotFound() {
        // given
        val agentRunId = 999L

        every { agentRunRepository.findById(agentRunId) } returns Optional.empty()

        // when & then
        assertThrows<IllegalArgumentException> {
            agentRunService.startRun(agentRunId)
        }

        verify(exactly = 1) { agentRunRepository.findById(agentRunId) }
    }

    @Test
    @DisplayName("AgentRun 완료 - RUNNING → COMPLETED")
    fun completeRun_Success() {
        // given
        val agentRunId = 1L
        val agentRun = AgentRun(
            agentRunId = agentRunId,
            memberId = 1L,
            agentType = AgentType.LEARNING_PLAN,
            status = AgentRunStatus.PENDING,
            parameters = "{}"
        )
        agentRun.start() // PENDING → RUNNING (실제로 상태 변경)

        val result = """{"curriculum":[]}"""
        val learningPlanId = 5L
        val cost = 0.05
        val executionTimeMs = 180000L

        every { agentRunRepository.findById(agentRunId) } returns Optional.of(agentRun)

        // when
        val completed = agentRunService.completeRun(
            agentRunId = agentRunId,
            result = result,
            learningPlanId = learningPlanId,
            cost = cost,
            executionTimeMs = executionTimeMs
        )

        // then
        assertEquals(AgentRunStatus.COMPLETED, completed.status)
        assertEquals(result, completed.result)
        assertEquals(learningPlanId, completed.learningPlanId)
        assertEquals(cost, completed.estimatedCost)
        assertEquals(executionTimeMs, completed.executionTimeMs)
        assertNotNull(completed.completedAt)

        verify(exactly = 1) { agentRunRepository.findById(agentRunId) }
    }

    @Test
    @DisplayName("AgentRun 실패 - RUNNING → FAILED")
    fun failRun_Success() {
        // given
        val agentRunId = 1L
        val agentRun = AgentRun(
            agentRunId = agentRunId,
            memberId = 1L,
            agentType = AgentType.LEARNING_PLAN,
            status = AgentRunStatus.PENDING,
            parameters = "{}"
        )
        agentRun.start() // PENDING → RUNNING (실제로 상태 변경)

        val errorMessage = "LLM API timeout"

        every { agentRunRepository.findById(agentRunId) } returns Optional.of(agentRun)

        // when
        val failed = agentRunService.failRun(agentRunId, errorMessage)

        // then
        assertEquals(AgentRunStatus.FAILED, failed.status)
        assertEquals(errorMessage, failed.errorMessage)
        assertNotNull(failed.completedAt)

        verify(exactly = 1) { agentRunRepository.findById(agentRunId) }
    }

    @Test
    @DisplayName("AgentRun 조회 - 성공")
    fun getRun_Success() {
        // given
        val memberId = 1L
        val agentRunId = 1L
        val agentRun = AgentRun(
            agentRunId = agentRunId,
            memberId = memberId,
            agentType = AgentType.LEARNING_PLAN,
            status = AgentRunStatus.COMPLETED,
            parameters = "{}"
        )

        every { agentRunRepository.findById(agentRunId) } returns Optional.of(agentRun)

        // when
        val result = agentRunService.getRun(memberId, agentRunId)

        // then
        assertNotNull(result)
        assertEquals(agentRunId, result.agentRunId)
        assertEquals(memberId, result.memberId)

        verify(exactly = 1) { agentRunRepository.findById(agentRunId) }
    }

    @Test
    @DisplayName("AgentRun 조회 - 다른 회원의 실행 기록 조회 실패")
    fun getRun_WrongMember() {
        // given
        val ownerId = 1L
        val otherMemberId = 2L
        val agentRunId = 1L
        val agentRun = AgentRun(
            agentRunId = agentRunId,
            memberId = ownerId,
            agentType = AgentType.LEARNING_PLAN,
            status = AgentRunStatus.COMPLETED,
            parameters = "{}"
        )

        every { agentRunRepository.findById(agentRunId) } returns Optional.of(agentRun)

        // when & then
        assertThrows<IllegalArgumentException> {
            agentRunService.getRun(otherMemberId, agentRunId)
        }

        verify(exactly = 1) { agentRunRepository.findById(agentRunId) }
    }

    @Test
    @DisplayName("회원별 AgentRun 목록 조회 - 전체")
    fun getRunsByMember_All() {
        // given
        val memberId = 1L
        val runs = listOf(
            AgentRun(
                agentRunId = 1L,
                memberId = memberId,
                agentType = AgentType.LEARNING_PLAN,
                status = AgentRunStatus.COMPLETED,
                parameters = "{}"
            ),
            AgentRun(
                agentRunId = 2L,
                memberId = memberId,
                agentType = AgentType.LEARNING_PLAN,
                status = AgentRunStatus.RUNNING,
                parameters = "{}"
            )
        )

        every { memberRepository.existsById(memberId) } returns true
        every { agentRunRepository.findByMemberId(memberId) } returns runs

        // when
        val result = agentRunService.getRunsByMember(memberId, null)

        // then
        assertEquals(2, result.size)
        assertEquals(AgentRunStatus.COMPLETED, result[0].status)
        assertEquals(AgentRunStatus.RUNNING, result[1].status)

        verify(exactly = 1) { memberRepository.existsById(memberId) }
        verify(exactly = 1) { agentRunRepository.findByMemberId(memberId) }
    }

    @Test
    @DisplayName("회원별 AgentRun 목록 조회 - 상태 필터링")
    fun getRunsByMember_WithStatusFilter() {
        // given
        val memberId = 1L
        val status = AgentRunStatus.COMPLETED
        val runs = listOf(
            AgentRun(
                agentRunId = 1L,
                memberId = memberId,
                agentType = AgentType.LEARNING_PLAN,
                status = AgentRunStatus.COMPLETED,
                parameters = "{}"
            )
        )

        every { memberRepository.existsById(memberId) } returns true
        every { agentRunRepository.findByMemberIdAndStatus(memberId, status) } returns runs

        // when
        val result = agentRunService.getRunsByMember(memberId, status)

        // then
        assertEquals(1, result.size)
        assertEquals(AgentRunStatus.COMPLETED, result[0].status)

        verify(exactly = 1) { memberRepository.existsById(memberId) }
        verify(exactly = 1) { agentRunRepository.findByMemberIdAndStatus(memberId, status) }
    }

    @Test
    @DisplayName("회원 및 에이전트 타입별 조회")
    fun getRunsByMemberAndType_Success() {
        // given
        val memberId = 1L
        val agentType = AgentType.LEARNING_PLAN
        val runs = listOf(
            AgentRun(
                agentRunId = 1L,
                memberId = memberId,
                agentType = agentType,
                status = AgentRunStatus.COMPLETED,
                parameters = "{}"
            )
        )

        every { memberRepository.existsById(memberId) } returns true
        every { agentRunRepository.findByMemberIdAndAgentType(memberId, agentType) } returns runs

        // when
        val result = agentRunService.getRunsByMemberAndType(memberId, agentType)

        // then
        assertEquals(1, result.size)
        assertEquals(agentType, result[0].agentType)

        verify(exactly = 1) { memberRepository.existsById(memberId) }
        verify(exactly = 1) { agentRunRepository.findByMemberIdAndAgentType(memberId, agentType) }
    }

    @Test
    @DisplayName("학습 플랜으로 AgentRun 조회")
    fun getRunByLearningPlan_Success() {
        // given
        val learningPlanId = 5L
        val agentRun = AgentRun(
            agentRunId = 1L,
            memberId = 1L,
            agentType = AgentType.LEARNING_PLAN,
            status = AgentRunStatus.COMPLETED,
            parameters = "{}",
            learningPlanId = learningPlanId
        )

        every { agentRunRepository.findByLearningPlanId(learningPlanId) } returns agentRun

        // when
        val result = agentRunService.getRunByLearningPlan(learningPlanId)

        // then
        assertNotNull(result)
        assertEquals(learningPlanId, result?.learningPlanId)

        verify(exactly = 1) { agentRunRepository.findByLearningPlanId(learningPlanId) }
    }

    @Test
    @DisplayName("학습 플랜으로 AgentRun 조회 - 없음")
    fun getRunByLearningPlan_NotFound() {
        // given
        val learningPlanId = 999L

        every { agentRunRepository.findByLearningPlanId(learningPlanId) } returns null

        // when
        val result = agentRunService.getRunByLearningPlan(learningPlanId)

        // then
        assertNull(result)

        verify(exactly = 1) { agentRunRepository.findByLearningPlanId(learningPlanId) }
    }
}
