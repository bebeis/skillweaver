package com.bebeis.skillweaver.core.storage.technology

import com.bebeis.skillweaver.core.domain.technology.RelationType
import com.bebeis.skillweaver.core.domain.technology.TechRelationship
import org.springframework.data.jpa.repository.JpaRepository

interface TechRelationshipRepository : JpaRepository<TechRelationship, Long> {
    fun findByFromId(technologyId: Long): List<TechRelationship>
    fun findByFromIdAndRelationType(technologyId: Long, relationType: RelationType): List<TechRelationship>
}
