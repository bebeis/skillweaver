package com.bebeis.skillweaver.api.technology

import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.api.technology.dto.CreateTechnologyRequest
import com.bebeis.skillweaver.api.technology.dto.TechnologyResponse
import com.bebeis.skillweaver.api.technology.dto.UpdateTechnologyRequest
import com.bebeis.skillweaver.core.domain.technology.TechnologyCategory
import com.bebeis.skillweaver.core.service.technology.TechnologyService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/technologies")
class TechnologyController(
    private val technologyService: TechnologyService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTechnology(
        @Valid @RequestBody request: CreateTechnologyRequest
    ): ApiResponse<TechnologyResponse> {
        val response = technologyService.createTechnology(request)
        return ApiResponse.success(response, "기술이 등록되었습니다")
    }

    @GetMapping("/{technologyId}")
    fun getTechnology(
        @PathVariable technologyId: Long
    ): ApiResponse<TechnologyResponse> {
        val response = technologyService.getTechnology(technologyId)
        return ApiResponse.success(response)
    }

    @GetMapping("/key/{key}")
    fun getTechnologyByKey(
        @PathVariable key: String
    ): ApiResponse<TechnologyResponse> {
        val response = technologyService.getTechnologyByKey(key)
        return ApiResponse.success(response)
    }

    @GetMapping
    fun getAllTechnologies(
        @RequestParam(required = false) category: TechnologyCategory?,
        @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean
    ): ApiResponse<List<TechnologyResponse>> {
        val response = when {
            category != null -> technologyService.getTechnologiesByCategory(category)
            activeOnly -> technologyService.getActiveTechnologies()
            else -> technologyService.getAllTechnologies()
        }
        return ApiResponse.success(response)
    }

    @PutMapping("/{technologyId}")
    fun updateTechnology(
        @PathVariable technologyId: Long,
        @Valid @RequestBody request: UpdateTechnologyRequest
    ): ApiResponse<TechnologyResponse> {
        val response = technologyService.updateTechnology(technologyId, request)
        return ApiResponse.success(response, "기술 정보가 수정되었습니다")
    }

    @DeleteMapping("/{technologyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTechnology(
        @PathVariable technologyId: Long
    ) {
        technologyService.deleteTechnology(technologyId)
    }
}
