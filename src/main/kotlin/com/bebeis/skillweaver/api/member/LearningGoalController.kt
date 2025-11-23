package com.bebeis.skillweaver.api.member

import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.api.member.dto.CreateLearningGoalRequest
import com.bebeis.skillweaver.api.member.dto.LearningGoalResponse
import com.bebeis.skillweaver.api.member.dto.UpdateLearningGoalRequest
import com.bebeis.skillweaver.core.domain.member.goal.GoalPriority
import com.bebeis.skillweaver.core.domain.member.goal.GoalStatus
import com.bebeis.skillweaver.core.service.member.LearningGoalService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/members/{memberId}/goals")
class LearningGoalController(
    private val learningGoalService: LearningGoalService
) {

    @PostMapping
    fun createGoal(
        @PathVariable memberId: Long,
        @Valid @RequestBody request: CreateLearningGoalRequest
    ): ResponseEntity<ApiResponse<LearningGoalResponse>> {
        val response = learningGoalService.createGoal(memberId, request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response))
    }

    @GetMapping
    fun getGoals(
        @PathVariable memberId: Long,
        @RequestParam(required = false) status: GoalStatus?,
        @RequestParam(required = false) priority: GoalPriority?
    ): ResponseEntity<ApiResponse<List<LearningGoalResponse>>> {
        val response = when {
            status != null -> learningGoalService.getGoalsByStatus(memberId, status)
            priority != null -> learningGoalService.getGoalsByPriority(memberId, priority)
            else -> learningGoalService.getGoalsByMemberId(memberId)
        }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{goalId}")
    fun getGoal(
        @PathVariable memberId: Long,
        @PathVariable goalId: Long
    ): ResponseEntity<ApiResponse<LearningGoalResponse>> {
        val response = learningGoalService.getGoalById(memberId, goalId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{goalId}")
    fun updateGoal(
        @PathVariable memberId: Long,
        @PathVariable goalId: Long,
        @Valid @RequestBody request: UpdateLearningGoalRequest
    ): ResponseEntity<ApiResponse<LearningGoalResponse>> {
        val response = learningGoalService.updateGoal(memberId, goalId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/{goalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteGoal(
        @PathVariable memberId: Long,
        @PathVariable goalId: Long
    ) {
        learningGoalService.deleteGoal(memberId, goalId)
    }
}
