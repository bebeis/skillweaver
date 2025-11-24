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
import kotlin.math.max
import kotlin.math.min

@Agent(description = "Generate personalized learning plan for new technology")
@Component
class NewTechLearningAgent(
    private val memberRepository: MemberRepository,
    private val technologyRepository: TechnologyRepository,
    private val memberSkillRepository: MemberSkillRepository
) {
    
    private val logger = LoggerFactory.getLogger(NewTechLearningAgent::class.java)
    private val resourceExecutor: ExecutorService = Executors.newFixedThreadPool(
        max(4, Runtime.getRuntime().availableProcessors().coerceAtMost(8))
    )
    
    private val gpt41Mini = LlmOptions(
        model = "gpt-4.1-mini",
        temperature = 0.7
    )
    
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
        
        return context.ai()
            .withLlm(gpt41Mini)
            .withTools(CoreToolGroups.WEB)
            .create(
                prompt = """
                Generate a QUICK learning curriculum for ${techContext.displayName}.
                
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
                - Keep prerequisites and keyTopics minimal
                
                Return a list of StepBlueprint objects with:
                - order: 1, 2, 3, (4)
                - title: Brief step title
                - description: What to learn in this step (2-3 sentences)
                - estimatedHours: Hours needed (total should fit weekly capacity)
                - prerequisites: List of prerequisite concepts (can be empty for first step)
                - keyTopics: 2-3 key topics to cover
                
                Make it QUICK and PRACTICAL for an experienced developer!
                """.trimIndent()
            )
    }
    
    @Action
    fun finalizeQuickPlan(
        profile: MemberProfile,
        techContext: SimpleTechContext,
        curriculum: BasicCurriculum,
        gapAnalysis: NoGapAnalysis,
        request: LearningRequest
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
                    steps = curriculum.steps.map { step ->
                GeneratedStep(
                    order = step.order,
                    title = step.title,
                    description = step.description,
                    estimatedHours = step.estimatedHours,
                    keyTopics = step.keyTopics,
                    resources = emptyList()
                )
            },
            backgroundAnalysis = backgroundAnalysis,
            dailySchedule = dailySchedule,
            metadata = PlanMetadata(
                generatedPath = com.bebeis.skillweaver.core.domain.learning.LearningPathType.QUICK,
                llmModel = "GPT-4o-mini",
                estimatedCost = 0.05,
                generationTimeSeconds = 180,
                analysisDepth = "SIMPLE",
                gapAnalysisPerformed = false,
                resourcesEnriched = false
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

        val analysisMode = depthPlan.analysisMode.lowercase(Locale.getDefault())
        val gapMode = depthPlan.gapMode.lowercase(Locale.getDefault())
        val curriculumMode = depthPlan.curriculumMode.lowercase(Locale.getDefault())
        val resourceMode = depthPlan.resourceMode.lowercase(Locale.getDefault())

        val simpleCtx = when (analysisMode) {
            "quick" -> runCatching { quickAnalysis(profile, request, context) }.getOrNull()
            "skip" -> null
            else -> runCatching { quickAnalysis(profile, request, context) }.getOrNull()
        }
        val deepCtx = when (analysisMode) {
            "detailed", "standard" -> runCatching { deepAnalysis(profile, request, context) }.getOrNull()
            "quick" -> null
            else -> runCatching { deepAnalysis(profile, request, context) }.getOrNull()
        }

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

        val plan = when (curriculumMode) {
            "quick" -> {
                val ctx = simpleCtx ?: runCatching { quickAnalysis(profile, request, context) }.getOrNull()
                    ?: throw IllegalStateException("SimpleTechContext required for quick curriculum")
                val basic = generateQuickCurriculum(profile, ctx, context)
                val gapForQuick = when (gap) {
                    is NoGapAnalysis -> gap
                    else -> NoGapAnalysis(skipped = false, reason = "gap converted from ${gap::class.simpleName}")
                }
                finalizeQuickPlan(profile, ctx, basic, gapForQuick, request)
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
                finalizeStandardPlan(profile, ctx, cur, quickGap, request)
            }
        }

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
                Analyze skill gaps for learning ${techContext.displayName}.
                
                Member Profile:
                - Experience Level: ${profile.experienceLevel.name}
                - Current Skills: $skillNames
                - Skill Count: ${profile.currentSkillCount}
                
                Technology Info:
                - Prerequisites: ${techContext.prerequisites.joinToString(", ")}
                - Related Technologies: ${techContext.relatedTechnologies.joinToString(", ")}
                
                Return a QuickGapAnalysis with:
                - hasSignificantGaps: true if member lacks critical prerequisites
                - identifiedGaps: List of missing knowledge areas (max 5)
                - strengthAreas: List of existing skills that will help (max 3)
                - recommendedPreparation: Brief advice if gaps exist, null otherwise
                
                Be honest but encouraging for an INTERMEDIATE developer.
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
        
        return context.ai()
            .withLlm(gpt41Mini)
            .withTools(CoreToolGroups.WEB)
            .create(
                prompt = """
                Generate a STANDARD learning curriculum for ${techContext.displayName}.
                
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
                
                Create a StandardCurriculum with EXACTLY 5-7 steps:
                - Include foundation building if gaps exist
                - Cover core concepts thoroughly
                - Add intermediate topics
                - Include practical application
                - Estimate hours realistically
                - Balance theory and practice
                
                Return a list of StepBlueprint objects with:
                - order: 1, 2, 3, ..., 7
                - title: Clear step title
                - description: What to learn (3-4 sentences)
                - estimatedHours: Hours needed
                - prerequisites: List of prerequisite concepts
                - keyTopics: 3-5 key topics to cover
                
                Make it COMPREHENSIVE and BALANCED for an INTERMEDIATE developer!
                """.trimIndent()
            )
    }
    
    @Action
    fun finalizeStandardPlan(
        profile: MemberProfile,
        techContext: DeepTechContext,
        curriculum: StandardCurriculum,
        gapAnalysis: QuickGapAnalysis,
        request: LearningRequest
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
        
        return GeneratedLearningPlan(
            memberId = profile.memberId,
            targetTechnologyKey = request.targetTechnologyKey,
            targetTechnologyName = techContext.displayName,
            title = "Standard ${techContext.displayName} Learning Path",
            description = description,
            totalEstimatedHours = totalHours,
            startDate = startDate,
            targetEndDate = targetEndDate,
            steps = curriculum.steps.map { step ->
                GeneratedStep(
                    order = step.order,
                    title = step.title,
                    description = step.description,
                    estimatedHours = step.estimatedHours,
                    keyTopics = step.keyTopics,
                    resources = emptyList()
                )
            },
            backgroundAnalysis = backgroundAnalysis,
            dailySchedule = dailySchedule,
            metadata = PlanMetadata(
                generatedPath = com.bebeis.skillweaver.core.domain.learning.LearningPathType.STANDARD,
                llmModel = "GPT-4o-mini",
                estimatedCost = 0.15,
                generationTimeSeconds = 480,
                analysisDepth = "MODERATE",
                gapAnalysisPerformed = true,
                resourcesEnriched = false
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
        
        return context.ai()
            .withLlm(gpt41Mini)
            .withTools(CoreToolGroups.WEB)
            .create(
                prompt = """
                Generate a DETAILED learning curriculum for ${techContext.displayName}.
                
                Technology Info:
                - Category: ${techContext.category}
                - Ecosystem: ${techContext.ecosystem}
                - Difficulty: ${techContext.difficultyLevel}
                - Prerequisites: ${techContext.prerequisites.joinToString(", ")}
                - Common Use Cases: ${techContext.commonUseCases.joinToString(", ")}
                
                Member Context:
                - Experience Level: ${profile.experienceLevel.name} (BEGINNER)
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
                - Add real-world project work
                - Provide clear milestones
                - Estimate hours generously
                
                Return a list of StepBlueprint objects with:
                - order: 1, 2, 3, ..., 12
                - title: Very clear, beginner-friendly title
                - description: Detailed what/why/how (4-5 sentences)
                - estimatedHours: Generous time estimate
                - prerequisites: Clear prerequisite list
                - keyTopics: 4-6 specific topics to master
                
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
        context: OperationContext
    ): List<LearningResource> {
        val preferredTypes = preferredResourceTypes(learningStyle)
        return try {
            context.ai()
                .withLlm(gpt41Mini)
                .withTools(
                    CoreToolGroups.WEB,
                    "github",
                    "youtube"
                )
                .create<List<LearningResource>>(
                    prompt = """
                    Find 3-4 learning resources for this step.
                    
                    Technology: $techName
                    Step: ${step.title}
                    Description: ${step.description}
                    KeyTopics: ${step.keyTopics.joinToString(", ")}
                    Language preference: ${if (prefersKorean) "Korean first, then English" else "English allowed, Korean if good"} 
                    Preferred resource types (ordered): ${preferredTypes.joinToString(", ")} based on learning style: $learningStyle
                    
                    If learning style is VIDEO_FIRST, actively search Inflearn, Udemy, and YouTube first, then fall back to other sources.
                    
                    Return List<LearningResource> with type (DOC/VIDEO/BLOG/COURSE/REPO), title, url, language, description.
                    Prioritize free, reputable sources. Respect preferred types first. Include at least one hands-on/repository or project-style resource if applicable.
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
