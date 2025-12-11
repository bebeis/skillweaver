package com.bebeis.skillweaver.api.technology.dto

import com.bebeis.skillweaver.agent.graph.*

/**
 * V4 통합 기술 API DTO
 * 
 * RDB Technology API와 Graph API를 통합한 요청/응답 DTO
 */

// =============================================================================
// 요청 DTO
// =============================================================================

/**
 * 기술 생성 요청
 */
data class CreateTechnologyGraphRequest(
    val name: String,
    val displayName: String,
    val category: TechCategory,
    val difficulty: Difficulty = Difficulty.INTERMEDIATE,
    val ecosystem: String? = null,
    val officialSite: String? = null,
    val description: String? = null,
    val learningRoadmap: String? = null,
    val estimatedLearningHours: Int? = null,
    val learningTips: String? = null,
    val useCases: List<String> = emptyList(),
    val communityPopularity: Int? = null,
    val jobMarketDemand: Int? = null,
    val relations: List<TechRelationRequest> = emptyList()
) {
    fun toTechNode() = TechNode(
        name = name,
        displayName = displayName,
        category = category,
        difficulty = difficulty,
        ecosystem = ecosystem,
        officialSite = officialSite,
        description = description,
        learningRoadmap = learningRoadmap,
        estimatedLearningHours = estimatedLearningHours,
        learningTips = learningTips,
        useCases = useCases,
        communityPopularity = communityPopularity,
        jobMarketDemand = jobMarketDemand
    )
}

/**
 * 기술 수정 요청
 */
data class UpdateTechnologyGraphRequest(
    val displayName: String? = null,
    val category: TechCategory? = null,
    val difficulty: Difficulty? = null,
    val ecosystem: String? = null,
    val officialSite: String? = null,
    val active: Boolean? = null,
    val description: String? = null,
    val learningRoadmap: String? = null,
    val estimatedLearningHours: Int? = null,
    val learningTips: String? = null,
    val useCases: List<String>? = null,
    val communityPopularity: Int? = null,
    val jobMarketDemand: Int? = null
) {
    fun toTechNodeUpdate() = TechNodeUpdate(
        displayName = displayName,
        category = category,
        difficulty = difficulty,
        ecosystem = ecosystem,
        officialSite = officialSite,
        active = active,
        description = description,
        learningRoadmap = learningRoadmap,
        estimatedLearningHours = estimatedLearningHours,
        learningTips = learningTips,
        useCases = useCases,
        communityPopularity = communityPopularity,
        jobMarketDemand = jobMarketDemand
    )
}

/**
 * 관계 생성 요청
 */
data class TechRelationRequest(
    val to: String,
    val relation: TechRelation,
    val weight: Double = 1.0
)

/**
 * 갭 분석 요청
 */
data class GapAnalysisGraphRequest(
    val knownTechnologies: List<String>,
    val targetTechnology: String
)

// =============================================================================
// 응답 DTO
// =============================================================================

/**
 * 기술 상세 응답
 */
data class TechnologyGraphResponse(
    val name: String,
    val displayName: String,
    val category: String,
    val difficulty: String,
    val ecosystem: String?,
    val officialSite: String?,
    val active: Boolean,
    val description: String?,
    val learningRoadmap: String?,
    val estimatedLearningHours: Int?,
    val learningTips: String?,
    val useCases: List<String>,
    val communityPopularity: Int?,
    val jobMarketDemand: Int?
) {
    companion object {
        fun from(node: TechNode) = TechnologyGraphResponse(
            name = node.name,
            displayName = node.displayName,
            category = node.category.name,
            difficulty = node.difficulty.name,
            ecosystem = node.ecosystem,
            officialSite = node.officialSite,
            active = node.active,
            description = node.description,
            learningRoadmap = node.learningRoadmap,
            estimatedLearningHours = node.estimatedLearningHours,
            learningTips = node.learningTips,
            useCases = node.useCases,
            communityPopularity = node.communityPopularity,
            jobMarketDemand = node.jobMarketDemand
        )
    }
}

/**
 * 기술 상세 응답 (관계 포함)
 */
data class TechnologyGraphDetailResponse(
    val name: String,
    val displayName: String,
    val category: String,
    val difficulty: String,
    val ecosystem: String?,
    val officialSite: String?,
    val active: Boolean,
    val description: String?,
    val learningRoadmap: String?,
    val estimatedLearningHours: Int?,
    val learningTips: String?,
    val useCases: List<String>,
    val communityPopularity: Int?,
    val jobMarketDemand: Int?,
    val prerequisites: PrerequisitesGraphDto?,
    val relatedTechnologies: List<TechNodeSummaryDto>
) {
    companion object {
        fun from(
            node: TechNode,
            prerequisites: Prerequisites?,
            related: List<TechNode>
        ) = TechnologyGraphDetailResponse(
            name = node.name,
            displayName = node.displayName,
            category = node.category.name,
            difficulty = node.difficulty.name,
            ecosystem = node.ecosystem,
            officialSite = node.officialSite,
            active = node.active,
            description = node.description,
            learningRoadmap = node.learningRoadmap,
            estimatedLearningHours = node.estimatedLearningHours,
            learningTips = node.learningTips,
            useCases = node.useCases,
            communityPopularity = node.communityPopularity,
            jobMarketDemand = node.jobMarketDemand,
            prerequisites = prerequisites?.let { PrerequisitesGraphDto.from(it) },
            relatedTechnologies = related.map { TechNodeSummaryDto.from(it) }
        )
    }
}

/**
 * 기술 목록 응답
 */
data class TechnologyGraphListResponse(
    val technologies: List<TechnologyGraphResponse>,
    val totalCount: Int
)

/**
 * 기술 노드 요약 DTO
 */
data class TechNodeSummaryDto(
    val name: String,
    val displayName: String,
    val category: String,
    val difficulty: String
) {
    companion object {
        fun from(node: TechNode) = TechNodeSummaryDto(
            name = node.name,
            displayName = node.displayName,
            category = node.category.name,
            difficulty = node.difficulty.name
        )
    }
}

/**
 * 선행 지식 DTO
 */
data class PrerequisitesGraphDto(
    val required: List<TechNodeSummaryDto>,
    val recommended: List<TechNodeSummaryDto>
) {
    companion object {
        fun from(prereqs: Prerequisites) = PrerequisitesGraphDto(
            required = prereqs.required.map { TechNodeSummaryDto.from(it) },
            recommended = prereqs.recommended.map { TechNodeSummaryDto.from(it) }
        )
    }
}

/**
 * 로드맵 응답
 */
data class RoadmapGraphResponse(
    val technology: String,
    val displayName: String,
    val prerequisites: PrerequisitesGraphDto,
    val nextSteps: List<TechNodeSummaryDto>
)

/**
 * 학습 경로 응답
 */
data class LearningPathGraphResponse(
    val from: String,
    val to: String,
    val totalSteps: Int,
    val path: List<PathStepDto>
) {
    companion object {
        fun from(learningPath: LearningPath) = LearningPathGraphResponse(
            from = learningPath.from,
            to = learningPath.to,
            totalSteps = learningPath.steps.size,
            path = learningPath.steps.mapIndexed { index, step ->
                PathStepDto(
                    step = index + 1,
                    technology = step.technology,
                    relation = step.relation.name
                )
            }
        )
    }
}

data class PathStepDto(
    val step: Int,
    val technology: String,
    val relation: String
)

/**
 * 추천 응답
 */
data class RecommendationsGraphResponse(
    val technology: String,
    val recommendations: List<RecommendationGraphDto>
)

data class RecommendationGraphDto(
    val name: String,
    val displayName: String,
    val category: String,
    val relation: String = "USED_WITH"
) {
    companion object {
        fun from(node: TechNode) = RecommendationGraphDto(
            name = node.name,
            displayName = node.displayName,
            category = node.category.name
        )
    }
}

/**
 * 갭 분석 응답
 */
data class GapAnalysisGraphResponse(
    val target: String,
    val known: List<String>,
    val missing: List<MissingTechGraphDto>,
    val ready: Boolean,
    val readinessScore: Double,
    val message: String
)

data class MissingTechGraphDto(
    val name: String,
    val displayName: String,
    val priority: String
)

/**
 * 관계 응답
 */
data class TechRelationshipGraphResponse(
    val from: String,
    val to: String,
    val relation: String,
    val weight: Double
) {
    companion object {
        fun from(edge: TechEdge) = TechRelationshipGraphResponse(
            from = edge.from,
            to = edge.to,
            relation = edge.relation.name,
            weight = edge.weight
        )
    }
}
