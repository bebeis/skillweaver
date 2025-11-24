package com.bebeis.skillweaver.api.agent

import com.bebeis.skillweaver.core.domain.agent.AgentRun
import com.bebeis.skillweaver.core.domain.agent.AgentRunStatus
import com.bebeis.skillweaver.core.domain.agent.AgentType
import com.bebeis.skillweaver.core.service.agent.AgentRunService
import com.embabel.agent.core.Agent
import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.AgentProcess
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@DisplayName("AgentStreamController 단위 테스트")
class AgentStreamControllerTest {

    @MockK
    private lateinit var agentRunService: AgentRunService

    @MockK
    private lateinit var agentPlatform: AgentPlatform

    @MockK
    private lateinit var objectMapper: ObjectMapper

    @MockK(relaxed = true)
    private lateinit var agentEventBroadcaster: AgentEventBroadcaster

    @InjectMockKs
    private lateinit var agentStreamController: AgentStreamController

    @Test
    @DisplayName("학습 플랜 스트리밍 - SseEmitter 반환 성공")
    fun executeLearningPlanAgentWithStream_Success() {
        // given
        val memberId = 1L
        val targetTechnology = "Kotlin Coroutines"
        val prefersFastPlan = false

        val parametersJson = """{"targetTechnology":"$targetTechnology","prefersFastPlan":$prefersFastPlan}"""

        val agentRun = AgentRun(
            agentRunId = 1L,
            memberId = memberId,
            agentType = AgentType.LEARNING_PLAN,
            status = AgentRunStatus.PENDING,
            parameters = parametersJson
        )

        val mockAgent = mockk<Agent>(relaxed = true) {
            every { name } returns "NewTechLearningAgent"
        }

        val mockProcess = mockk<AgentProcess>(relaxed = true) {
            every { finished } returns true
            every { history } returns emptyList()
            every { last(any<Class<Any>>()) } returns mapOf(
                "path" to "QUICK",
                "curriculum" to emptyList<Any>()
            )
        }

        every { objectMapper.writeValueAsString(any()) } returns parametersJson
        every { 
            agentRunService.createRun(
                memberId = memberId,
                agentType = AgentType.LEARNING_PLAN,
                parameters = parametersJson
            )
        } returns agentRun

        every { agentPlatform.agents() } returns listOf(mockAgent)
        every { 
            agentPlatform.createAgentProcessFrom(any(), any(), any(), any(), any())
        } returns mockProcess
        every { agentRunService.startRun(1L) } returns agentRun.apply { 
            if (status == AgentRunStatus.PENDING) start() 
        }
        every { agentPlatform.start(mockProcess) } answers { mockk() }
        every {
            agentRunService.completeRun(any(), any(), any(), any(), any())
        } returns agentRun.apply { 
            if (status == AgentRunStatus.PENDING) start()
            complete(
                result = """{"path":"QUICK"}""",
                planId = null,
                cost = 0.0,
                timeMs = 1000L
            )
        }

        // when
        val emitter = agentStreamController.executeLearningPlanAgentWithStream(
            memberId = memberId,
            targetTechnology = targetTechnology,
            prefersFastPlan = prefersFastPlan
        )

        // then
        assertNotNull(emitter)
        Thread.sleep(100) // 비동기 처리 대기

        verify(exactly = 1) { 
            agentRunService.createRun(memberId, AgentType.LEARNING_PLAN, parametersJson) 
        }
        verify(exactly = 1) { agentPlatform.agents() }
    }

    @Test
    @DisplayName("학습 플랜 스트리밍 - prefersFastPlan 기본값 false")
    fun executeLearningPlanAgentWithStream_DefaultPrefersFastPlan() {
        // given
        val memberId = 1L
        val targetTechnology = "Kotlin"
        val parametersJson = """{"targetTechnology":"$targetTechnology","prefersFastPlan":false}"""

        val agentRun = AgentRun(
            agentRunId = 1L,
            memberId = memberId,
            agentType = AgentType.LEARNING_PLAN,
            status = AgentRunStatus.PENDING,
            parameters = parametersJson
        )

        val mockAgent = mockk<Agent>(relaxed = true) {
            every { name } returns "NewTechLearningAgent"
        }

        val mockProcess = mockk<AgentProcess>(relaxed = true) {
            every { finished } returns true
            every { history } returns emptyList()
            every { last(any<Class<Any>>()) } returns mapOf<String, Any>()
        }

        every { objectMapper.writeValueAsString(any()) } returns parametersJson
        every { 
            agentRunService.createRun(any(), any(), any())
        } returns agentRun

        every { agentPlatform.agents() } returns listOf(mockAgent)
        every { 
            agentPlatform.createAgentProcessFrom(any(), any(), any(), any(), any())
        } returns mockProcess
        every { agentRunService.startRun(any()) } returns agentRun.apply { 
            if (status == AgentRunStatus.PENDING) start() 
        }
        every { agentPlatform.start(any()) } answers { mockk() }
        every {
            agentRunService.completeRun(any(), any(), any(), any(), any())
        } returns agentRun.apply { 
            if (status == AgentRunStatus.PENDING) start()
            complete("{}", null, 0.0, 1000L)
        }

        // when
        val emitter = agentStreamController.executeLearningPlanAgentWithStream(
            memberId = memberId,
            targetTechnology = targetTechnology
            // prefersFastPlan 생략 (기본값 false 사용)
        )

        // then
        assertNotNull(emitter)
        Thread.sleep(100) // 비동기 처리 대기

        verify(exactly = 1) { agentRunService.createRun(any(), any(), any()) }
    }

    @Test
    @DisplayName("학습 플랜 스트리밍 - Agent 없음 예외")
    fun executeLearningPlanAgentWithStream_AgentNotFound() {
        // given
        val memberId = 1L
        val targetTechnology = "Kotlin"
        val parametersJson = """{"targetTechnology":"$targetTechnology","prefersFastPlan":false}"""

        val agentRun = AgentRun(
            agentRunId = 1L,
            memberId = memberId,
            agentType = AgentType.LEARNING_PLAN,
            status = AgentRunStatus.PENDING,
            parameters = parametersJson
        )

        every { objectMapper.writeValueAsString(any()) } returns parametersJson
        every { agentRunService.createRun(any(), any(), any()) } returns agentRun
        every { agentPlatform.agents() } returns emptyList() // Agent 없음

        // when
        val emitter = agentStreamController.executeLearningPlanAgentWithStream(
            memberId = memberId,
            targetTechnology = targetTechnology
        )

        // then
        assertNotNull(emitter)
        Thread.sleep(200) // 에러 처리 대기

        verify(exactly = 1) { agentRunService.createRun(any(), any(), any()) }
        verify(exactly = 1) { agentPlatform.agents() }
    }
}
