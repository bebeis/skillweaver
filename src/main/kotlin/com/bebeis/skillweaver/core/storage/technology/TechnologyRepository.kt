package com.bebeis.skillweaver.core.storage.technology

import com.bebeis.skillweaver.core.domain.technology.Technology
import com.bebeis.skillweaver.core.domain.technology.TechnologyCategory
import org.springframework.data.jpa.repository.JpaRepository

interface TechnologyRepository : JpaRepository<Technology, Long> {
    fun findByKey(key: String): Technology?
    fun existsByKey(key: String): Boolean
    fun findByCategory(category: TechnologyCategory): List<Technology>
    fun findByActiveTrue(): List<Technology>
}
