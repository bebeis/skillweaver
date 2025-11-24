package com.bebeis.skillweaver.core.storage.technology

import com.bebeis.skillweaver.core.domain.technology.TechnologyKnowledge
import org.springframework.data.jpa.repository.JpaRepository

interface TechnologyKnowledgeRepository : JpaRepository<TechnologyKnowledge, Long> {
    fun findByTechnologyId(technologyId: Long): TechnologyKnowledge?
}
