package com.bebeis.skillweaver.api.plan

import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.api.common.auth.AuthUser
import com.bebeis.skillweaver.api.plan.dto.*
import com.bebeis.skillweaver.core.domain.learning.LearningPlanStatus
import com.bebeis.skillweaver.core.service.learning.LearningPlanService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/members/{memberId}/learning-plans")
class LearningPlanController(
    private val learningPlanService: LearningPlanService
) {

    @PostMapping
    fun createPlan(
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long,
        @Valid @RequestBody request: CreateLearningPlanRequest
    ): ResponseEntity<ApiResponse<LearningPlanResponse>> {
        require(authMemberId == memberId) { "본인의 학습 플랜만 생성할 수 있습니다" }
        val response = learningPlanService.createPlan(memberId, request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response))
    }

    @GetMapping
    fun getPlans(
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long,
        @RequestParam(required = false) status: LearningPlanStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<List<LearningPlanResponse>>> {
        require(authMemberId == memberId) { "본인의 학습 플랜만 조회할 수 있습니다" }
        val response = if (status != null) {
            learningPlanService.getPlansByStatus(memberId, status)
        } else {
            learningPlanService.getPlansByMemberId(memberId)
        }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{planId}")
    fun getPlan(
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long,
        @PathVariable planId: Long
    ): ResponseEntity<ApiResponse<LearningPlanResponse>> {
        require(authMemberId == memberId) { "본인의 학습 플랜만 조회할 수 있습니다" }
        val response = learningPlanService.getPlanById(memberId, planId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{planId}/status")
    fun updatePlanStatus(
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long,
        @PathVariable planId: Long,
        @Valid @RequestBody request: UpdatePlanStatusRequest
    ): ResponseEntity<ApiResponse<LearningPlanResponse>> {
        require(authMemberId == memberId) { "본인의 학습 플랜만 수정할 수 있습니다" }
        val response = learningPlanService.updatePlanStatus(memberId, planId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/{planId}/steps/{stepId}/complete")
    fun completeStep(
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long,
        @PathVariable planId: Long,
        @PathVariable stepId: Long
    ): ResponseEntity<ApiResponse<LearningPlanResponse>> {
        require(authMemberId == memberId) { "본인의 학습 플랜만 수정할 수 있습니다" }
        val response = learningPlanService.completeStep(memberId, planId, stepId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{planId}/progress")
    fun getProgress(
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long,
        @PathVariable planId: Long
    ): ResponseEntity<ApiResponse<PlanProgressResponse>> {
        require(authMemberId == memberId) { "본인의 학습 플랜만 조회할 수 있습니다" }
        val response = learningPlanService.getProgress(memberId, planId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/{planId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePlan(
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long,
        @PathVariable planId: Long
    ) {
        require(authMemberId == memberId) { "본인의 학습 플랜만 삭제할 수 있습니다" }
        learningPlanService.deletePlan(memberId, planId)
    }
}
