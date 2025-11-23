package com.bebeis.skillweaver.api.technology.dto

import com.bebeis.skillweaver.core.domain.technology.Technology
import com.bebeis.skillweaver.core.domain.technology.TechnologyCategory
import java.time.LocalDateTime

data class TechnologyResponse(
    val technologyId: Long,
    val key: String,
    val displayName: String,
    val category: TechnologyCategory,
    val ecosystem: String?,
    val officialSite: String?,
    val active: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(technology: Technology): TechnologyResponse {
            return TechnologyResponse(
                technologyId = technology.technologyId!!,
                key = technology.key,
                displayName = technology.displayName,
                category = technology.category,
                ecosystem = technology.ecosystem,
                officialSite = technology.officialSite,
                active = technology.active,
                createdAt = technology.createdAt,
                updatedAt = technology.updatedAt
            )
        }
    }
}

data class CreateTechnologyRequest(
    val key: String,
    val displayName: String,
    val category: TechnologyCategory,
    val ecosystem: String? = null,
    val officialSite: String? = null
)

data class UpdateTechnologyRequest(
    val displayName: String? = null,
    val ecosystem: String? = null,
    val officialSite: String? = null,
    val active: Boolean? = null
)
