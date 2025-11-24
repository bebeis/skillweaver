package com.bebeis.skillweaver.agent

import com.bebeis.skillweaver.agent.domain.*
import com.bebeis.skillweaver.core.domain.learning.LearningPathType.DETAILED
import com.bebeis.skillweaver.core.domain.member.ExperienceLevel
import com.bebeis.skillweaver.core.domain.member.LearningStyle
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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Agent(description = "Generate personalized learning plan for new technology")
@Component
class NewTechLearningAgent(
    private val memberRepository: MemberRepository,
    private val technologyRepository: TechnologyRepository,
    private val memberSkillRepository: MemberSkillRepository
) {
    
    private val logger = LoggerFactory.getLogger(NewTechLearningAgent::class.java)
    
    private val gpt4oMini = LlmOptions(
        model = "gpt-4o-mini",
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
    @Condition("hasSufficientSkills || prefersFastPlan")
    fun quickAnalysis(
        profile: MemberProfile,
        request: LearningRequest,
        context: OperationContext
    ): SimpleTechContext {
        logger.info("Quick analysis for technology: {}", request.targetTechnologyKey)
        
        val technology = technologyRepository.findByKey(request.targetTechnologyKey)
            ?: throw IllegalArgumentException("Technology not found: ${request.targetTechnologyKey}")
        
        return context.ai()
            .withLlm(gpt4oMini)
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
    @Condition("hasSufficientSkills && prefersFastPlan")
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
    @Condition("prefersFastPlan")
    fun generateQuickCurriculum(
        profile: MemberProfile,
        techContext: SimpleTechContext,
        context: OperationContext
    ): BasicCurriculum {
        logger.info("Generating quick curriculum for: {}", techContext.technologyKey)
        
        val weeklyHours = profile.weeklyCapacityMinutes / 60
        
        return context.ai()
            .withLlm(gpt4oMini)
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
    @AchievesGoal(description = "Return the generated learning plan")
    fun returnGeneratedPlan(
        plan: GeneratedLearningPlan
    ): GeneratedLearningPlan {
        logger.info("Goal achieved! Returning GeneratedLearningPlan for member: {} / technology: {}", 
            plan.memberId, plan.targetTechnologyKey)
        
        return plan
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
    
    @Condition
    fun prefersFastPlan(profile: MemberProfile): Boolean {
        val prefers = profile.learningPreference.learningStyle == LearningStyle.PROJECT_BASED &&
                profile.experienceLevel == ExperienceLevel.ADVANCED
        
        logger.debug("prefersFastPlan for member {}: {} (style: {}, level: {})",
            profile.memberId, prefers, profile.learningPreference.learningStyle, profile.experienceLevel)
        
        return prefers
    }
    
    @Action(
        toolGroups = [
            CoreToolGroups.WEB,
            "github"
        ]
    )
    @Condition("!prefersFastPlan")
    fun deepAnalysis(
        profile: MemberProfile,
        request: LearningRequest,
        context: OperationContext
    ): DeepTechContext {
        logger.info("Deep analysis for technology: {}", request.targetTechnologyKey)
        
        val technology = technologyRepository.findByKey(request.targetTechnologyKey)
            ?: throw IllegalArgumentException("Technology not found: ${request.targetTechnologyKey}")
        
        return context.ai()
            .withLlm(gpt4oMini)
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
    @Condition("hasSufficientSkills && !prefersFastPlan")
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
            .withLlm(gpt4oMini)
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
    @Condition("hasSufficientSkills && !prefersFastPlan")
    fun generateStandardCurriculum(
        profile: MemberProfile,
        techContext: DeepTechContext,
        gapAnalysis: QuickGapAnalysis,
        context: OperationContext
    ): StandardCurriculum {
        logger.info("Generating standard curriculum for: {}", techContext.technologyKey)
        
        val weeklyHours = profile.weeklyCapacityMinutes / 60
        
        return context.ai()
            .withLlm(gpt4oMini)
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
    
    @Condition
    fun needsStandardPath(profile: MemberProfile): Boolean {
        val needs = !prefersFastPlan(profile) && 
                profile.experienceLevel == ExperienceLevel.INTERMEDIATE
        
        logger.debug("needsStandardPath for member {}: {}", profile.memberId, needs)
        
        return needs
    }
    
    @Action
    @Condition("!hasSufficientSkills")
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
            .withLlm(gpt4oMini)
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
    @Condition("!hasSufficientSkills")
    fun generateDetailedCurriculum(
        profile: MemberProfile,
        techContext: DeepTechContext,
        gapAnalysis: DetailedGapAnalysis,
        context: OperationContext
    ): DetailedCurriculum {
        logger.info("Generating detailed curriculum for: {}", techContext.technologyKey)
        
        val weeklyHours = profile.weeklyCapacityMinutes / 60
        
        return context.ai()
            .withLlm(gpt4oMini)
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
    @Condition("!hasSufficientSkills")
    fun enrichWithResources(
        curriculum: DetailedCurriculum,
        techContext: DeepTechContext,
        profile: MemberProfile,
        context: OperationContext
    ): EnrichedCurriculum {
        logger.info("Enriching curriculum with resources")
        
        val steps = curriculum.steps.map { step ->
            val resources = context.ai()
                .withLlm(gpt4oMini)
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
        }
        
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
                    resources = step.learningResources.map { 
                        "${it.type}: ${it.title}${it.url?.let { url -> " ($url)" } ?: ""}"
                    }
                )
            },
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
}
