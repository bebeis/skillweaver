package com.bebeis.skillweaver.agent.specialist

import com.bebeis.skillweaver.core.domain.learning.ResourceType
import com.bebeis.skillweaver.core.domain.member.ExperienceLevel
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.core.CoreToolGroups
import com.embabel.common.ai.model.LlmOptions
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 학습 자료 큐레이션 전문 Agent
 * 학습자 레벨과 선호에 맞는 고품질 학습 자료 선별
 */
@Agent(description = "Learning resource curation specialist")
@Component
class ResourceCuratorAgent {
    
    private val logger = LoggerFactory.getLogger(ResourceCuratorAgent::class.java)
    
    private val curatorLlm = LlmOptions(
        model = "gpt-4.1-mini",
        temperature = 0.4
    )
    
    @Action(toolGroups = [CoreToolGroups.WEB, "youtube", "github"])
    @AchievesGoal(description = "Curate personalized learning resources for target technology")
    fun curateResources(
        request: ResourceCurationRequest,
        context: OperationContext
    ): CuratedResources {
        logger.info("Curating resources for: {} (level: {})", 
            request.technology, request.learnerLevel)
        
        val languagePreference = if (request.preferKorean) {
            "Prefer Korean language resources when available. Include '한국어' in search queries for Korean tutorials."
        } else {
            "English language resources are preferred."
        }
        
        return context.ai()
            .withLlm(curatorLlm)
            .withTools(CoreToolGroups.WEB)
            .create(
                prompt = """
                You are a learning resource curator. Find and curate the best learning resources.
                
                === Request ===
                Technology: ${request.technology}
                Learner Level: ${request.learnerLevel.name}
                $languagePreference
                
                === Curation Task ===
                Use web search, YouTube, and GitHub to find the best resources for this learner.
                
                For a ${request.learnerLevel.name} learner, curate resources in these categories:
                
                1. **Official Documentation** (1-3 resources)
                   - Getting started guides
                   - Official tutorials
                   - API references
                
                2. **Video Courses** (2-4 resources)
                   - YouTube tutorials
                   - Free/paid courses
                   - Match difficulty to learner level
                
                3. **Hands-on Practice** (2-3 resources)
                   - Interactive tutorials
                   - Coding exercises
                   - Practice projects
                
                4. **GitHub Examples** (2-3 repositories)
                   - Starter templates
                   - Example projects
                   - Best practice implementations
                
                ${getAdditionalGuidance(request.learnerLevel)}
                
                Return a CuratedResources with:
                - technology: "${request.technology}"
                - learnerLevel: "${request.learnerLevel.name}"
                - officialDocs: List of CuratedResource objects
                - videoCourses: List of CuratedResource objects
                - practiceResources: List of CuratedResource objects
                - githubRepositories: List of CuratedResource objects
                - totalResourceCount: Total count of all resources
                
                Each CuratedResource should have:
                - title: Resource title
                - url: Valid URL
                - type: DOC, VIDEO, TUTORIAL, GITHUB, COURSE
                - language: "EN" or "KO"
                - description: Brief description (1-2 sentences)
                - difficulty: BEGINNER, INTERMEDIATE, ADVANCED
                - estimatedHours: Estimated hours to complete (null if not applicable)
                """.trimIndent()
            )
    }
    
    private fun getAdditionalGuidance(level: ExperienceLevel): String {
        return when (level) {
            ExperienceLevel.BEGINNER -> """
                BEGINNER FOCUS:
                - Prioritize step-by-step tutorials
                - Look for "crash course" or "for beginners" content
                - Avoid advanced topics
                - Include visual/video resources
            """.trimIndent()
            
            ExperienceLevel.INTERMEDIATE -> """
                INTERMEDIATE FOCUS:
                - Balance fundamentals and advanced topics
                - Include project-based resources
                - Look for best practices guides
                - Include some challenging content
            """.trimIndent()
            
            ExperienceLevel.ADVANCED -> """
                ADVANCED FOCUS:
                - Focus on advanced patterns and architectures
                - Include performance optimization resources
                - Look for deep-dive technical articles
                - Include edge cases and advanced configurations
            """.trimIndent()
        }
    }
}

/**
 * 리소스 큐레이션 요청
 */
data class ResourceCurationRequest(
    val technology: String,
    val learnerLevel: ExperienceLevel,
    val preferKorean: Boolean = false
)

/**
 * 큐레이션된 리소스 결과
 */
data class CuratedResources(
    val technology: String,
    val learnerLevel: String,
    val officialDocs: List<CuratedResource>,
    val videoCourses: List<CuratedResource>,
    val practiceResources: List<CuratedResource>,
    val githubRepositories: List<CuratedResource>,
    val totalResourceCount: Int
)

data class CuratedResource(
    val title: String,
    val url: String,
    val type: ResourceType,
    val language: String,
    val description: String,
    val difficulty: String,
    val estimatedHours: Int? = null
)
