package com.bebeis.skillweaver.core.service.technology

import com.bebeis.skillweaver.api.common.exception.ErrorCode
import com.bebeis.skillweaver.api.common.exception.conflict
import com.bebeis.skillweaver.api.common.exception.notFound
import com.bebeis.skillweaver.api.common.dto.PaginationResponse
import com.bebeis.skillweaver.api.technology.dto.*
import com.bebeis.skillweaver.core.domain.technology.*
import com.bebeis.skillweaver.core.domain.technology.Technology
import com.bebeis.skillweaver.core.storage.technology.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TechnologyService(
    private val technologyRepository: TechnologyRepository,
    private val technologyKnowledgeRepository: TechnologyKnowledgeRepository,
    private val techPrerequisiteRepository: TechPrerequisiteRepository,
    private val techUseCaseRepository: TechUseCaseRepository,
    private val techRelationshipRepository: TechRelationshipRepository
) {
    private val logger = LoggerFactory.getLogger(TechnologyService::class.java)

    @Transactional
    fun createTechnology(request: CreateTechnologyRequest): TechnologyDetailResponse {
        if (technologyRepository.existsByKey(request.key)) {
            logger.warn("Technology creation failed: key already exists - ${request.key}")
            conflict(ErrorCode.TECHNOLOGY_KEY_ALREADY_EXISTS.message)
        }

        val technology = Technology(
            key = request.key,
            displayName = request.displayName,
            category = request.category,
            ecosystem = request.ecosystem,
            officialSite = request.officialSite,
            learningRoadmap = request.learningRoadmap,
            estimatedLearningHours = request.estimatedLearningHours,
            prerequisites = request.prerequisites,
            relatedTechnologies = request.relatedTechnologies,
            communityPopularity = request.communityPopularity,
            jobMarketDemand = request.jobMarketDemand
        )

        val savedTechnology = technologyRepository.save(technology)
        logger.info("Technology created successfully: ${savedTechnology.technologyId}")

        return buildDetailResponse(savedTechnology)
    }

    fun getTechnology(technologyId: Long): TechnologyDetailResponse {
        val technology = findTechnology(technologyId)
        return buildDetailResponse(technology)
    }

    fun getTechnologyByKey(key: String): TechnologyDetailResponse {
        val technology = technologyRepository.findByKey(key) ?: run {
            logger.warn("Technology not found by key: $key")
            notFound(ErrorCode.TECHNOLOGY_NOT_FOUND)
        }
        return buildDetailResponse(technology)
    }

    fun getTechnologies(
        category: TechnologyCategory?,
        ecosystem: String?,
        active: Boolean?,
        search: String?,
        page: Int,
        size: Int
    ): TechnologyListResponse {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceAtLeast(1))
        val result = technologyRepository.findByFilters(category, ecosystem, active, search, pageable)
        return TechnologyListResponse(
            technologies = result.content.map { TechnologySummaryResponse.from(it) },
            pagination = PaginationResponse(
                page = result.number,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages
            )
        )
    }

    @Transactional
    fun updateTechnology(technologyId: Long, request: UpdateTechnologyRequest): TechnologyDetailResponse {
        val technology = findTechnology(technologyId)

        val updatedTechnology = Technology(
            technologyId = technology.technologyId,
            key = technology.key,
            displayName = request.displayName ?: technology.displayName,
            category = technology.category,
            ecosystem = request.ecosystem ?: technology.ecosystem,
            officialSite = request.officialSite ?: technology.officialSite,
            active = request.active ?: technology.active,
            learningRoadmap = request.learningRoadmap ?: technology.learningRoadmap,
            estimatedLearningHours = request.estimatedLearningHours ?: technology.estimatedLearningHours,
            prerequisites = request.prerequisites ?: technology.prerequisites,
            relatedTechnologies = request.relatedTechnologies ?: technology.relatedTechnologies,
            communityPopularity = request.communityPopularity ?: technology.communityPopularity,
            jobMarketDemand = request.jobMarketDemand ?: technology.jobMarketDemand
        )

        val savedTechnology = technologyRepository.save(updatedTechnology)
        logger.info("Technology updated successfully: $technologyId")

        return buildDetailResponse(savedTechnology)
    }

    @Transactional
    fun deleteTechnology(technologyId: Long) {
        if (!technologyRepository.existsById(technologyId)) {
            logger.warn("Technology not found for deletion: $technologyId")
            notFound(ErrorCode.TECHNOLOGY_NOT_FOUND)
        }

        technologyRepository.deleteById(technologyId)
        logger.info("Technology deleted successfully: $technologyId")
    }

    fun getTechnologyKnowledge(technologyId: Long): TechnologyKnowledgeResponse {
        val technology = findTechnology(technologyId)
        val techId = ensureTechnologyId(technology)
        val knowledge = getOrCreateKnowledge(techId)
        return toKnowledgeResponse(knowledge)
    }

    @Transactional
    fun upsertTechnologyKnowledge(
        technologyId: Long,
        request: UpsertTechnologyKnowledgeRequest
    ): TechnologyKnowledgeMutationResponse {
        val technology = findTechnology(technologyId)
        val techId = ensureTechnologyId(technology)
        val existing = technologyKnowledgeRepository.findByTechnologyId(techId)

        val saved = if (existing == null) {
            TechnologyKnowledge(
                technologyId = techId,
                summary = request.summary,
                learningTips = request.learningTips,
                sourceType = request.sourceType
            ).let { technologyKnowledgeRepository.save(it) }
        } else {
            val updated = TechnologyKnowledge(
                technologyKnowledgeId = existing.technologyKnowledgeId,
                technologyId = existing.technologyId,
                summary = request.summary,
                learningTips = request.learningTips,
                sourceType = request.sourceType
            )
            technologyKnowledgeRepository.save(updated)
        }

        logger.info("Technology knowledge upserted for technology: $technologyId")
        return TechnologyKnowledgeMutationResponse(
            technologyKnowledgeId = saved.technologyKnowledgeId!!,
            createdAt = saved.createdAt,
            updatedAt = saved.updatedAt
        )
    }

    @Transactional
    fun addPrerequisite(
        technologyId: Long,
        request: CreateTechPrerequisiteRequest
    ): TechPrerequisiteResponse {
        val technology = findTechnology(technologyId)
        val techId = ensureTechnologyId(technology)
        val knowledge = getOrCreateKnowledge(techId)

        val saved = techPrerequisiteRepository.save(
            TechPrerequisite(
                knowledgeId = knowledge.technologyKnowledgeId!!,
                prerequisiteKey = request.prerequisiteKey
            )
        )

        val displayName = technologyRepository.findByKey(request.prerequisiteKey)?.displayName
        logger.info("Prerequisite added. technologyId=$technologyId, prerequisite=${request.prerequisiteKey}")
        return TechPrerequisiteResponse(
            prerequisiteKey = saved.prerequisiteKey,
            displayName = displayName
        )
    }

    @Transactional
    fun addUseCase(
        technologyId: Long,
        request: CreateTechUseCaseRequest
    ): TechnologyUseCaseResponse {
        val technology = findTechnology(technologyId)
        val techId = ensureTechnologyId(technology)
        val knowledge = getOrCreateKnowledge(techId)

        val saved = techUseCaseRepository.save(
            TechUseCase(
                knowledgeId = knowledge.technologyKnowledgeId!!,
                useCase = request.useCase
            )
        )

        logger.info("Use case added. technologyId=$technologyId")
        return TechnologyUseCaseResponse(
            useCaseId = saved.techUseCaseId!!,
            knowledgeId = saved.knowledgeId,
            useCase = saved.useCase
        )
    }

    fun getRelationships(
        technologyId: Long,
        relationType: RelationType?
    ): List<TechRelationshipResponse> {
        findTechnology(technologyId)
        val relationships = if (relationType != null) {
            techRelationshipRepository.findByFromIdAndRelationType(technologyId, relationType)
        } else {
            techRelationshipRepository.findByFromId(technologyId)
        }

        if (relationships.isEmpty()) {
            return emptyList()
        }

        val technologyIds = relationships.flatMap { listOf(it.fromId, it.toId) }.toSet()
        val technologyMap = technologyRepository.findAllById(technologyIds.toList()).associateBy { it.technologyId!! }

        return relationships.mapNotNull { relationship ->
            val from = technologyMap[relationship.fromId]
            val to = technologyMap[relationship.toId]

            if (from == null || to == null) {
                null
            } else {
                TechRelationshipResponse(
                    techRelationshipId = relationship.techRelationshipId!!,
                    fromTechnology = RelatedTechnologyResponse(
                        technologyId = from.technologyId!!,
                        key = from.key,
                        displayName = from.displayName
                    ),
                    toTechnology = RelatedTechnologyResponse(
                        technologyId = to.technologyId!!,
                        key = to.key,
                        displayName = to.displayName
                    ),
                    relationType = relationship.relationType,
                    weight = relationship.weight
                )
            }
        }
    }

    @Transactional
    fun createRelationship(
        technologyId: Long,
        request: CreateTechRelationshipRequest
    ): TechRelationshipResponse {
        if (technologyId == request.toTechnologyId) {
            conflict("동일한 기술 간에는 관계를 생성할 수 없습니다.")
        }

        val fromTechnology = findTechnology(technologyId)
        val toTechnology = findTechnology(request.toTechnologyId)

        val existing = techRelationshipRepository.findByFromId(technologyId)
            .any { it.toId == request.toTechnologyId && it.relationType == request.relationType }
        if (existing) {
            conflict("이미 동일한 관계가 존재합니다.")
        }

        val relationship = TechRelationship(
            fromId = fromTechnology.technologyId!!,
            toId = toTechnology.technologyId!!,
            relationType = request.relationType,
            weight = request.weight
        )

        val saved = techRelationshipRepository.save(relationship)
        logger.info("Technology relationship created from ${fromTechnology.key} to ${toTechnology.key}")

        return TechRelationshipResponse(
            techRelationshipId = saved.techRelationshipId!!,
            fromTechnology = RelatedTechnologyResponse(
                technologyId = fromTechnology.technologyId!!,
                key = fromTechnology.key,
                displayName = fromTechnology.displayName
            ),
            toTechnology = RelatedTechnologyResponse(
                technologyId = toTechnology.technologyId!!,
                key = toTechnology.key,
                displayName = toTechnology.displayName
            ),
            relationType = saved.relationType,
            weight = saved.weight
        )
    }

    private fun buildDetailResponse(technology: Technology): TechnologyDetailResponse {
        val bundle = buildKnowledgeBundle(technology.technologyId!!)
        return TechnologyDetailResponse(
            technologyId = technology.technologyId!!,
            key = technology.key,
            displayName = technology.displayName,
            category = technology.category,
            ecosystem = technology.ecosystem,
            officialSite = technology.officialSite,
            active = technology.active,
            knowledge = bundle.knowledge,
            prerequisites = bundle.prerequisites,
            useCases = bundle.useCases,
            learningRoadmap = technology.learningRoadmap,
            estimatedLearningHours = technology.estimatedLearningHours,
            relatedTechnologies = technology.relatedTechnologies,
            communityPopularity = technology.communityPopularity,
            jobMarketDemand = technology.jobMarketDemand
        )
    }

    private fun buildKnowledgeBundle(technologyId: Long): KnowledgeBundle {
        val knowledge = technologyKnowledgeRepository.findByTechnologyId(technologyId) ?: return KnowledgeBundle(
            knowledge = null,
            prerequisites = emptyList(),
            useCases = emptyList()
        )

        val prerequisites = techPrerequisiteRepository.findByKnowledgeId(knowledge.technologyKnowledgeId!!)
        val useCases = techUseCaseRepository.findByKnowledgeId(knowledge.technologyKnowledgeId!!)
        val prerequisiteKeys = prerequisites.map { it.prerequisiteKey }
        val displayNames = resolveDisplayNames(prerequisiteKeys)

        return KnowledgeBundle(
            knowledge = toKnowledgeResponse(knowledge),
            prerequisites = prerequisites.map {
                TechPrerequisiteResponse(
                    prerequisiteKey = it.prerequisiteKey,
                    displayName = displayNames[it.prerequisiteKey]
                )
            },
            useCases = useCases.map { it.useCase }
        )
    }

    private fun resolveDisplayNames(keys: List<String>): Map<String, String> {
        if (keys.isEmpty()) {
            return emptyMap()
        }
        return technologyRepository.findByKeyIn(keys)
            .associate { it.key to it.displayName }
    }

    private fun toKnowledgeResponse(entity: TechnologyKnowledge): TechnologyKnowledgeResponse {
        return TechnologyKnowledgeResponse(
            technologyKnowledgeId = entity.technologyKnowledgeId!!,
            technologyId = entity.technologyId,
            summary = entity.summary,
            learningTips = entity.learningTips,
            sourceType = entity.sourceType,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    private fun getOrCreateKnowledge(technologyId: Long): TechnologyKnowledge {
        return technologyKnowledgeRepository.findByTechnologyId(technologyId)
            ?: technologyKnowledgeRepository.save(TechnologyKnowledge(technologyId = technologyId))
    }

    private fun findTechnology(technologyId: Long): Technology {
        return technologyRepository.findById(technologyId).orElse(null) ?: run {
            logger.warn("Technology not found: $technologyId")
            notFound(ErrorCode.TECHNOLOGY_NOT_FOUND)
        }
    }

    private data class KnowledgeBundle(
        val knowledge: TechnologyKnowledgeResponse?,
        val prerequisites: List<TechPrerequisiteResponse>,
        val useCases: List<String>
    )

    private fun ensureTechnologyId(technology: Technology): Long {
        return technology.technologyId ?: notFound(ErrorCode.TECHNOLOGY_NOT_FOUND)
    }
}
