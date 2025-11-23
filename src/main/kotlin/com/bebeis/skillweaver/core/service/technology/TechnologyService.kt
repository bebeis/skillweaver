package com.bebeis.skillweaver.core.service.technology

import com.bebeis.skillweaver.api.common.exception.ErrorCode
import com.bebeis.skillweaver.api.common.exception.conflict
import com.bebeis.skillweaver.api.common.exception.notFound
import com.bebeis.skillweaver.api.technology.dto.CreateTechnologyRequest
import com.bebeis.skillweaver.api.technology.dto.TechnologyResponse
import com.bebeis.skillweaver.api.technology.dto.UpdateTechnologyRequest
import com.bebeis.skillweaver.core.domain.technology.Technology
import com.bebeis.skillweaver.core.domain.technology.TechnologyCategory
import com.bebeis.skillweaver.core.storage.technology.TechnologyRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TechnologyService(
    private val technologyRepository: TechnologyRepository
) {
    private val logger = LoggerFactory.getLogger(TechnologyService::class.java)

    @Transactional
    fun createTechnology(request: CreateTechnologyRequest): TechnologyResponse {
        if (technologyRepository.existsByKey(request.key)) {
            logger.warn("Technology creation failed: key already exists - ${request.key}")
            conflict(ErrorCode.TECHNOLOGY_KEY_ALREADY_EXISTS.message)
        }

        val technology = Technology(
            key = request.key,
            displayName = request.displayName,
            category = request.category,
            ecosystem = request.ecosystem,
            officialSite = request.officialSite
        )

        val savedTechnology = technologyRepository.save(technology)
        logger.info("Technology created successfully: ${savedTechnology.technologyId}")

        return TechnologyResponse.from(savedTechnology)
    }

    fun getTechnology(technologyId: Long): TechnologyResponse {
        val technology = technologyRepository.findById(technologyId).orElse(null) ?: run {
            logger.warn("Technology not found: $technologyId")
            notFound(ErrorCode.TECHNOLOGY_NOT_FOUND)
        }

        return TechnologyResponse.from(technology)
    }

    fun getTechnologyByKey(key: String): TechnologyResponse {
        val technology = technologyRepository.findByKey(key) ?: run {
            logger.warn("Technology not found by key: $key")
            notFound(ErrorCode.TECHNOLOGY_NOT_FOUND)
        }

        return TechnologyResponse.from(technology)
    }

    fun getAllTechnologies(): List<TechnologyResponse> {
        return technologyRepository.findAll()
            .map { TechnologyResponse.from(it) }
    }

    fun getTechnologiesByCategory(category: TechnologyCategory): List<TechnologyResponse> {
        return technologyRepository.findByCategory(category)
            .map { TechnologyResponse.from(it) }
    }

    fun getActiveTechnologies(): List<TechnologyResponse> {
        return technologyRepository.findByActiveTrue()
            .map { TechnologyResponse.from(it) }
    }

    @Transactional
    fun updateTechnology(technologyId: Long, request: UpdateTechnologyRequest): TechnologyResponse {
        val technology = technologyRepository.findById(technologyId).orElse(null) ?: run {
            logger.warn("Technology not found for update: $technologyId")
            notFound(ErrorCode.TECHNOLOGY_NOT_FOUND)
        }

        val updatedTechnology = Technology(
            technologyId = technology.technologyId,
            key = technology.key,
            displayName = request.displayName ?: technology.displayName,
            category = technology.category,
            ecosystem = request.ecosystem ?: technology.ecosystem,
            officialSite = request.officialSite ?: technology.officialSite,
            active = request.active ?: technology.active
        )

        val savedTechnology = technologyRepository.save(updatedTechnology)
        logger.info("Technology updated successfully: $technologyId")

        return TechnologyResponse.from(savedTechnology)
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
}
