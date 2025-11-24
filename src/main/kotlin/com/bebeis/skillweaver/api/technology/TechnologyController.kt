package com.bebeis.skillweaver.api.technology

import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.api.technology.dto.CreateTechnologyRequest
import com.bebeis.skillweaver.api.technology.dto.TechnologyDetailResponse
import com.bebeis.skillweaver.api.technology.dto.TechnologyListResponse
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
    ): ApiResponse<TechnologyDetailResponse> {
        val response = technologyService.createTechnology(request)
        return ApiResponse.success(response, "기술이 등록되었습니다")
    }

    @GetMapping("/{technologyId}")
    fun getTechnology(
        @PathVariable technologyId: Long
    ): ApiResponse<TechnologyDetailResponse> {
        val response = technologyService.getTechnology(technologyId)
        return ApiResponse.success(response)
    }

    @GetMapping("/key/{key}")
    fun getTechnologyByKey(
        @PathVariable key: String
    ): ApiResponse<TechnologyDetailResponse> {
        val response = technologyService.getTechnologyByKey(key)
        return ApiResponse.success(response)
    }

    @GetMapping
    fun getAllTechnologies(
        @RequestParam(required = false) category: TechnologyCategory?,
        @RequestParam(required = false) ecosystem: String?,
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<TechnologyListResponse> {
        val response = technologyService.getTechnologies(category, ecosystem, active, search, page, size)
        return ApiResponse.success(response)
    }

    @PutMapping("/{technologyId}")
    fun updateTechnology(
        @PathVariable technologyId: Long,
        @Valid @RequestBody request: UpdateTechnologyRequest
    ): ApiResponse<TechnologyDetailResponse> {
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
