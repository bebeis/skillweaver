package com.bebeis.skillweaver.agent.specialist

import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.core.CoreToolGroups
import com.embabel.common.ai.model.LlmOptions
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 기술 동향 및 최신 정보 조사 전문 Agent
 * 특정 기술의 최신 버전, 트렌드, 활용 사례를 조사
 */
@Agent(description = "Technology research specialist for trends and updates")
@Component
class TechResearchAgent {
    
    private val logger = LoggerFactory.getLogger(TechResearchAgent::class.java)
    
    private val researchLlm = LlmOptions(
        model = "gpt-4.1-mini",
        temperature = 0.3
    )
    
    @Action(toolGroups = [CoreToolGroups.WEB])
    fun researchTechnologyTrends(
        technology: String,
        context: OperationContext
    ): TechResearchResult {
        logger.info("Researching technology trends for: {}", technology)
        
        return context.ai()
            .withLlm(researchLlm)
            .withTools(CoreToolGroups.WEB)
            .create(
                prompt = """
                You are a technology research specialist. Research the following technology thoroughly.
                
                Technology: $technology
                
                Use web search to find the most up-to-date information about:
                
                1. **Current Version & Release Info**
                   - Latest stable version
                   - Recent major changes or updates
                   - Release date of current version
                
                2. **Industry Trends (2024)**
                   - Current adoption rate and popularity
                   - Usage trends in the industry
                   - Notable companies using this technology
                
                3. **Learning Recommendations**
                   - Best resources for learning (official docs, courses)
                   - Common learning paths recommended by the community
                   - Estimated time to proficiency
                
                4. **Cautions & Considerations**
                   - Common pitfalls for beginners
                   - Known issues or limitations
                   - Migration considerations if coming from alternative tech
                
                Return a TechResearchResult with:
                - technology: "$technology"
                - currentVersion: Latest stable version string
                - lastUpdated: When the research was conducted
                - keyTrends: List of 3-5 current trends
                - industryAdoption: Brief description of current adoption
                - topLearningResources: List of 3-5 recommended learning resources with URLs
                - commonPitfalls: List of 2-4 common mistakes to avoid
                - estimatedLearningWeeks: Estimated weeks for competency (integer)
                
                Be specific and cite sources when possible.
                """.trimIndent()
            )
    }
}

/**
 * 기술 연구 결과
 */
data class TechResearchResult(
    val technology: String,
    val currentVersion: String,
    val lastUpdated: String,
    val keyTrends: List<String>,
    val industryAdoption: String,
    val topLearningResources: List<LearningResourceInfo>,
    val commonPitfalls: List<String>,
    val estimatedLearningWeeks: Int
)

data class LearningResourceInfo(
    val title: String,
    val url: String,
    val type: String // OFFICIAL_DOC, COURSE, TUTORIAL, BOOK
)
