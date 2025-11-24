package com.bebeis.skillweaver.api.technology

import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.api.technology.dto.CreateTechRelationshipRequest
import com.bebeis.skillweaver.api.technology.dto.TechRelationshipListResponse
import com.bebeis.skillweaver.api.technology.dto.TechRelationshipResponse
import com.bebeis.skillweaver.core.domain.technology.RelationType
import com.bebeis.skillweaver.core.service.technology.TechnologyService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/technologies/{technologyId}/relationships")
class TechnologyRelationshipController(
    private val technologyService: TechnologyService
) {

    @GetMapping
    fun getRelationships(
        @PathVariable technologyId: Long,
        @RequestParam(required = false) relationType: RelationType?
    ): ApiResponse<TechRelationshipListResponse> {
        val relationships = technologyService.getRelationships(technologyId, relationType)
        return ApiResponse.success(
            TechRelationshipListResponse(relationships = relationships)
        )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addRelationship(
        @PathVariable technologyId: Long,
        @Valid @RequestBody request: CreateTechRelationshipRequest
    ): ApiResponse<TechRelationshipResponse> {
        val response = technologyService.createRelationship(technologyId, request)
        return ApiResponse.success(response, "기술 관계가 추가되었습니다.")
    }
}
