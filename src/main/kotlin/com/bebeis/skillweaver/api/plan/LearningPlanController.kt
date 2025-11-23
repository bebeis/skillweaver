package com.bebeis.skillweaver.api.plan

import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.api.plan.dto.*
import com.bebeis.skillweaver.core.domain.learning.LearningPlanStatus
import com.bebeis.skillweaver.core.service.learning.LearningPlanService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/learning-plans")
class LearningPlanController(
    private val learningPlanService: LearningPlanService
) {

    @PostMapping
    fun createPlan(
        @RequestParam memberId: Long,
        @Valid @RequestBody request: CreateLearningPlanRequest
    ): ResponseEntity<ApiResponse<LearningPlanResponse>> {
        val response = learningPlanService.createPlan(memberId, request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response))
    }

    @GetMapping
    fun getPlans(
        @RequestParam memberId: Long,
        @RequestParam(required = false) status: LearningPlanStatus?
    ): ResponseEntity<ApiResponse<List<LearningPlanResponse>>> {
        val response = if (status != null) {
            learningPlanService.getPlansByStatus(memberId, status)
        } else {
            learningPlanService.getPlansByMemberId(memberId)
        }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{planId}")
    fun getPlan(
        @RequestParam memberId: Long,
        @PathVariable planId: Long
    ): ResponseEntity<ApiResponse<LearningPlanResponse>> {
        val response = learningPlanService.getPlanById(memberId, planId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{planId}/status")
    fun updatePlanStatus(
        @RequestParam memberId: Long,
        @PathVariable planId: Long,
        @Valid @RequestBody request: UpdatePlanStatusRequest
    ): ResponseEntity<ApiResponse<LearningPlanResponse>> {
        val response = learningPlanService.updatePlanStatus(memberId, planId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/{planId}/steps/{stepId}/complete")
    fun completeStep(
        @RequestParam memberId: Long,
        @PathVariable planId: Long,
        @PathVariable stepId: Long
    ): ResponseEntity<ApiResponse<LearningPlanResponse>> {
        val response = learningPlanService.completeStep(memberId, planId, stepId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{planId}/progress")
    fun getProgress(
        @RequestParam memberId: Long,
        @PathVariable planId: Long
    ): ResponseEntity<ApiResponse<PlanProgressResponse>> {
        val response = learningPlanService.getProgress(memberId, planId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/{planId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePlan(
        @RequestParam memberId: Long,
        @PathVariable planId: Long
    ) {
        learningPlanService.deletePlan(memberId, planId)
    }
}
