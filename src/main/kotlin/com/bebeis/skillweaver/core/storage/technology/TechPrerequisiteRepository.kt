package com.bebeis.skillweaver.core.storage.technology

import com.bebeis.skillweaver.core.domain.technology.TechPrerequisite
import org.springframework.data.jpa.repository.JpaRepository

interface TechPrerequisiteRepository : JpaRepository<TechPrerequisite, Long> {
    fun findByKnowledgeId(knowledgeId: Long): List<TechPrerequisite>
}
