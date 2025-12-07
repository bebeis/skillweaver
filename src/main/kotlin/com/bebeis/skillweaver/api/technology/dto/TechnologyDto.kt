package com.bebeis.skillweaver.api.technology.dto

import com.bebeis.skillweaver.api.common.dto.PaginationResponse
import com.bebeis.skillweaver.core.domain.technology.KnowledgeSource
import com.bebeis.skillweaver.core.domain.technology.RelationType
import com.bebeis.skillweaver.core.domain.technology.Technology
import com.bebeis.skillweaver.core.domain.technology.TechnologyCategory
import java.time.LocalDateTime

data class TechnologySummaryResponse(
    val technologyId: Long,
    val key: String,
    val displayName: String,
    val category: TechnologyCategory,
    val ecosystem: String?,
    val officialSite: String?,
    val active: Boolean
) {
    companion object {
        fun from(technology: Technology): TechnologySummaryResponse {
            return TechnologySummaryResponse(
                technologyId = technology.technologyId!!,
                key = technology.key,
                displayName = technology.displayName,
                category = technology.category,
                ecosystem = technology.ecosystem,
                officialSite = technology.officialSite,
                active = technology.active
            )
        }
    }
}

data class TechnologyDetailResponse(
    val technologyId: Long,
    val key: String,
    val displayName: String,
    val category: TechnologyCategory,
    val ecosystem: String?,
    val officialSite: String?,
    val active: Boolean,
    val knowledge: TechnologyKnowledgeResponse?,
    val prerequisites: List<TechPrerequisiteResponse>,
    val useCases: List<String>,
    
    // Phase 3: 학습 메타데이터
    val learningRoadmap: String?,
    val estimatedLearningHours: Int?,
    val relatedTechnologies: List<String>,
    val communityPopularity: Int?,
    val jobMarketDemand: Int?
)

data class TechnologyKnowledgeResponse(
    val technologyKnowledgeId: Long,
    val technologyId: Long,
    val summary: String?,
    val learningTips: String?,
    val sourceType: KnowledgeSource,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class TechPrerequisiteResponse(
    val prerequisiteKey: String,
    val displayName: String?
)

data class TechnologyUseCaseResponse(
    val useCaseId: Long,
    val knowledgeId: Long,
    val useCase: String
)

data class TechnologyListResponse(
    val technologies: List<TechnologySummaryResponse>,
    val pagination: PaginationResponse
)

data class CreateTechnologyRequest(
    val key: String,
    val displayName: String,
    val category: TechnologyCategory,
    val ecosystem: String? = null,
    val officialSite: String? = null,
    
    // Phase 3: 학습 메타데이터
    val learningRoadmap: String? = null,
    val estimatedLearningHours: Int? = null,
    val prerequisites: List<String> = emptyList(),
    val relatedTechnologies: List<String> = emptyList(),
    val communityPopularity: Int? = null,
    val jobMarketDemand: Int? = null
)

data class UpdateTechnologyRequest(
    val displayName: String? = null,
    val ecosystem: String? = null,
    val officialSite: String? = null,
    val active: Boolean? = null,
    
    // Phase 3: 학습 메타데이터
    val learningRoadmap: String? = null,
    val estimatedLearningHours: Int? = null,
    val prerequisites: List<String>? = null,
    val relatedTechnologies: List<String>? = null,
    val communityPopularity: Int? = null,
    val jobMarketDemand: Int? = null
)

data class UpsertTechnologyKnowledgeRequest(
    val summary: String?,
    val learningTips: String?,
    val sourceType: KnowledgeSource = KnowledgeSource.COMMUNITY
)

data class TechnologyKnowledgeMutationResponse(
    val technologyKnowledgeId: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class CreateTechPrerequisiteRequest(
    val prerequisiteKey: String
)

data class CreateTechUseCaseRequest(
    val useCase: String
)

data class TechRelationshipResponse(
    val techRelationshipId: Long,
    val fromTechnology: RelatedTechnologyResponse,
    val toTechnology: RelatedTechnologyResponse,
    val relationType: RelationType,
    val weight: Int
)

data class RelatedTechnologyResponse(
    val technologyId: Long,
    val key: String,
    val displayName: String
)

data class CreateTechRelationshipRequest(
    val toTechnologyId: Long,
    val relationType: RelationType,
    val weight: Int
)

data class TechRelationshipListResponse(
    val relationships: List<TechRelationshipResponse>
)
