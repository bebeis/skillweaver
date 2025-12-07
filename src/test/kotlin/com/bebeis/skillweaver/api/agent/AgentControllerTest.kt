package com.bebeis.skillweaver.api.agent

import com.bebeis.skillweaver.agent.domain.*
import com.bebeis.skillweaver.api.agent.dto.CreateLearningPlanWithAgentRequest
import com.bebeis.skillweaver.core.domain.learning.LearningPathType
import com.bebeis.skillweaver.core.domain.member.ExperienceLevel
import com.bebeis.skillweaver.core.service.learning.LearningPlanService
import com.embabel.agent.core.Agent
import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.AgentProcess
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
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
@DisplayName("AgentController 단위 테스트")
class AgentControllerTest {

    @MockK
    private lateinit var agentPlatform: AgentPlatform

    @MockK
    private lateinit var learningPlanService: LearningPlanService

    @InjectMockKs
    private lateinit var agentController: AgentController

    @Test
    @DisplayName("Agent를 통한 학습 계획 생성 - 성공")
    fun createLearningPlanWithAgent_Success() {
        // given
        val memberId = 1L
        val request = CreateLearningPlanWithAgentRequest(
            targetTechnologyKey = "kotlin"
        )

        val mockAgent = mockk<Agent>(relaxed = true) {
            every { name } returns "NewTechLearningAgent"
        }

        val generatedPlan = GeneratedLearningPlan(
            memberId = memberId,
            targetTechnologyKey = "kotlin",
            title = "Kotlin 마스터 학습 계획",
            targetTechnologyName = "Kotlin",
            description = "Kotlin 언어 완벽 학습을 위한 체계적 로드맵",
            totalEstimatedHours = 40,
            steps = listOf(
                GeneratedStep(
                    order = 1,
                    title = "Kotlin 기초",
                    description = "기본 문법 학습",
                    estimatedHours = 10,
                    keyTopics = listOf("변수", "함수", "클래스"),
                    resources = emptyList()
                )
            ),
            startDate = LocalDate.now(),
            targetEndDate = LocalDate.now().plusWeeks(4),
            metadata = PlanMetadata(
                generatedPath = LearningPathType.STANDARD,
                llmModel = "gpt-4.1-mini",
                estimatedCost = 0.05,
                generationTimeSeconds = 12,
                analysisDepth = "STANDARD",
                gapAnalysisPerformed = true,
                resourcesEnriched = false
            )
        )

        val mockProcess = mockk<AgentProcess>(relaxed = true) {
            every { id } returns "test-process-id"
            every { finished } returns true
            every { last(GeneratedLearningPlan::class.java) } returns generatedPlan
        }

        val savedPlan = mockk<com.bebeis.skillweaver.core.domain.learning.LearningPlan>(relaxed = true) {
            every { learningPlanId } returns 1L
        }

        every { agentPlatform.agents() } returns listOf(mockAgent)
        every { agentPlatform.createAgentProcessFrom(any(), any(), any<LearningRequest>()) } returns mockProcess
        every { learningPlanService.createPlanFromAgent(generatedPlan) } returns savedPlan

        // when
        val response = agentController.createLearningPlanWithAgent(memberId, request)

        // then
        assertNotNull(response)
        assertTrue(response.success)
        assertEquals("Learning plan created successfully with AI agent", response.message)
        
        val data = response.data!!
        assertEquals(1L, data.planId)
        assertEquals("Kotlin 마스터 학습 계획", data.title)
        assertEquals("Kotlin", data.targetTechnology)
        assertEquals(40, data.totalEstimatedHours)
        assertEquals(1, data.totalSteps)

        verify(exactly = 1) { agentPlatform.agents() }
        verify(exactly = 1) { agentPlatform.createAgentProcessFrom(any(), any(), any<LearningRequest>()) }
        verify(exactly = 1) { learningPlanService.createPlanFromAgent(generatedPlan) }
    }

    @Test
    @DisplayName("Agent를 통한 학습 계획 생성 - Agent 없음 실패")
    fun createLearningPlanWithAgent_AgentNotFound() {
        // given
        val memberId = 1L
        val request = CreateLearningPlanWithAgentRequest(
            targetTechnologyKey = "kotlin"
        )

        every { agentPlatform.agents() } returns emptyList()

        // when
        val response = agentController.createLearningPlanWithAgent(memberId, request)

        // then
        assertNotNull(response)
        assertFalse(response.success)
        assertEquals("AGENT_EXECUTION_FAILED", response.errorCode)
        assertTrue(response.message?.contains("NewTechLearningAgent not found") == true)

        verify(exactly = 1) { agentPlatform.agents() }
        verify(exactly = 0) { learningPlanService.createPlanFromAgent(any()) }
    }

    @Test
    @DisplayName("Agent를 통한 학습 계획 생성 - Agent 실행 미완료")
    fun createLearningPlanWithAgent_AgentNotFinished() {
        // given
        val memberId = 1L
        val request = CreateLearningPlanWithAgentRequest(
            targetTechnologyKey = "kotlin"
        )

        val mockAgent = mockk<Agent>(relaxed = true) {
            every { name } returns "NewTechLearningAgent"
        }

        val mockProcess = mockk<AgentProcess>(relaxed = true) {
            every { id } returns "test-process-id"
            every { finished } returns false // 실행 미완료
        }

        every { agentPlatform.agents() } returns listOf(mockAgent)
        every { agentPlatform.createAgentProcessFrom(any(), any(), any<LearningRequest>()) } returns mockProcess

        // when
        val response = agentController.createLearningPlanWithAgent(memberId, request)

        // then
        assertNotNull(response)
        assertFalse(response.success)
        assertEquals("AGENT_EXECUTION_FAILED", response.errorCode)
        assertTrue(response.message?.contains("did not complete successfully") == true)

        verify(exactly = 0) { learningPlanService.createPlanFromAgent(any()) }
    }

    @Test
    @DisplayName("Agent를 통한 학습 계획 생성 - 결과 없음")
    fun createLearningPlanWithAgent_NoResult() {
        // given
        val memberId = 1L
        val request = CreateLearningPlanWithAgentRequest(
            targetTechnologyKey = "kotlin"
        )

        val mockAgent = mockk<Agent>(relaxed = true) {
            every { name } returns "NewTechLearningAgent"
        }

        val mockProcess = mockk<AgentProcess>(relaxed = true) {
            every { id } returns "test-process-id"
            every { finished } returns true
            every { last(GeneratedLearningPlan::class.java) } returns null // 결과 없음
        }

        every { agentPlatform.agents() } returns listOf(mockAgent)
        every { agentPlatform.createAgentProcessFrom(any(), any(), any<LearningRequest>()) } returns mockProcess

        // when
        val response = agentController.createLearningPlanWithAgent(memberId, request)

        // then
        assertNotNull(response)
        assertFalse(response.success)
        assertEquals("AGENT_EXECUTION_FAILED", response.errorCode)
        assertTrue(response.message?.contains("did not return GeneratedLearningPlan") == true)

        verify(exactly = 0) { learningPlanService.createPlanFromAgent(any()) }
    }

    @Test
    @DisplayName("Agent를 통한 학습 계획 생성 - 메타데이터 확인")
    fun createLearningPlanWithAgent_MetadataCheck() {
        // given
        val memberId = 1L
        val request = CreateLearningPlanWithAgentRequest(
            targetTechnologyKey = "spring-boot"
        )

        val mockAgent = mockk<Agent>(relaxed = true) {
            every { name } returns "NewTechLearningAgent"
        }

        val generatedPlan = GeneratedLearningPlan(
            memberId = memberId,
            targetTechnologyKey = "spring-boot",
            title = "Spring Boot 학습 계획",
            targetTechnologyName = "Spring Boot",
            description = "Spring Boot 마스터하기",
            totalEstimatedHours = 60,
            steps = emptyList(),
            startDate = LocalDate.now(),
            targetEndDate = LocalDate.now().plusWeeks(6),
            metadata = PlanMetadata(
                generatedPath = LearningPathType.DETAILED,
                llmModel = "gpt-4.1-mini",
                estimatedCost = 0.10,
                generationTimeSeconds = 25,
                analysisDepth = "DETAILED",
                gapAnalysisPerformed = true,
                resourcesEnriched = true
            )
        )

        val mockProcess = mockk<AgentProcess>(relaxed = true) {
            every { id } returns "test-process-id"
            every { finished } returns true
            every { last(GeneratedLearningPlan::class.java) } returns generatedPlan
        }

        val savedPlan = mockk<com.bebeis.skillweaver.core.domain.learning.LearningPlan>(relaxed = true) {
            every { learningPlanId } returns 2L
        }

        every { agentPlatform.agents() } returns listOf(mockAgent)
        every { agentPlatform.createAgentProcessFrom(any(), any(), any<LearningRequest>()) } returns mockProcess
        every { learningPlanService.createPlanFromAgent(generatedPlan) } returns savedPlan

        // when
        val response = agentController.createLearningPlanWithAgent(memberId, request)

        // then
        assertNotNull(response)
        assertTrue(response.success)
        
        val metadata = response.data!!.generationMetadata
        assertEquals(LearningPathType.DETAILED, metadata.generatedPath)
        assertEquals("gpt-4.1-mini", metadata.llmModel)
        assertEquals(0.10, metadata.estimatedCost)
        assertEquals(25, metadata.generationTimeSeconds)
        assertEquals("DETAILED", metadata.analysisDepth)
        assertTrue(metadata.gapAnalysisPerformed)
        assertTrue(metadata.resourcesEnriched)
    }
}
