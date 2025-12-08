package com.bebeis.skillweaver.agent

import com.bebeis.skillweaver.agent.domain.BackgroundAnalysis
import com.bebeis.skillweaver.agent.domain.BasicCurriculum
import com.bebeis.skillweaver.agent.domain.DailyScheduleItem
import com.bebeis.skillweaver.agent.domain.DeepTechContext
import com.bebeis.skillweaver.agent.domain.DepthPlan
import com.bebeis.skillweaver.agent.domain.DetailedCurriculum
import com.bebeis.skillweaver.agent.domain.DetailedGapAnalysis
import com.bebeis.skillweaver.agent.domain.EnrichedCurriculum
import com.bebeis.skillweaver.agent.domain.EnrichedStep
import com.bebeis.skillweaver.agent.domain.GeneratedLearningPlan
import com.bebeis.skillweaver.agent.domain.GeneratedStep
import com.bebeis.skillweaver.agent.domain.LearningRequest
import com.bebeis.skillweaver.agent.domain.LearningResource
import com.bebeis.skillweaver.agent.domain.MemberProfile
import com.bebeis.skillweaver.agent.domain.NoGapAnalysis
import com.bebeis.skillweaver.agent.domain.PlanMetadata
import com.bebeis.skillweaver.agent.domain.QuickGapAnalysis
import com.bebeis.skillweaver.agent.domain.SimpleTechContext
import com.bebeis.skillweaver.agent.domain.StandardCurriculum
import com.bebeis.skillweaver.core.domain.learning.LearningPathType.DETAILED
import com.bebeis.skillweaver.core.domain.learning.ResourceType
import com.bebeis.skillweaver.core.domain.member.ExperienceLevel
import com.bebeis.skillweaver.core.domain.member.LearningStyle
import com.bebeis.skillweaver.core.domain.technology.TechnologyCategory
import com.bebeis.skillweaver.core.storage.member.MemberRepository
import com.bebeis.skillweaver.core.storage.member.MemberSkillRepository
import com.bebeis.skillweaver.core.storage.technology.TechnologyRepository
import com.bebeis.skillweaver.agent.tools.KnowledgeSearchTool
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.Condition
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.core.CoreToolGroups
import com.embabel.common.ai.model.LlmOptions
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.bebeis.skillweaver.agent.specialist.ResourceCuratorAgent
import com.bebeis.skillweaver.agent.specialist.ResourceCurationRequest
import com.bebeis.skillweaver.agent.specialist.TechResearchAgent
import com.bebeis.skillweaver.agent.event.AgentProgressEvent
import com.bebeis.skillweaver.agent.event.ProgressStage
import com.bebeis.skillweaver.agent.event.ActiveAgentRunRegistry
import org.springframework.context.ApplicationEventPublisher
import kotlin.math.max
import kotlin.math.min

@Agent(description = "Generate personalized learning plan for new technology with RAG and Multi-Agent support")
@Component
class NewTechLearningAgent(
    private val memberRepository: MemberRepository,
    private val technologyRepository: TechnologyRepository,
    private val memberSkillRepository: MemberSkillRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val knowledgeSearchTool: KnowledgeSearchTool? = null,
    private val techResearchAgent: TechResearchAgent? = null,
    private val resourceCuratorAgent: ResourceCuratorAgent? = null,
    private val activeAgentRunRegistry: ActiveAgentRunRegistry? = null
) {
    
    private val logger = LoggerFactory.getLogger(NewTechLearningAgent::class.java)
    private val resourceExecutor: ExecutorService = Executors.newFixedThreadPool(
        max(4, Runtime.getRuntime().availableProcessors().coerceAtMost(8))
    )
    
    private val gpt41Mini = LlmOptions(
        model = "gpt-4.1-mini",
        temperature = 0.4
    )
    

    /**
     * Persona 시스템 - 에이전트의 전문성과 행동 가이드라인 정의
     */
    private val learningArchitectPersona = """
        You are a Senior Tech Learning Architect with 15+ years of experience in software development and education.
        
        Your Expertise:
        - Designing personalized learning paths for developers at all levels
        - Analyzing skill gaps and creating actionable, realistic study plans
        - Curating high-quality learning resources from official docs, GitHub, and courses
        - Understanding Korean developer community trends and preferences
        
        Your Approach:
        - Always verify information accuracy before including it in recommendations
        - Provide specific, actionable recommendations with clear time estimates
        - Consider learner's time constraints, learning style, and prior experience
        - Prioritize practical, hands-on learning with real-world project examples
        - When recommending resources, prefer official documentation and reputable sources
        - Respect Korean language preferences when specified
        
        Your Communication Style:
        - Be encouraging but realistic about time commitments
        - Explain the "why" behind each learning step
        - Highlight common pitfalls and how to avoid them
    """.trimIndent()
    
    /**
     * Chain-of-Thought 가이드라인 - LLM이 단계별로 사고하도록 유도
     */
    private val chainOfThoughtGuideline = """
        Think step by step and show your reasoning:
        1. First, analyze the given context and requirements
        2. Identify key factors that influence your decision
        3. Consider alternatives and trade-offs
        4. Make your recommendation with clear justification
        5. Verify your answer for consistency and completeness
    """.trimIndent()
    
    /**
     * Few-shot Examples - 커리큘럼 생성 품질 향상을 위한 예시
     */
    private val curriculumFewShotExamples = """
        <example technology="Spring Boot" level="INTERMEDIATE" skills="3 years Java">
        Curriculum (5 steps, 32 hours total):
        1. "Spring Boot 프로젝트 구조 이해" (4h)
           - Topics: 자동 설정, 의존성 관리, 프로젝트 레이아웃
           - Why: Spring Boot의 "마법"을 이해해야 디버깅과 커스터마이징이 가능
        2. "REST API 개발 심화" (8h)
           - Topics: Controller, Service, DTO 패턴, 예외 처리, 검증
           - Why: 실무에서 가장 많이 작성하는 코드, 패턴 숙달 필수
        3. "데이터 접근 계층 구현" (8h)
           - Topics: JPA, Repository 패턴, 쿼리 최적화
           - Why: 성능 문제의 80%가 DB 접근에서 발생
        4. "테스트 주도 개발" (6h)
           - Topics: 단위 테스트, 통합 테스트, MockK/Mockito
           - Why: 리팩토링과 유지보수를 위한 안전망
        5. "미니 프로젝트: 쇼핑몰 API" (6h)
           - Topics: 전체 흐름 통합, 코드 리뷰 체크리스트
           - Why: 실전 경험으로 자신감 확보
        </example>
        
        <example technology="Kotlin" level="BEGINNER" skills="2 years Python">
        Curriculum (7 steps, 45 hours total):
        1. "Kotlin 기초 문법" (6h)
           - Topics: 변수, 타입 시스템, null safety, 함수
           - Why: Python과 다른 정적 타입 언어의 기초 이해
        2. "함수형 프로그래밍 기초" (8h)
           - Topics: 람다, 고차 함수, 컬렉션 연산
           - Why: Kotlin의 강점이자 코드 품질 향상의 핵심
        3. "객체지향 in Kotlin" (7h)
           - Topics: 클래스, data class, sealed class, object
           - Why: Python보다 엄격한 OOP 개념 적응
        4. "코루틴 기초" (8h)
           - Topics: suspend, async/await, Flow 기초
           - Why: 현대 Kotlin 애플리케이션의 필수 요소
        5. "Kotlin + Spring Boot" (6h)
           - Topics: 프로젝트 설정, Kotlin 관용구 적용
           - Why: 실무에서 가장 많이 사용하는 조합
        6. "테스트와 MockK" (5h)
           - Topics: JUnit5, MockK, 테스트 작성 패턴
           - Why: Kotlin 친화적 테스트 도구 숙달
        7. "미니 프로젝트: Todo API" (5h)
           - Topics: CRUD 구현, 코루틴 적용
           - Why: 학습 내용 통합 및 포트폴리오 확보
        </example>
    """.trimIndent()
    
    /**
     * Gap 분석용 CoT 가이드라인
     */
    private val gapAnalysisCoT = """
        Analyze skill gaps step by step:
        1. List all prerequisites required for the target technology
        2. Compare each prerequisite with member's current skills
        3. For each gap, assess severity: HIGH (blocking), MEDIUM (slowing), LOW (minor)
        4. Identify existing skills that transfer positively
        5. Recommend specific preparation steps for HIGH/MEDIUM gaps
        6. Estimate realistic preparation time based on member's experience level
    """.trimIndent()
    
    /**
     * 진행률 이벤트 발행 헬퍼
     */
    private fun publishProgress(
        agentRunId: Long?,
        stage: ProgressStage,
        message: String,
        detail: String? = null,
        stepIndex: Int? = null,
        totalSteps: Int? = null
    ) {
        if (agentRunId == null) return
        
        try {
            val progressPercent = when (stage) {
                ProgressStage.ANALYSIS_STARTED -> 10
                ProgressStage.DEEP_ANALYSIS -> 20
                ProgressStage.GAP_ANALYSIS -> 30
                ProgressStage.CURRICULUM_GENERATION -> 50
                ProgressStage.RESOURCE_ENRICHMENT -> 70
                ProgressStage.RESOURCE_STEP_PROGRESS -> {
                    if (stepIndex != null && totalSteps != null && totalSteps > 0) {
                        70 + (stepIndex * 25 / totalSteps)
                    } else 75
                }
                ProgressStage.FINALIZING -> 95
            }
            
            eventPublisher.publishEvent(
                AgentProgressEvent(
                    processId = agentRunId.toString(),
                    stage = stage,
                    message = message,
                    detail = detail,
                    progressPercent = progressPercent,
                    stepIndex = stepIndex,
                    totalSteps = totalSteps
                )
            )
            logger.debug("Published progress event: agentRunId={}, stage={}, message={}", agentRunId, stage, message)
        } catch (e: Exception) {
            logger.warn("Failed to publish progress event: {}", e.message)
        }
    }
    
    /**
     * RAG 지식 검색을 통해 커리큘럼 생성에 필요한 컨텍스트를 수집합니다.
     * KnowledgeSearchTool이 없으면 빈 문자열을 반환합니다.
     */
    private fun fetchRagContext(technology: String): String {
        if (knowledgeSearchTool == null) {
            logger.debug("KnowledgeSearchTool not available, skipping RAG context")
            return ""
        }
        
        return try {
            val roadmapResults = knowledgeSearchTool.searchRoadmap(technology)
            val bestPractices = knowledgeSearchTool.searchBestPractices(technology)
            
            if (roadmapResults.isEmpty() && bestPractices.isEmpty()) {
                logger.debug("No RAG results found for technology: {}", technology)
                return ""
            }
            
            buildString {
                if (roadmapResults.isNotEmpty()) {
                    appendLine("=== Curated Learning Roadmap from Knowledge Base ===")
                    roadmapResults.take(3).forEach { result ->
                        appendLine("- ${result.content.take(500)}")
                        appendLine("  (Source: ${result.source})")
                    }
                    appendLine()
                }
                
                if (bestPractices.isNotEmpty()) {
                    appendLine("=== Best Practices from Knowledge Base ===")
                    bestPractices.take(3).forEach { result ->
                        appendLine("- ${result.content.take(300)}")
                    }
                    appendLine()
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to fetch RAG context for {}: {}", technology, e.message)
            ""
        }
    }
    
    /**
     * RAG 컨텍스트가 있을 경우 프롬프트에 포함할 참조 섹션을 생성합니다.
     */
    private fun buildRagPromptSection(ragContext: String): String {
        if (ragContext.isBlank()) return ""
        
        return """
            
            === Knowledge Base Reference (Use this as authoritative guidance) ===
            $ragContext
            
            IMPORTANT: Incorporate insights from the Knowledge Base above when creating the curriculum.
            Prefer structured roadmaps and best practices from the knowledge base over generic advice.
        """.trimIndent()
    }
    
    /**
     * 전문 Agent들을 호출하여 기술 조사 및 리소스 큐레이션을 수행합니다.
     * Optional 패턴으로 Agent가 없으면 스킵합니다.
     */
    private fun fetchSpecialistInsights(
        technology: String,
        profile: MemberProfile,
        context: OperationContext
    ): SpecialistInsights {
        logger.info("Fetching specialist insights for: {}", technology)
        
        // 기술 조사 Agent 호출
        val techResearch = techResearchAgent?.let { agent ->
            try {
                agent.researchTechnologyTrends(technology, context)
            } catch (e: Exception) {
                logger.warn("TechResearchAgent failed for {}: {}", technology, e.message)
                null
            }
        }
        
        // 리소스 큐레이션 Agent 호출  
        val curatedResources = resourceCuratorAgent?.let { agent ->
            try {
                val request = ResourceCurationRequest(
                    technology = technology,
                    learnerLevel = profile.experienceLevel,
                    preferKorean = profile.learningPreference.preferKorean
                )
                agent.curateResources(request, context)
            } catch (e: Exception) {
                logger.warn("ResourceCuratorAgent failed for {}: {}", technology, e.message)
                null
            }
        }
        
        return SpecialistInsights(
            techResearch = techResearch,
            curatedResources = curatedResources
        )
    }
    
    /**
     * 전문 Agent 인사이트를 프롬프트에 포함할 섹션으로 변환
     */
    private fun buildSpecialistPromptSection(insights: SpecialistInsights): String {
        if (insights.isEmpty()) return ""
        
        return buildString {
            insights.techResearch?.let { research ->
                appendLine()
                appendLine("=== Technology Research Insights (from TechResearchAgent) ===")
                appendLine("Current Version: ${research.currentVersion}")
                appendLine("Key Trends: ${research.keyTrends.joinToString(", ")}")
                appendLine("Industry Adoption: ${research.industryAdoption}")
                appendLine("Common Pitfalls: ${research.commonPitfalls.joinToString("; ")}")
                appendLine("Recommended Learning Time: ${research.estimatedLearningWeeks} weeks")
                appendLine()
            }
            
            insights.curatedResources?.let { resources ->
                appendLine("=== Curated Resources (from ResourceCuratorAgent) ===")
                if (resources.officialDocs.isNotEmpty()) {
                    appendLine("Official Docs: ${resources.officialDocs.take(2).joinToString(", ") { it.title }}")
                }
                if (resources.videoCourses.isNotEmpty()) {
                    appendLine("Video Courses: ${resources.videoCourses.take(2).joinToString(", ") { it.title }}")
                }
                appendLine()
            }
        }
    }
    
    /**
     * 전문 Agent 인사이트 데이터 클래스
     */
    data class SpecialistInsights(
        val techResearch: com.bebeis.skillweaver.agent.specialist.TechResearchResult?,
        val curatedResources: com.bebeis.skillweaver.agent.specialist.CuratedResources?
    ) {
        fun isEmpty(): Boolean = techResearch == null && curatedResources == null
    }
    
    @Action
    fun extractMemberProfile(
        request: LearningRequest,
        context: OperationContext
    ): MemberProfile {
        logger.info("Extracting member profile for memberId: {}", request.memberId)
        
        val member = memberRepository.findById(request.memberId).orElse(null)
            ?: throw IllegalArgumentException("Member not found: ${request.memberId}")
        
        val currentSkills = memberSkillRepository.findByMemberId(request.memberId)
        
        return MemberProfile(
            memberId = member.memberId!!,
            name = member.name,
            targetTrack = member.targetTrack,
            experienceLevel = member.experienceLevel,
            learningPreference = member.learningPreference,
            currentSkillCount = currentSkills.size,
            weeklyCapacityMinutes = member.weeklyCapacityMinutes()
        )
    }
    
    @Action(
        toolGroups = [CoreToolGroups.WEB]
    )
    fun quickAnalysis(
        profile: MemberProfile,
        request: LearningRequest,
        context: OperationContext
    ): SimpleTechContext {
        logger.info("Quick analysis for technology: {}", request.targetTechnologyKey)
        
        val technology = resolveTechnologyDescriptor(request.targetTechnologyKey)
        
        return context.ai()
            .withLlm(gpt41Mini)
            .create(
                prompt = """
                Analyze the technology: ${technology.displayName} (${technology.key})
                
                Research:
                - Latest version and updates
                - Market demand and adoption trends
                - Official documentation and learning resources
                - Real-world use cases
                
                Member Profile:
                - Experience Level: ${profile.experienceLevel.name}
                - Target Track: ${profile.targetTrack.name}
                - Current Skills: ${profile.currentSkillCount}
                
                Return a SimpleTechContext with brief, essential information.
                """.trimIndent()
            )
    }
    
    @Action
    fun skipGapAnalysis(
        profile: MemberProfile,
        techContext: SimpleTechContext
    ): NoGapAnalysis {
        logger.info("Skipping gap analysis for experienced member: {}", profile.memberId)
        
        return NoGapAnalysis(
            skipped = true,
            reason = "Experienced ${profile.experienceLevel} developer with ${profile.currentSkillCount} skills - self-assessment capable"
        )
    }
    
    @Action
    fun generateQuickCurriculum(
        profile: MemberProfile,
        techContext: SimpleTechContext,
        context: OperationContext
    ): BasicCurriculum {
        logger.info("Generating quick curriculum for: {}", techContext.technologyKey)
        
        val weeklyHours = profile.weeklyCapacityMinutes / 60
        
        // RAG 지식 검색
        val ragContext = fetchRagContext(techContext.displayName)
        val ragSection = buildRagPromptSection(ragContext)
        if (ragContext.isNotBlank()) {
            logger.info("RAG context found for {}, enhancing prompt", techContext.displayName)
        }
        
        // Phase 5: 전문 Agent 인사이트 수집
        val specialistInsights = fetchSpecialistInsights(techContext.displayName, profile, context)
        val specialistSection = buildSpecialistPromptSection(specialistInsights)
        if (!specialistInsights.isEmpty()) {
            logger.info("Specialist insights collected for {}", techContext.displayName)
        }
        
        return context.ai()
            .withLlm(gpt41Mini)
            .withTools(CoreToolGroups.WEB)
            .create(
                prompt = """
                $learningArchitectPersona
                
                $chainOfThoughtGuideline
                
                Study these curriculum examples for quality reference:
                $curriculumFewShotExamples
                $ragSection
                $specialistSection
                
                Now generate a QUICK learning curriculum for ${techContext.displayName}.
                
                Use web search to find:
                - Latest best practices and learning resources
                - Popular tutorials and documentation
                - Community recommendations for fast learning
                
                Technology Info:
                - Category: ${techContext.category}
                - Difficulty: ${techContext.difficultyLevel}
                - Estimated Learning Time: ${techContext.estimatedLearningWeeks} weeks
                
                Member Constraints:
                - Experience Level: ${profile.experienceLevel.name}
                - Weekly Capacity: $weeklyHours hours
                - Learning Style: ${profile.learningPreference.learningStyle.name}
                - Prefers Korean: ${profile.learningPreference.preferKorean}
                
                Create a BasicCurriculum with EXACTLY 3-4 steps:
                - Each step should be practical and actionable
                - Focus on core concepts only (skip advanced topics)
                - Estimate hours realistically (considering member's experience)
                - Keep prerequisites and keyTopics minimal but specific
                - For each step, explain WHY it's essential
                
                Return a list of StepBlueprint objects with:
                - order: 1, 2, 3, (4)
                - title: Brief step title (Korean if preferKorean)
                - description: What to learn and WHY (2-3 sentences)
                - estimatedHours: Realistic hours (total should fit weekly capacity)
                - prerequisites: List of prerequisite concepts (can be empty for first step)
                - keyTopics: 2-3 specific, actionable topics
                
                Make it QUICK and PRACTICAL, inspired by the examples!
                """.trimIndent()
            )
    }
    
    @Action
    fun finalizeQuickPlan(
        profile: MemberProfile,
        techContext: SimpleTechContext,
        curriculum: BasicCurriculum,
        gapAnalysis: NoGapAnalysis,
        request: LearningRequest,
        context: OperationContext
    ): GeneratedLearningPlan {
        logger.info("Finalizing quick plan for member: {} / technology: {}", 
            profile.memberId, techContext.technologyKey)
        
        val totalHours = curriculum.steps.sumOf { it.estimatedHours }
        val weeklyHours = profile.weeklyCapacityMinutes / 60
        val estimatedWeeks = (totalHours.toDouble() / weeklyHours).toInt().coerceAtLeast(1)
        
        val startDate = LocalDate.now()
        val targetEndDate = startDate.plus(estimatedWeeks.toLong(), ChronoUnit.WEEKS)
        val backgroundAnalysis = buildQuickBackgroundAnalysis(profile, techContext, gapAnalysis)
        val dailySchedule = buildDailySchedule(
            profile = profile,
            startDate = startDate,
            steps = curriculum.steps.map { step ->
                StepWorkload(
                    order = step.order,
                    estimatedHours = step.estimatedHours,
                    keyTopics = step.keyTopics
                )
            }
        )
        
        val stepsWithoutResources = curriculum.steps.map { step ->
            GeneratedStep(
                order = step.order,
                title = step.title,
                description = step.description,
                estimatedHours = step.estimatedHours,
                keyTopics = step.keyTopics,
                resources = emptyList()
            )
        }
        
        val enrichedSteps = runParallelOrdered(
            items = stepsWithoutResources,
            block = { step ->
                val resources = generateResourcesForStep(
                    step = step,
                    techName = techContext.displayName,
                    prefersKorean = profile.learningPreference.preferKorean,
                    learningStyle = profile.learningPreference.learningStyle,
                    context = context,
                    quickMode = true
                )
                step.copy(resources = resources)
            },
            onError = { step, ex ->
                logger.warn("Resource generation failed for step {}: {}", step.title, ex.message)
                step.copy(resources = emptyList())
            }
        )
        
        return GeneratedLearningPlan(
            memberId = profile.memberId,
            targetTechnologyKey = request.targetTechnologyKey,
            targetTechnologyName = techContext.displayName,
            title = "Quick ${techContext.displayName} Learning Path",
            description = "Fast-track learning plan for experienced developers. " +
                    "Focuses on core concepts and practical application. " +
                    "${curriculum.steps.size} essential steps in $estimatedWeeks weeks.",
            totalEstimatedHours = totalHours,
            startDate = startDate,
            targetEndDate = targetEndDate,
            steps = enrichedSteps,
            backgroundAnalysis = backgroundAnalysis,
            dailySchedule = dailySchedule,
            metadata = PlanMetadata(
                generatedPath = com.bebeis.skillweaver.core.domain.learning.LearningPathType.QUICK,
                llmModel = "GPT-4o-mini",
                estimatedCost = 0.08,
                generationTimeSeconds = 240,
                analysisDepth = "SIMPLE",
                gapAnalysisPerformed = false,
                resourcesEnriched = true
            )
        )
    }
    
    @Action
    fun composeHybridPlan(
        profile: MemberProfile,
        techDisplayName: String,
        pathMix: List<String>,
        context: OperationContext
    ): GeneratedLearningPlan {
        logger.info("Composing hybrid plan for member: {} / technology: {}", profile.memberId, techDisplayName)

        val weeklyHours = (profile.weeklyCapacityMinutes / 60).coerceAtLeast(3)
        val stepsWithoutResources = buildHybridSteps(
            techName = techDisplayName,
            pathMix = pathMix,
            weeklyHours = weeklyHours,
            prefersKorean = profile.learningPreference.preferKorean,
            learningStyle = profile.learningPreference.learningStyle
        )
        val steps = runParallelOrdered(
            items = stepsWithoutResources,
            block = { step ->
                val resources = generateResourcesForStep(
                    step = step,
                    techName = techDisplayName,
                    prefersKorean = profile.learningPreference.preferKorean,
                    learningStyle = profile.learningPreference.learningStyle,
                    context = context
                )
                step.copy(resources = resources)
            },
            onError = { step, ex ->
                logger.warn("Resource generation failed for step {}: {}", step.title, ex.message)
                step.copy(resources = emptyList())
            }
        )

        val totalHours = steps.sumOf { it.estimatedHours }
        val startDate = LocalDate.now()
        val estimatedWeeks = (totalHours.toDouble() / weeklyHours).toInt().coerceAtLeast(1)
        val targetEndDate = startDate.plus(estimatedWeeks.toLong(), ChronoUnit.WEEKS)

        val backgroundAnalysis = BackgroundAnalysis(
            existingRelevantSkills = loadSkillNames(profile.memberId),
            knowledgeGaps = emptyList(),
            recommendations = listOf(
                "주당 ${(profile.weeklyCapacityMinutes / 60)}시간 기준 총 ${totalHours}시간 계획",
                "혼합 경로 구성: ${pathMix.joinToString(", ")}",
                "학습 스타일: ${profile.learningPreference.learningStyle}"
            ),
            riskFactors = emptyList()
        )

        val dailySchedule = buildDailySchedule(
            profile = profile,
            startDate = startDate,
            steps = steps.map {
                StepWorkload(
                    order = it.order,
                    estimatedHours = it.estimatedHours,
                    keyTopics = it.keyTopics
                )
            }
        )

        return GeneratedLearningPlan(
            memberId = profile.memberId,
            targetTechnologyKey = techDisplayName.lowercase(Locale.getDefault()),
            targetTechnologyName = techDisplayName,
            title = "Hybrid $techDisplayName Learning Path",
            description = "혼합 경로: ${pathMix.joinToString(" + ")} 기반 ${steps.size}단계, 약 ${totalHours}시간",
            totalEstimatedHours = totalHours,
            startDate = startDate,
            targetEndDate = targetEndDate,
            steps = steps,
            backgroundAnalysis = backgroundAnalysis,
            dailySchedule = dailySchedule,
            metadata = PlanMetadata(
                generatedPath = DETAILED,
                llmModel = "HYBRID_HEURISTIC",
                estimatedCost = 0.0,
                generationTimeSeconds = 5,
                analysisDepth = "HYBRID",
                gapAnalysisPerformed = true,
                resourcesEnriched = false
            )
        )
    }
    
    @Condition
    fun hasSufficientSkills(profile: MemberProfile): Boolean {
        val sufficient = profile.currentSkillCount >= 5 &&
                profile.experienceLevel in listOf(
                    ExperienceLevel.INTERMEDIATE,
                    ExperienceLevel.ADVANCED
                )
        
        logger.debug("hasSufficientSkills for member {}: {} (skills: {}, level: {})",
            profile.memberId, sufficient, profile.currentSkillCount, profile.experienceLevel)
        
        return sufficient
    }
    
    @Action
    fun decideDepthPlan(
        profile: MemberProfile,
        request: LearningRequest,
        context: OperationContext
    ): DepthPlan {
        val technology = resolveTechnologyDescriptor(request.targetTechnologyKey)
        return context.ai()
            .withLlm(gpt41Mini)
            .create(
                prompt = """
                Decide per-stage depth for an adaptive learning plan.
                
                Use modes:
                - analysisMode: quick | standard | detailed | skip
                - gapMode: none | quick | detailed | skip
                - curriculumMode: quick | standard | detailed | skip
                - resourceMode: quick | standard | detailed | skip (use detailed for full enrichment)
                - allowHybrid: true/false
                - hybridMix: optional array ["quick"/"standard"/"detailed"] if allowHybrid
                
                Member:
                - Experience: ${profile.experienceLevel}
                - Skills count: ${profile.currentSkillCount}
                - Weekly hours: ${profile.weeklyCapacityMinutes / 60}
                - Daily minutes: ${profile.learningPreference.dailyMinutes}
                - Learning style: ${profile.learningPreference.learningStyle}
                - Prefer Korean: ${profile.learningPreference.preferKorean}
                
                Technology:
                - Name: ${technology.displayName}
                - Key: ${technology.key}
                
                Return JSON matching DepthPlan. Keep rationale short (Korean).
                """.trimIndent()
            )
    }
    
    @Action
    @AchievesGoal(description = "Generate adaptive learning plan with mixed depths")
    fun buildAdaptivePlan(
        profile: MemberProfile,
        request: LearningRequest,
        depthPlan: DepthPlan,
        context: OperationContext
    ): GeneratedLearningPlan {
        val tech = resolveTechnologyDescriptor(request.targetTechnologyKey)
        
        // DEBUG: request.agentRunId 값 확인
        logger.info("buildAdaptivePlan started: request.agentRunId={}, eventPublisher={}", 
            request.agentRunId, if (eventPublisher != null) "available" else "NULL")

        val analysisMode = depthPlan.analysisMode.lowercase(Locale.getDefault())
        val gapMode = depthPlan.gapMode.lowercase(Locale.getDefault())
        val curriculumMode = depthPlan.curriculumMode.lowercase(Locale.getDefault())
        val resourceMode = depthPlan.resourceMode.lowercase(Locale.getDefault())

        publishProgress(request.agentRunId, ProgressStage.ANALYSIS_STARTED, "${tech.displayName} 기술 분석 시작", "분석 모드: $analysisMode")

        val simpleCtx = when (analysisMode) {
            "quick" -> runCatching { quickAnalysis(profile, request, context) }.getOrNull()
            "skip" -> null
            else -> runCatching { quickAnalysis(profile, request, context) }.getOrNull()
        }
        
        publishProgress(request.agentRunId, ProgressStage.DEEP_ANALYSIS, "심층 분석 진행 중", "Gap 분석 모드: $gapMode")
        
        val deepCtx = when (analysisMode) {
            "detailed", "standard" -> runCatching { deepAnalysis(profile, request, context) }.getOrNull()
            "quick" -> null
            else -> runCatching { deepAnalysis(profile, request, context) }.getOrNull()
        }

        publishProgress(request.agentRunId, ProgressStage.GAP_ANALYSIS, "역량 Gap 분석 중", "현재 스킬 수: ${profile.currentSkillCount}")

        val gap = when (gapMode) {
            "quick" -> {
                val ctx = deepCtx ?: runCatching { deepAnalysis(profile, request, context) }.getOrNull()
                    ?: throw IllegalStateException("DeepTechContext required for quick gap analysis")
                quickGapCheck(profile, ctx, context)
            }
            "detailed" -> {
                val ctx = deepCtx ?: runCatching { deepAnalysis(profile, request, context) }.getOrNull()
                    ?: throw IllegalStateException("DeepTechContext required for detailed gap analysis")
                detailedGapAnalysis(profile, ctx, context)
            }
            "skip", "none" -> {
                val ctx = simpleCtx ?: runCatching { quickAnalysis(profile, request, context) }.getOrNull()
                    ?: throw IllegalStateException("SimpleTechContext required for skip gap")
                skipGapAnalysis(profile, ctx)
            }
            else -> {
                val ctx = simpleCtx ?: runCatching { quickAnalysis(profile, request, context) }.getOrNull()
                    ?: throw IllegalStateException("SimpleTechContext required for default gap")
                skipGapAnalysis(profile, ctx)
            }
        }

        publishProgress(request.agentRunId, ProgressStage.CURRICULUM_GENERATION, "커리큘럼 생성 중", "모드: $curriculumMode")

        val plan = when (curriculumMode) {
            "quick" -> {
                val ctx = simpleCtx ?: runCatching { quickAnalysis(profile, request, context) }.getOrNull()
                    ?: throw IllegalStateException("SimpleTechContext required for quick curriculum")
                val basic = generateQuickCurriculum(profile, ctx, context)
                val gapForQuick = when (gap) {
                    is NoGapAnalysis -> gap
                    else -> NoGapAnalysis(skipped = false, reason = "gap converted from ${gap::class.simpleName}")
                }
                publishProgress(request.agentRunId, ProgressStage.RESOURCE_ENRICHMENT, "학습 자료 수집 시작", "Quick 모드")
                finalizeQuickPlan(profile, ctx, basic, gapForQuick, request, context)
            }
            "detailed" -> {
                val ctx = deepCtx ?: runCatching { deepAnalysis(profile, request, context) }.getOrNull()
                    ?: throw IllegalStateException("DeepTechContext required for detailed curriculum")
                val detailedGap = when (gap) {
                    is DetailedGapAnalysis -> gap
                    is QuickGapAnalysis -> detailedGapAnalysis(profile, ctx, context)
                    is NoGapAnalysis -> detailedGapAnalysis(profile, ctx, context)
                    else -> detailedGapAnalysis(profile, ctx, context)
                }
                val cur = generateDetailedCurriculum(profile, ctx, detailedGap, context)
                publishProgress(request.agentRunId, ProgressStage.RESOURCE_ENRICHMENT, "학습 자료 수집 시작", "Detailed 모드, ${cur.steps.size}개 스텝")
                val enriched = if (resourceMode != "skip") {
                    enrichWithResources(cur, ctx, profile, context)
                } else EnrichedCurriculum(
                    cur.steps.map { s ->
                        EnrichedStep(
                            s.order, s.title, s.description, s.estimatedHours, s.prerequisites, s.keyTopics, emptyList()
                        )
                    }
                )
                finalizeDetailedPlan(profile, ctx, enriched, detailedGap, request)
            }
            else -> { // standard
                val ctx = deepCtx ?: runCatching { deepAnalysis(profile, request, context) }.getOrNull()
                    ?: throw IllegalStateException("DeepTechContext required for standard curriculum")
                val quickGap = when (gap) {
                    is QuickGapAnalysis -> gap
                    is DetailedGapAnalysis -> QuickGapAnalysis(
                        hasSignificantGaps = true,
                        identifiedGaps = gap.criticalGaps.map { it.area } + gap.minorGaps,
                        strengthAreas = gap.strengths,
                        recommendedPreparation = gap.preparationPlan.firstOrNull()?.title
                    )
                    else -> quickGapCheck(profile, ctx, context)
                }
                val cur = generateStandardCurriculum(profile, ctx, quickGap, context)
                publishProgress(request.agentRunId, ProgressStage.RESOURCE_ENRICHMENT, "학습 자료 수집 시작", "Standard 모드, ${cur.steps.size}개 스텝")
                finalizeStandardPlan(profile, ctx, cur, quickGap, request, context)
            }
        }

        publishProgress(request.agentRunId, ProgressStage.FINALIZING, "학습 플랜 최종화 중", "총 ${plan.steps.size}개 스텝")

        if (depthPlan.allowHybrid || depthPlan.hybridMix.isNotEmpty()) {
            val mix = if (depthPlan.hybridMix.isNotEmpty()) depthPlan.hybridMix else chooseHybridMix(profile, tech.displayName, context)
            val hybridPlan = composeHybridPlan(
                profile = profile,
                techDisplayName = tech.displayName,
                pathMix = mix,
                context = context
            )
            return if (hybridPlan.steps.size >= plan.steps.size) hybridPlan else plan
        }

        return plan
    }
    
    private fun chooseHybridMix(
        profile: MemberProfile,
        techName: String,
        context: OperationContext
    ): List<String> {
        return runCatching {
            context.ai()
                .withLlm(gpt41Mini)
                .create<List<String>>(
                    prompt = """
                    Decide a mixed learning path order using quick/standard/detailed segments.
                    
                    Member:
                    - Experience: ${profile.experienceLevel}
                    - Skills count: ${profile.currentSkillCount}
                    - Weekly hours: ${profile.weeklyCapacityMinutes / 60}
                    - Daily minutes: ${profile.learningPreference.dailyMinutes}
                    - Learning style: ${profile.learningPreference.learningStyle}
                    
                    Technology: $techName
                    
                    Return a JSON array with a short sequence like ["quick","standard"] or ["detailed","standard","quick"].
                    Keep length 2-3, avoid duplicates when unnecessary.
                    """.trimIndent()
                )
        }.getOrDefault(listOf("standard", "quick"))
    }
    
    @Action(
        toolGroups = [
            CoreToolGroups.WEB,
            "github"
        ]
    )
    fun deepAnalysis(
        profile: MemberProfile,
        request: LearningRequest,
        context: OperationContext
    ): DeepTechContext {
        logger.info("Deep analysis for technology: {}", request.targetTechnologyKey)
        
        val technology = resolveTechnologyDescriptor(request.targetTechnologyKey)
        
        return context.ai()
            .withLlm(gpt41Mini)
            .create(
                prompt = """
                Perform comprehensive analysis of: ${technology.displayName} (${technology.key})
                
                Research:
                - Latest version and release information
                - Official repository and documentation
                - Market demand and job trends
                - Related technologies and ecosystem
                - Common learning challenges and questions
                - Typical use cases and applications
                - Prerequisites and learning path
                
                Member Profile:
                - Experience Level: ${profile.experienceLevel.name}
                - Target Track: ${profile.targetTrack.name}
                - Current Skills: ${profile.currentSkillCount}
                
                Return comprehensive DeepTechContext with accurate, thorough information.
                """.trimIndent()
            )
    }
    
    @Action
    fun quickGapCheck(
        profile: MemberProfile,
        techContext: DeepTechContext,
        context: OperationContext
    ): QuickGapAnalysis {
        logger.info("Quick gap check for member: {}", profile.memberId)
        
        val currentSkills = memberSkillRepository.findByMemberId(profile.memberId)
        val skillNames = currentSkills.mapNotNull { skill ->
            skill.technologyId?.let { technologyRepository.findById(it).orElse(null)?.key }
                ?: skill.customName
        }.joinToString(", ")
        
        return context.ai()
            .withLlm(gpt41Mini)
            .create(
                prompt = """
                $learningArchitectPersona
                
                $gapAnalysisCoT
                
                Now analyze skill gaps for learning ${techContext.displayName}.
                
                Member Profile:
                - Experience Level: ${profile.experienceLevel.name}
                - Current Skills: $skillNames
                - Skill Count: ${profile.currentSkillCount}
                
                Technology Info:
                - Prerequisites: ${techContext.prerequisites.joinToString(", ")}
                - Related Technologies: ${techContext.relatedTechnologies.joinToString(", ")}
                
                Based on your step-by-step analysis, return a QuickGapAnalysis with:
                - hasSignificantGaps: true if member lacks critical (HIGH severity) prerequisites
                - identifiedGaps: List of missing knowledge areas with severity (max 5)
                - strengthAreas: List of existing skills that will help (max 3)
                - recommendedPreparation: Specific, actionable advice if gaps exist, null otherwise
                
                Be honest but encouraging. Focus on practical, achievable preparation steps.
                """.trimIndent()
            )
    }
    
    @Action
    fun generateStandardCurriculum(
        profile: MemberProfile,
        techContext: DeepTechContext,
        gapAnalysis: QuickGapAnalysis,
        context: OperationContext
    ): StandardCurriculum {
        logger.info("Generating standard curriculum for: {}", techContext.technologyKey)
        
        val weeklyHours = profile.weeklyCapacityMinutes / 60
        
        // RAG 지식 검색
        val ragContext = fetchRagContext(techContext.displayName)
        val ragSection = buildRagPromptSection(ragContext)
        if (ragContext.isNotBlank()) {
            logger.info("RAG context found for {}, enhancing standard curriculum prompt", techContext.displayName)
        }

        // 전문 Agent 인사이트 수집
        val specialistInsights = fetchSpecialistInsights(techContext.displayName, profile, context)
        val specialistSection = buildSpecialistPromptSection(specialistInsights)
        if (!specialistInsights.isEmpty()) {
            logger.info("Specialist insights collected for {}", techContext.displayName)
        }
        
        return context.ai()
            .withLlm(gpt41Mini)
            .withTools(CoreToolGroups.WEB)
            .create(
                prompt = """
                $learningArchitectPersona
                
                $chainOfThoughtGuideline
                
                Study these high-quality curriculum examples for reference:
                $curriculumFewShotExamples
                $ragSection
                $specialistSection
                
                Now generate a STANDARD learning curriculum for ${techContext.displayName}.
                
                Technology Info:
                - Category: ${techContext.category}
                - Ecosystem: ${techContext.ecosystem}
                - Difficulty: ${techContext.difficultyLevel}
                - Prerequisites: ${techContext.prerequisites.joinToString(", ")}
                - Use Cases: ${techContext.commonUseCases.joinToString(", ")}
                
                Member Context:
                - Experience Level: ${profile.experienceLevel.name}
                - Weekly Capacity: $weeklyHours hours
                - Learning Style: ${profile.learningPreference.learningStyle.name}
                - Prefers Korean: ${profile.learningPreference.preferKorean}
                
                Gap Analysis:
                - Has Gaps: ${gapAnalysis.hasSignificantGaps}
                - Identified Gaps: ${gapAnalysis.identifiedGaps.joinToString(", ")}
                - Strengths: ${gapAnalysis.strengthAreas.joinToString(", ")}
                
                Create a StandardCurriculum following this structure:
                - EXACTLY 5-7 steps, similar to the examples above
                - Include foundation building if gaps exist
                - Cover core concepts thoroughly
                - Add intermediate topics
                - Include practical application (mini-project)
                - Estimate hours realistically
                - Balance theory (40%) and practice (60%)
                - For each step, explain WHY it's important
                
                Return a list of StepBlueprint objects with:
                - order: 1, 2, 3, ..., 7
                - title: Clear step title (Korean if preferKorean is true)
                - description: What to learn and WHY (3-4 sentences)
                - estimatedHours: Realistic hours needed
                - prerequisites: List of prerequisite concepts
                - keyTopics: 3-5 specific, actionable topics
                
                Make it COMPREHENSIVE and BALANCED, inspired by the examples!
                """.trimIndent()
            )
    }
    
    @Action
    fun finalizeStandardPlan(
        profile: MemberProfile,
        techContext: DeepTechContext,
        curriculum: StandardCurriculum,
        gapAnalysis: QuickGapAnalysis,
        request: LearningRequest,
        context: OperationContext
    ): GeneratedLearningPlan {
        logger.info("Finalizing standard plan for member: {} / technology: {}", 
            profile.memberId, techContext.technologyKey)
        
        val totalHours = curriculum.steps.sumOf { it.estimatedHours }
        val weeklyHours = profile.weeklyCapacityMinutes / 60
        val estimatedWeeks = (totalHours.toDouble() / weeklyHours).toInt().coerceAtLeast(1)
        
        val startDate = LocalDate.now()
        val targetEndDate = startDate.plus(estimatedWeeks.toLong(), ChronoUnit.WEEKS)
        val backgroundAnalysis = buildStandardBackgroundAnalysis(profile, techContext, gapAnalysis)
        val dailySchedule = buildDailySchedule(
            profile = profile,
            startDate = startDate,
            steps = curriculum.steps.map { step ->
                StepWorkload(
                    order = step.order,
                    estimatedHours = step.estimatedHours,
                    keyTopics = step.keyTopics
                )
            }
        )
        
        val description = buildString {
            append("Standard ${techContext.displayName} learning plan for ${profile.experienceLevel.name} developers. ")
            if (gapAnalysis.hasSignificantGaps) {
                append("Includes foundation building to address knowledge gaps. ")
            }
            append("Covers ${curriculum.steps.size} comprehensive steps over $estimatedWeeks weeks.")
        }
        
        val stepsWithoutResources = curriculum.steps.map { step ->
            GeneratedStep(
                order = step.order,
                title = step.title,
                description = step.description,
                estimatedHours = step.estimatedHours,
                keyTopics = step.keyTopics,
                resources = emptyList()
            )
        }
        
        val enrichedSteps = runParallelOrdered(
            items = stepsWithoutResources,
            block = { step ->
                val resources = generateResourcesForStep(
                    step = step,
                    techName = techContext.displayName,
                    prefersKorean = profile.learningPreference.preferKorean,
                    learningStyle = profile.learningPreference.learningStyle,
                    context = context
                )
                step.copy(resources = resources)
            },
            onError = { step, ex ->
                logger.warn("Resource generation failed for step {}: {}", step.title, ex.message)
                step.copy(resources = emptyList())
            }
        )
        
        return GeneratedLearningPlan(
            memberId = profile.memberId,
            targetTechnologyKey = request.targetTechnologyKey,
            targetTechnologyName = techContext.displayName,
            title = "Standard ${techContext.displayName} Learning Path",
            description = description,
            totalEstimatedHours = totalHours,
            startDate = startDate,
            targetEndDate = targetEndDate,
            steps = enrichedSteps,
            backgroundAnalysis = backgroundAnalysis,
            dailySchedule = dailySchedule,
            metadata = PlanMetadata(
                generatedPath = com.bebeis.skillweaver.core.domain.learning.LearningPathType.STANDARD,
                llmModel = "GPT-4o-mini",
                estimatedCost = 0.20,
                generationTimeSeconds = 600,
                analysisDepth = "MODERATE",
                gapAnalysisPerformed = true,
                resourcesEnriched = true
            )
        )
    }
    
    @Action
    fun detailedGapAnalysis(
        profile: MemberProfile,
        techContext: DeepTechContext,
        context: OperationContext
    ): DetailedGapAnalysis {
        logger.info("Detailed gap analysis for member: {}", profile.memberId)
        
        val currentSkills = memberSkillRepository.findByMemberId(profile.memberId)
        val skillDetails = currentSkills.mapNotNull { skill ->
            val techName = skill.technologyId?.let { 
                technologyRepository.findById(it).orElse(null)?.displayName 
            } ?: skill.customName
            techName?.let { "$it (${skill.level.name}, ${skill.yearsOfUse} years)" }
        }.joinToString("\n- ")
        
        return context.ai()
            .withLlm(gpt41Mini)
            .withTools(CoreToolGroups.WEB)
            .create(
                prompt = """
                Perform a DETAILED gap analysis for learning ${techContext.displayName}.
                
                Member Profile:
                - Experience Level: ${profile.experienceLevel.name}
                - Target Track: ${profile.targetTrack.name}
                - Current Skills:
                  - $skillDetails
                
                Technology Requirements:
                - Prerequisites: ${techContext.prerequisites.joinToString(", ")}
                - Related Technologies: ${techContext.relatedTechnologies.joinToString(", ")}
                - Difficulty: ${techContext.difficultyLevel}
                
                Return a DetailedGapAnalysis with:
                - overallReadiness: "NOT_READY", "PARTIALLY_READY", or "READY"
                - criticalGaps: List of GapDetail (area, severity, description, recommendedAction)
                - minorGaps: List of less critical missing knowledge
                - strengths: Existing skills that will help
                - preparationPlan: List of PreparationStep to address gaps
                - estimatedPrepWeeks: Weeks needed for preparation
                
                Be thorough and provide actionable guidance for a BEGINNER.
                """.trimIndent()
            )
    }
    
    @Action
    fun generateDetailedCurriculum(
        profile: MemberProfile,
        techContext: DeepTechContext,
        gapAnalysis: DetailedGapAnalysis,
        context: OperationContext
    ): DetailedCurriculum {
        logger.info("Generating detailed curriculum for: {}", techContext.technologyKey)
        
        val weeklyHours = profile.weeklyCapacityMinutes / 60
        
        // RAG 지식 검색
        val ragContext = fetchRagContext(techContext.displayName)
        val ragSection = buildRagPromptSection(ragContext)
        if (ragContext.isNotBlank()) {
            logger.info("RAG context found for {}, enhancing detailed curriculum prompt", techContext.displayName)
        }

        // 전문 Agent 인사이트 수집
        val specialistInsights = fetchSpecialistInsights(techContext.displayName, profile, context)
        val specialistSection = buildSpecialistPromptSection(specialistInsights)
        if (!specialistInsights.isEmpty()) {
            logger.info("Specialist insights collected for {}", techContext.displayName)
        }
        
        return context.ai()
            .withLlm(gpt41Mini)
            .withTools(CoreToolGroups.WEB)
            .create(
                prompt = """
                $learningArchitectPersona
                
                $chainOfThoughtGuideline
                
                Study these curriculum examples for quality and structure reference:
                $curriculumFewShotExamples
                $ragSection
                $specialistSection
                
                Now generate a DETAILED learning curriculum for ${techContext.displayName}.
                
                Technology Info:
                - Category: ${techContext.category}
                - Ecosystem: ${techContext.ecosystem}
                - Difficulty: ${techContext.difficultyLevel}
                - Prerequisites: ${techContext.prerequisites.joinToString(", ")}
                - Common Use Cases: ${techContext.commonUseCases.joinToString(", ")}
                
                Member Context:
                - Experience Level: ${profile.experienceLevel.name} (BEGINNER - needs extra support)
                - Weekly Capacity: $weeklyHours hours
                - Learning Style: ${profile.learningPreference.learningStyle.name}
                - Prefers Korean: ${profile.learningPreference.preferKorean}
                
                Gap Analysis:
                - Overall Readiness: ${gapAnalysis.overallReadiness}
                - Critical Gaps: ${gapAnalysis.criticalGaps.size}
                - Preparation Weeks: ${gapAnalysis.estimatedPrepWeeks}
                
                Create a DetailedCurriculum with EXACTLY 8-12 steps:
                - Include ALL prerequisite foundation building
                - Start from absolute basics if needed
                - Progress gradually through fundamentals
                - Cover intermediate concepts thoroughly
                - Include multiple practice opportunities
                - Add real-world mini-project work
                - Provide clear milestones and checkpoints
                - Estimate hours generously (beginners need more time)
                - For each step, explain WHY it matters
                
                Return a list of StepBlueprint objects with:
                - order: 1, 2, 3, ..., 12
                - title: Very clear, beginner-friendly title (Korean if preferKorean)
                - description: Detailed what/why/how (4-5 sentences, encouraging tone)
                - estimatedHours: Generous, realistic time estimate
                - prerequisites: Clear prerequisite list
                - keyTopics: 4-6 specific, beginner-friendly topics
                
                Make it COMPREHENSIVE, GENTLE, and SUPPORTIVE for a BEGINNER!
                """.trimIndent()
            )
    }
    
    @Action(
        toolGroups = [
            CoreToolGroups.WEB,
            "github",
            "youtube"
        ]
    )
    fun enrichWithResources(
        curriculum: DetailedCurriculum,
        techContext: DeepTechContext,
        profile: MemberProfile,
        context: OperationContext
    ): EnrichedCurriculum {
        logger.info("Enriching curriculum with resources")
        
        val steps = runParallelOrdered(
            items = curriculum.steps,
            block = { step ->
                val resources = context.ai()
                    .withLlm(gpt41Mini)
                    .create<List<LearningResource>>(
                        prompt = """
                        Find 3-5 quality learning resources for this step:
                        
                        Step: ${step.title}
                        Topics: ${step.keyTopics.joinToString(", ")}
                        Technology: ${techContext.displayName}
                        Language Preference: ${if (profile.learningPreference.preferKorean) "Korean preferred" else "English"}
                        
                        Search for:
                        - Official documentation and guides
                        - Code examples and repositories
                        - Tutorial videos and courses
                        - Practice platforms
                        
                        Return List<LearningResource> with type, title, url, and helpful description.
                        Prioritize free, high-quality resources suitable for ${profile.experienceLevel} level.
                        """.trimIndent()
                    )
                
                EnrichedStep(
                    order = step.order,
                    title = step.title,
                    description = step.description,
                    estimatedHours = step.estimatedHours,
                    prerequisites = step.prerequisites,
                    keyTopics = step.keyTopics,
                    learningResources = resources
                )
            },
            onError = { step, ex ->
                logger.warn("Resource enrichment failed for step {}: {}", step.title, ex.message)
                EnrichedStep(
                    order = step.order,
                    title = step.title,
                    description = step.description,
                    estimatedHours = step.estimatedHours,
                    prerequisites = step.prerequisites,
                    keyTopics = step.keyTopics,
                    learningResources = emptyList()
                )
            }
        )
        
        return EnrichedCurriculum(steps)
    }
    
    @Action
    fun finalizeDetailedPlan(
        profile: MemberProfile,
        techContext: DeepTechContext,
        enrichedCurriculum: EnrichedCurriculum,
        gapAnalysis: DetailedGapAnalysis,
        request: LearningRequest
    ): GeneratedLearningPlan {
        logger.info("Finalizing detailed plan for member: {} / technology: {}", 
            profile.memberId, techContext.technologyKey)
        
        val totalHours = enrichedCurriculum.steps.sumOf { it.estimatedHours }
        val weeklyHours = profile.weeklyCapacityMinutes / 60
        val estimatedWeeks = (totalHours.toDouble() / weeklyHours).toInt().coerceAtLeast(1)
        
        val startDate = LocalDate.now()
        val targetEndDate = startDate.plus(estimatedWeeks.toLong(), ChronoUnit.WEEKS)
        val backgroundAnalysis = buildDetailedBackgroundAnalysis(profile, techContext, gapAnalysis)
        val dailySchedule = buildDailySchedule(
            profile = profile,
            startDate = startDate,
            steps = enrichedCurriculum.steps.map { step ->
                StepWorkload(
                    order = step.order,
                    estimatedHours = step.estimatedHours,
                    keyTopics = step.keyTopics
                )
            }
        )
        
        val description = buildString {
            append("Comprehensive ${techContext.displayName} learning plan for ${profile.experienceLevel.name} developers. ")
            append("Includes ${gapAnalysis.estimatedPrepWeeks} weeks of foundation building. ")
            append("${enrichedCurriculum.steps.size} detailed steps with curated resources over $estimatedWeeks weeks.")
        }
        
        return GeneratedLearningPlan(
            memberId = profile.memberId,
            targetTechnologyKey = request.targetTechnologyKey,
            targetTechnologyName = techContext.displayName,
            title = "Comprehensive ${techContext.displayName} Learning Path",
            description = description,
            totalEstimatedHours = totalHours,
            startDate = startDate,
            targetEndDate = targetEndDate,
            steps = enrichedCurriculum.steps.map { step ->
                GeneratedStep(
                    order = step.order,
                    title = step.title,
                    description = step.description,
                    estimatedHours = step.estimatedHours,
                    keyTopics = step.keyTopics,
                    resources = step.learningResources
                )
            },
            backgroundAnalysis = backgroundAnalysis,
            dailySchedule = dailySchedule,
            metadata = PlanMetadata(
                generatedPath = DETAILED,
                llmModel = "GPT-4o-mini",
                estimatedCost = 0.30,
                generationTimeSeconds = 900,
                analysisDepth = "COMPREHENSIVE",
                gapAnalysisPerformed = true,
                resourcesEnriched = true
            )
        )
    }
    
    @Condition
    fun needsDetailedPath(profile: MemberProfile): Boolean {
        val needs = profile.experienceLevel == ExperienceLevel.BEGINNER ||
                !hasSufficientSkills(profile)
        
        logger.debug("needsDetailedPath for member {}: {}", profile.memberId, needs)
        
        return needs
    }

    private fun decidePathMix(profile: MemberProfile): List<String> {
        return when (profile.experienceLevel) {
            ExperienceLevel.BEGINNER -> listOf("detailed", "standard", "quick")
            ExperienceLevel.INTERMEDIATE -> listOf("standard", "detailed", "quick")
            ExperienceLevel.ADVANCED -> listOf("standard", "quick")
        }
    }

    private fun buildHybridSteps(
        techName: String,
        pathMix: List<String>,
        weeklyHours: Int,
        prefersKorean: Boolean,
        learningStyle: LearningStyle
    ): List<GeneratedStep> {
        val steps = mutableListOf<GeneratedStep>()
        var order = 1

        pathMix.forEach { path ->
            when (path) {
                "quick" -> {
                    steps += GeneratedStep(
                        order = order++,
                        title = "Quick Start: $techName 핵심 훑기",
                        description = "짧은 시간에 전체 지형을 파악하고 핵심 API를 스케치합니다.",
                        estimatedHours = 3,
                        keyTopics = listOf("$techName 기본 개념", "주요 API/DSL", "간단 실습")
                    )
                }

                "standard" -> {
                    steps += GeneratedStep(
                        order = order++,
                        title = "Standard: 필수 개념 심화",
                        description = "필수 개념을 정리하고 작은 예제들을 만들어 봅니다.",
                        estimatedHours = 8,
                        keyTopics = listOf("$techName 핵심 패턴", "베스트 프랙티스", "테스트/디버깅")
                    )
                    steps += GeneratedStep(
                        order = order++,
                        title = "Standard: 실전 미니 프로젝트",
                        description = "주간 학습 시간을 고려한 미니 프로젝트를 수행합니다.",
                        estimatedHours = minOf(weeklyHours * 2, 12),
                        keyTopics = listOf("요구사항 정리", "기능 구현", "리팩토링")
                    )
                }

                "detailed" -> {
                    steps += GeneratedStep(
                        order = order++,
                        title = "Detailed: 기초 다지기",
                        description = "부족한 선행지식과 개념을 보강합니다.",
                        estimatedHours = 10,
                        keyTopics = listOf("필수 선행지식 복습", "빈틈 채우기", "핵심 이론 정리")
                    )
                    steps += GeneratedStep(
                        order = order++,
                        title = "Detailed: 심화+성능/안정성",
                        description = "성능·안정성·운영 관점에서 심화 학습합니다.",
                        estimatedHours = 12,
                        keyTopics = listOf("성능/메모리 고려사항", "운영/모니터링", "트러블슈팅 패턴")
                    )
                }
            }
        }

        when (learningStyle) {
            LearningStyle.PROJECT_BASED, LearningStyle.HANDS_ON -> {
                steps += GeneratedStep(
                    order = order++,
                    title = "Hands-on Lab: $techName 집중 실습",
                    description = "작은 기능을 직접 구현하며 빠르게 피드백을 얻습니다.",
                    estimatedHours = minOf(weeklyHours, 8),
                    keyTopics = listOf("실습 위주 진행", "코드 리뷰 체크리스트", "테스트 작성")
                )
            }

            LearningStyle.VIDEO_FIRST -> {
                steps += GeneratedStep(
                    order = order++,
                    title = "영상 러닝 스프린트",
                    description = "추천 동영상/코스 위주로 핵심 개념을 빠르게 흡수하고 요약 노트를 작성합니다.",
                    estimatedHours = minOf(weeklyHours, 6),
                    keyTopics = listOf("핵심 개념 요약", "데모 따라하기", "노트 정리")
                )
            }

            LearningStyle.DOC_FIRST -> {
                steps += GeneratedStep(
                    order = order++,
                    title = "문서 기반 심화 학습",
                    description = "공식 문서/가이드 위주로 읽고 샘플 코드를 재현합니다.",
                    estimatedHours = minOf(weeklyHours, 6),
                    keyTopics = listOf("공식 가이드 정독", "샘플 코드 재현", "FAQ/트러블슈팅 메모")
                )
            }

            else -> { /* BALANCED, THEORY_FIRST */ }
        }

        if (prefersKorean) {
            steps.add(
                GeneratedStep(
                    order = order++,
                    title = "정리: 한국어 자료로 복습",
                    description = "한국어 자료 위주로 전체 여정을 복습하고 노트를 정리합니다.",
                    estimatedHours = 2,
                    keyTopics = listOf("요약 노트", "용어 정리", "질문 리스트")
                )
            )
        }

        steps.add(
            GeneratedStep(
                order = order++,
                title = "마무리: ${techName} 실전 적용",
                description = "전체 흐름을 종합해 데모나 포트폴리오 산출물을 완성합니다. 학습 스타일($learningStyle)에 맞춰 진행하세요.",
                estimatedHours = 6,
                keyTopics = listOf("데모/포트폴리오", "회고", "다음 단계 정의")
            )
        )

        return steps
    }

    private fun buildHybridBackgroundAnalysis(
        profile: MemberProfile,
        techName: String,
        pathMix: List<String>,
        skillNames: List<String>,
        totalHours: Int
    ): BackgroundAnalysis {
        val recommendations = mutableListOf<String>()
        recommendations += "주당 ${(profile.weeklyCapacityMinutes / 60)}시간 기준 총 ${totalHours}시간 계획"
        recommendations += "혼합 경로 구성: ${pathMix.joinToString(", ")}"
        recommendations += "학습 스타일: ${profile.learningPreference.learningStyle}"

        val riskFactors = mutableListOf<String>()
        if (!hasSufficientSkills(profile)) {
            riskFactors += "선행지식이 부족할 수 있어요. 상세 단계(detailed)부터 차근차근 시작해 주세요."
        }
        if (profile.learningPreference.dailyMinutes < 60) {
            riskFactors += "일일 학습 시간이 짧아 기간이 늘어날 수 있음"
        }

        return BackgroundAnalysis(
            existingRelevantSkills = skillNames,
            knowledgeGaps = emptyList(),
            recommendations = recommendations,
            riskFactors = riskFactors
        )
    }

    private fun generateResourcesForStep(
        step: GeneratedStep,
        techName: String,
        prefersKorean: Boolean,
        learningStyle: LearningStyle,
        context: OperationContext,
        quickMode: Boolean = false
    ): List<LearningResource> {
        val preferredTypes = preferredResourceTypes(learningStyle)
        val resourceCount = if (quickMode) "2" else "3-4"
        
        return try {
            val aiBuilder = context.ai().withLlm(gpt41Mini)
            
            val aiWithTools = if (quickMode) {
                aiBuilder.withTools(CoreToolGroups.WEB)
            } else {
                aiBuilder.withTools(
                    CoreToolGroups.WEB,
                    "github",
                    "youtube"
                )
            }
            
            val videoSearchHint = if (!quickMode && learningStyle == LearningStyle.VIDEO_FIRST) {
                "If learning style is VIDEO_FIRST, actively search Inflearn, Udemy, and YouTube first, then fall back to other sources."
            } else ""
            
            aiWithTools.create<List<LearningResource>>(
                prompt = """
                Find $resourceCount learning resources for this step.
                
                Technology: $techName
                Step: ${step.title}
                Description: ${step.description}
                KeyTopics: ${step.keyTopics.joinToString(", ")}
                Language preference: ${if (prefersKorean) "Korean first, then English" else "English allowed, Korean if good"} 
                Preferred resource types (ordered): ${preferredTypes.joinToString(", ")} based on learning style: $learningStyle
                
                $videoSearchHint
                
                Return List<LearningResource> with type (DOC/VIDEO/BLOG/COURSE/REPO), title, url, language, description.
                Prioritize free, reputable sources. Respect preferred types first.
                """.trimIndent()
            )
        } catch (e: Exception) {
            logger.warn("Resource generation failed for step {}: {}", step.title, e.message)
            emptyList()
        }
    }

    private fun preferredResourceTypes(learningStyle: LearningStyle): List<ResourceType> {
        return when (learningStyle) {
            LearningStyle.VIDEO_FIRST -> listOf(ResourceType.VIDEO, ResourceType.COURSE, ResourceType.DOC, ResourceType.BLOG, ResourceType.REPO)
            LearningStyle.PROJECT_BASED, LearningStyle.HANDS_ON -> listOf(ResourceType.REPO, ResourceType.PROJECT, ResourceType.DOC, ResourceType.VIDEO, ResourceType.BLOG)
            LearningStyle.DOC_FIRST, LearningStyle.THEORY_FIRST -> listOf(ResourceType.DOC, ResourceType.DOCUMENTATION, ResourceType.BLOG, ResourceType.VIDEO, ResourceType.REPO)
            else -> listOf(ResourceType.DOC, ResourceType.VIDEO, ResourceType.BLOG, ResourceType.REPO, ResourceType.COURSE)
        }
    }

    private fun <T, R> runParallelOrdered(
        items: List<T>,
        block: (T) -> R,
        onError: (T, Throwable) -> R
    ): List<R> {
        if (items.isEmpty()) return emptyList()

        val futures = items.mapIndexed { index, item ->
            CompletableFuture
                .supplyAsync({ index to block(item) }, resourceExecutor)
                .exceptionally { ex -> index to onError(item, ex) }
        }

        return futures
            .map { it.join() }
            .sortedBy { it.first }
            .map { it.second }
    }

    private fun buildQuickBackgroundAnalysis(
        profile: MemberProfile,
        techContext: SimpleTechContext,
        gapAnalysis: NoGapAnalysis
    ): BackgroundAnalysis {
        val skills = loadSkillNames(profile.memberId)
        val recommendations = mutableListOf(
            "빠른 경로로 ${techContext.estimatedLearningWeeks}주 내 핵심만 집중",
            "주당 ${profile.weeklyCapacityMinutes / 60}시간 기준으로 설계"
        )
        recommendations += "프로젝트 중심 학습 스타일을 활용해 바로 실습하세요"

        val riskFactors = mutableListOf<String>()
        if (gapAnalysis.skipped) {
            riskFactors += "갭 분석을 생략했으므로 필수 선행지식을 직접 점검하세요"
        }

        return BackgroundAnalysis(
            existingRelevantSkills = skills,
            knowledgeGaps = emptyList(),
            recommendations = recommendations,
            riskFactors = riskFactors,
            rawText = gapAnalysis.reason
        )
    }

    private fun buildStandardBackgroundAnalysis(
        profile: MemberProfile,
        techContext: DeepTechContext,
        gapAnalysis: QuickGapAnalysis
    ): BackgroundAnalysis {
        val skills = loadSkillNames(profile.memberId)
        val knowledgeGaps = gapAnalysis.identifiedGaps.ifEmpty { techContext.prerequisites }
        val recommendations = mutableListOf<String>()
        gapAnalysis.recommendedPreparation?.takeIf { it.isNotBlank() }?.let { recommendations += it }
        recommendations += "주당 ${profile.weeklyCapacityMinutes / 60}시간 기준 표준 경로"
        recommendations += "학습 스타일: ${profile.learningPreference.learningStyle}"

        val riskFactors = mutableListOf<String>()
        if (gapAnalysis.hasSignificantGaps) {
            riskFactors += "핵심 선행지식 부족: ${knowledgeGaps.take(3).joinToString(", ")}"
        }

        return BackgroundAnalysis(
            existingRelevantSkills = (skills + gapAnalysis.strengthAreas).distinct(),
            knowledgeGaps = knowledgeGaps,
            recommendations = recommendations,
            riskFactors = riskFactors
        )
    }

    private fun buildDetailedBackgroundAnalysis(
        profile: MemberProfile,
        techContext: DeepTechContext,
        gapAnalysis: DetailedGapAnalysis
    ): BackgroundAnalysis {
        val knowledgeGaps = (gapAnalysis.criticalGaps.map { it.area } + gapAnalysis.minorGaps).distinct()
        val recommendations = gapAnalysis.preparationPlan
            .take(3)
            .map { "예열 단계 ${it.order}: ${it.title} (${it.estimatedHours}h)" }
            .ifEmpty { listOf("기초 보강 후 본 학습 진행") }
        val riskFactors = gapAnalysis.criticalGaps.map {
            "${it.area} (${it.severity}) - ${it.recommendedAction}"
        }.ifEmpty { listOf("초보자 경로 - 추가 시간이 필요할 수 있음") }

        return BackgroundAnalysis(
            existingRelevantSkills = gapAnalysis.strengths.ifEmpty { loadSkillNames(profile.memberId) },
            knowledgeGaps = knowledgeGaps,
            recommendations = recommendations,
            riskFactors = riskFactors
        )
    }

    private fun buildDailySchedule(
        profile: MemberProfile,
        startDate: LocalDate,
        steps: List<StepWorkload>
    ): List<DailyScheduleItem> {
        if (steps.isEmpty()) return emptyList()

        val schedule = mutableListOf<DailyScheduleItem>()
        val dailyMinutesCapacity = max(45, profile.learningPreference.dailyMinutes)
        var dayIndex = 1

        steps.sortedBy { it.order }.forEach { step ->
            var remainingMinutes = max(30, step.estimatedHours * 60)
            val tasks = if (step.keyTopics.isNotEmpty()) {
                listOf("집중 토픽: ${step.keyTopics.take(3).joinToString(", ")}")
            } else emptyList()

            while (remainingMinutes > 0) {
                val allocation = min(dailyMinutesCapacity, remainingMinutes)
                schedule.add(
                    DailyScheduleItem(
                        dayIndex = dayIndex,
                        date = startDate.plusDays((dayIndex - 1).toLong()),
                        allocatedMinutes = allocation,
                        stepRef = step.order,
                        tasks = tasks
                    )
                )
                remainingMinutes -= allocation
                dayIndex++
            }
        }

        return schedule
    }

    private fun loadSkillNames(memberId: Long): List<String> {
        return memberSkillRepository.findByMemberId(memberId).mapNotNull { skill ->
            skill.technologyId?.let { techId ->
                technologyRepository.findById(techId).orElse(null)?.displayName
            } ?: skill.customName
        }.filter { it.isNotBlank() }
            .map { it.trim() }
            .distinct()
    }

    private data class StepWorkload(
        val order: Int,
        val estimatedHours: Int,
        val keyTopics: List<String>
    )

    private fun resolveTechnologyDescriptor(rawInput: String): TechnologyDescriptor {
        val trimmed = rawInput.trim()
        require(trimmed.isNotBlank()) { "Target technology is required" }

        val lower = trimmed.lowercase(Locale.getDefault())
        val normalized = lower.replace("[^a-z0-9]+".toRegex(), "-")
            .replace("-+".toRegex(), "-")
            .trim('-')

        val candidates = listOf(trimmed, lower, normalized)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val technology = candidates
            .asSequence()
            .mapNotNull { key -> technologyRepository.findByKey(key) }
            .firstOrNull()

        return technology?.let {
            TechnologyDescriptor(
                key = it.key,
                displayName = it.displayName,
                category = it.category,
                ecosystem = it.ecosystem ?: "General",
                officialSite = it.officialSite,
                isFallback = false
            )
        } ?: run {
            val displayName = trimmed.split("-", "_", " ")
                .filter { part -> part.isNotBlank() }
                .joinToString(" ") { part ->
                    part.replaceFirstChar { ch ->
                        if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                    }
                }
                .ifBlank { trimmed }
            logger.warn(
                "Technology '{}' not found in repository. Using fallback descriptor.",
                rawInput
            )
            TechnologyDescriptor(
                key = normalized.ifBlank { lower },
                displayName = displayName,
                category = TechnologyCategory.FRAMEWORK,
                ecosystem = "General",
                officialSite = null,
                isFallback = true
            )
        }
    }

    private data class TechnologyDescriptor(
        val key: String,
        val displayName: String,
        val category: TechnologyCategory,
        val ecosystem: String,
        val officialSite: String?,
        val isFallback: Boolean
    )

    @PreDestroy
    fun shutdownResourceExecutor() {
        resourceExecutor.shutdown()
    }
}
