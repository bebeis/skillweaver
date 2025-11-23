package com.bebeis.skillweaver.api.agent

import com.bebeis.skillweaver.agent.NewTechLearningAgent
import com.bebeis.skillweaver.agent.domain.GeneratedLearningPlan
import com.bebeis.skillweaver.agent.domain.LearningRequest
import com.bebeis.skillweaver.api.agent.dto.AgentLearningPlanResponse
import com.bebeis.skillweaver.api.agent.dto.CreateLearningPlanWithAgentRequest
import com.bebeis.skillweaver.api.agent.dto.GenerationMetadataResponse
import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.core.service.learning.LearningPlanService
import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.Verbosity
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

/**
 * Agent 기반 학습 계획 생성 Controller
 * 
 * 책임 분리:
 * - Agent: 학습 계획 생성 (비즈니스 로직)
 * - Controller: Agent 실행 및 DB 저장 (인프라 레이어)
 */
@RestController
@RequestMapping("/api/v1/agents/learning-plan")
class AgentController(
    private val agentPlatform: AgentPlatform,
    private val learningPlanService: LearningPlanService
) {
    
    private val logger = LoggerFactory.getLogger(AgentController::class.java)
    
    /**
     * Agent를 사용하여 학습 계획 생성
     * 
     * Phase 1: Quick Path만 지원
     * - ADVANCED 사용자 + PROJECT_BASED 학습 스타일
     * - 3-5분 실행 시간
     * - $0.05 예상 비용
     * 
     * TODO: Phase 2, 3 구현 후 모든 사용자 지원
     */
    @PostMapping
    fun createLearningPlanWithAgent(
        @RequestBody request: CreateLearningPlanWithAgentRequest,
        @RequestHeader("X-Member-Id") memberId: Long
    ): ApiResponse<AgentLearningPlanResponse> {
        logger.info("Starting agent-based learning plan creation for member: {} / technology: {}", 
            memberId, request.targetTechnologyKey)
        
        try {
            // 1. Agent 찾기
            val agent = agentPlatform.agents().firstOrNull { it.name.contains("NewTechLearning") }
                ?: throw IllegalStateException("NewTechLearningAgent not found. Please ensure the agent is registered.")
            
            // 2. Agent 실행 요청 생성
            val learningRequest = LearningRequest(
                memberId = memberId,
                targetTechnologyKey = request.targetTechnologyKey
            )
            
            // 3. Agent Process 생성
            val agentProcess = agentPlatform.createAgentProcessFrom(
                agent = agent,
                processOptions = ProcessOptions(
                    verbosity = Verbosity(
                        showPrompts = true,
                        showLlmResponses = true
                    )
                ),
                learningRequest
            )
            
            logger.info("Starting agent process: {}", agentProcess.id)
            
            // 4. Agent 실행 (blocking - 완료까지 대기)
            agentProcess.run()
            
            // 5. 완료 확인
            if (!agentProcess.finished) {
                throw IllegalStateException("Agent execution did not complete successfully")
            }
            
            // 6. 결과 가져오기
            val generatedPlan = agentProcess.last(GeneratedLearningPlan::class.java)
                ?: throw IllegalStateException("Agent did not return GeneratedLearningPlan")
            
            logger.info("Agent completed successfully. Generated plan: {}", generatedPlan.title)
            
            // 6. DB 저장 (Controller/Service 책임)
            val savedPlan = learningPlanService.createPlanFromAgent(generatedPlan)
            
            logger.info("Learning plan saved to database with ID: {}", savedPlan.learningPlanId)
            
            // 7. 응답 생성
            return ApiResponse.success(
                data = AgentLearningPlanResponse(
                    planId = savedPlan.learningPlanId!!,
                    title = generatedPlan.title,
                    targetTechnology = generatedPlan.targetTechnologyName,
                    description = generatedPlan.description,
                    totalEstimatedHours = generatedPlan.totalEstimatedHours,
                    totalSteps = generatedPlan.steps.size,
                    startDate = generatedPlan.startDate.toString(),
                    targetEndDate = generatedPlan.targetEndDate.toString(),
                    generationMetadata = GenerationMetadataResponse(
                        generatedPath = generatedPlan.metadata.generatedPath,
                        llmModel = generatedPlan.metadata.llmModel,
                        estimatedCost = generatedPlan.metadata.estimatedCost,
                        generationTimeSeconds = generatedPlan.metadata.generationTimeSeconds,
                        analysisDepth = generatedPlan.metadata.analysisDepth,
                        gapAnalysisPerformed = generatedPlan.metadata.gapAnalysisPerformed,
                        resourcesEnriched = generatedPlan.metadata.resourcesEnriched
                    )
                ),
                message = "Learning plan created successfully with AI agent"
            )
            
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid input: {}", e.message)
            return ApiResponse.error(
                errorCode = "INVALID_INPUT",
                message = e.message ?: "Invalid input"
            )
        } catch (e: IllegalStateException) {
            logger.error("Agent execution failed: {}", e.message)
            return ApiResponse.error(
                errorCode = "AGENT_EXECUTION_FAILED",
                message = e.message ?: "Agent execution failed"
            )
        } catch (e: Exception) {
            logger.error("Unexpected error during agent execution", e)
            return ApiResponse.error(
                errorCode = "INTERNAL_SERVER_ERROR",
                message = "An unexpected error occurred: ${e.message}"
            )
        }
    }
}
