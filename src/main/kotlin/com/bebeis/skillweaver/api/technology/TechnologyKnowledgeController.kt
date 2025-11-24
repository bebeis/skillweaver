package com.bebeis.skillweaver.api.technology

import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.api.technology.dto.*
import com.bebeis.skillweaver.core.service.technology.TechnologyService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/technologies/{technologyId}/knowledge")
class TechnologyKnowledgeController(
    private val technologyService: TechnologyService
) {

    @GetMapping
    fun getKnowledge(
        @PathVariable technologyId: Long
    ): ApiResponse<TechnologyKnowledgeResponse> {
        val response = technologyService.getTechnologyKnowledge(technologyId)
        return ApiResponse.success(response)
    }

    @PutMapping
    fun upsertKnowledge(
        @PathVariable technologyId: Long,
        @Valid @RequestBody request: UpsertTechnologyKnowledgeRequest
    ): ApiResponse<TechnologyKnowledgeMutationResponse> {
        val response = technologyService.upsertTechnologyKnowledge(technologyId, request)
        return ApiResponse.success(response, "기술 지식이 수정되었습니다.")
    }

    @PostMapping("/prerequisites")
    @ResponseStatus(HttpStatus.CREATED)
    fun addPrerequisite(
        @PathVariable technologyId: Long,
        @Valid @RequestBody request: CreateTechPrerequisiteRequest
    ): ApiResponse<TechPrerequisiteResponse> {
        val response = technologyService.addPrerequisite(technologyId, request)
        return ApiResponse.success(response, "선행 지식이 추가되었습니다.")
    }

    @PostMapping("/use-cases")
    @ResponseStatus(HttpStatus.CREATED)
    fun addUseCase(
        @PathVariable technologyId: Long,
        @Valid @RequestBody request: CreateTechUseCaseRequest
    ): ApiResponse<TechnologyUseCaseResponse> {
        val response = technologyService.addUseCase(technologyId, request)
        return ApiResponse.success(response, "사용 사례가 추가되었습니다.")
    }
}
