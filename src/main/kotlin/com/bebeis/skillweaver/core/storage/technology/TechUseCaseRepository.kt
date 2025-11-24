package com.bebeis.skillweaver.core.storage.technology

import com.bebeis.skillweaver.core.domain.technology.TechUseCase
import org.springframework.data.jpa.repository.JpaRepository

interface TechUseCaseRepository : JpaRepository<TechUseCase, Long> {
    fun findByKnowledgeId(knowledgeId: Long): List<TechUseCase>
}
