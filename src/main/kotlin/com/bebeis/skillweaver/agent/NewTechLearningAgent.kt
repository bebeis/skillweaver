package com.bebeis.skillweaver.agent

import com.bebeis.skillweaver.agent.domain.*
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
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Agent(description = "Generate personalized learning plan for new technology")
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
    
    @Action
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
                Provide a quick analysis of the technology: ${technology.displayName} (${technology.key})
                
                Member Profile:
                - Experience Level: ${profile.experienceLevel.name}
                - Target Track: ${profile.targetTrack.name}
                - Current Skills: ${profile.currentSkillCount}
                
                Return a SimpleTechContext with:
                - technologyKey: ${technology.key}
                - displayName: ${technology.displayName}
                - category: ${technology.category.name}
                - briefDescription: A short description (2-3 sentences)
                - estimatedLearningWeeks: Estimated weeks to learn (considering member's experience)
                - difficultyLevel: "EASY", "MEDIUM", or "HARD" (relative to member's experience)
                
                Keep it brief and focus on essential information only.
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
            .create(
                prompt = """
                Generate a QUICK learning curriculum for ${techContext.displayName}.
                
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
                generatedPath = "QUICK",
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
}
