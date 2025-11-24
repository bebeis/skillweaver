package com.bebeis.skillweaver.core.storage.technology

import com.bebeis.skillweaver.core.domain.technology.Technology
import com.bebeis.skillweaver.core.domain.technology.TechnologyCategory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TechnologyRepository : JpaRepository<Technology, Long> {
    fun findByKey(key: String): Technology?
    fun existsByKey(key: String): Boolean
    fun findByCategory(category: TechnologyCategory): List<Technology>
    fun findByActiveTrue(): List<Technology>
    fun findByKeyIn(keys: Collection<String>): List<Technology>

    @Query(
        """
        SELECT t FROM Technology t
        WHERE (:category IS NULL OR t.category = :category)
          AND (:ecosystem IS NULL OR t.ecosystem = :ecosystem)
          AND (:active IS NULL OR t.active = :active)
          AND (
                :search IS NULL OR
                LOWER(t.key) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(t.displayName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(COALESCE(t.ecosystem, '')) LIKE LOWER(CONCAT('%', :search, '%'))
          )
        """
    )
    fun findByFilters(
        category: TechnologyCategory?,
        ecosystem: String?,
        active: Boolean?,
        search: String?,
        pageable: Pageable
    ): Page<Technology>
}
