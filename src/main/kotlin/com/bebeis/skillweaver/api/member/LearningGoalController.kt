package com.bebeis.skillweaver.api.member

import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.api.common.auth.AuthUser
import com.bebeis.skillweaver.api.member.dto.CreateLearningGoalRequest
import com.bebeis.skillweaver.api.member.dto.LearningGoalListResponse
import com.bebeis.skillweaver.api.member.dto.LearningGoalResponse
import com.bebeis.skillweaver.api.member.dto.UpdateLearningGoalRequest
// Phase 4: Streak & Report DTOs
import com.bebeis.skillweaver.api.member.dto.StreakResponse
import com.bebeis.skillweaver.api.member.dto.WeeklyReportResponse
import com.bebeis.skillweaver.api.member.dto.MonthlyReportResponse
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
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long,
        @Valid @RequestBody request: CreateLearningGoalRequest
    ): ResponseEntity<ApiResponse<LearningGoalResponse>> {
        require(authMemberId == memberId) { "본인의 목표만 등록할 수 있습니다" }
        val response = learningGoalService.createGoal(memberId, request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response))
    }

    @GetMapping
    fun getGoals(
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long,
        @RequestParam(required = false) status: GoalStatus?,
        @RequestParam(required = false) priority: GoalPriority?
    ): ResponseEntity<ApiResponse<LearningGoalListResponse>> {
        require(authMemberId == memberId) { "본인의 목표만 조회할 수 있습니다" }
        val response = when {
            status != null -> learningGoalService.getGoalsByStatus(memberId, status)
            priority != null -> learningGoalService.getGoalsByPriority(memberId, priority)
            else -> learningGoalService.getGoalsByMemberId(memberId)
        }
        val wrapped = LearningGoalListResponse(
            goals = response,
            totalCount = response.size
        )
        return ResponseEntity.ok(ApiResponse.success(wrapped))
    }

    @GetMapping("/{goalId}")
    fun getGoal(
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long,
        @PathVariable goalId: Long
    ): ResponseEntity<ApiResponse<LearningGoalResponse>> {
        require(authMemberId == memberId) { "본인의 목표만 조회할 수 있습니다" }
        val response = learningGoalService.getGoalById(memberId, goalId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{goalId}")
    fun updateGoal(
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long,
        @PathVariable goalId: Long,
        @Valid @RequestBody request: UpdateLearningGoalRequest
    ): ResponseEntity<ApiResponse<LearningGoalResponse>> {
        require(authMemberId == memberId) { "본인의 목표만 수정할 수 있습니다" }
        val response = learningGoalService.updateGoal(memberId, goalId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/{goalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteGoal(
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long,
        @PathVariable goalId: Long
    ) {
        require(authMemberId == memberId) { "본인의 목표만 삭제할 수 있습니다" }
        learningGoalService.deleteGoal(memberId, goalId)
    }

    // =========================================================================
    // Phase 4: 스트릭 및 리포트 API
    // =========================================================================

    /**
     * 학습 스트릭 정보 조회
     * GET /api/v1/members/{memberId}/goals/{goalId}/streak
     */
    @GetMapping("/{goalId}/streak")
    fun getStreak(
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long,
        @PathVariable goalId: Long
    ): ResponseEntity<ApiResponse<StreakResponse>> {
        require(authMemberId == memberId) { "본인의 목표만 조회할 수 있습니다" }
        val response = learningGoalService.getStreakInfo(memberId, goalId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 주간 학습 리포트 조회
     * GET /api/v1/members/{memberId}/goals/{goalId}/reports/weekly
     */
    @GetMapping("/{goalId}/reports/weekly")
    fun getWeeklyReport(
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long,
        @PathVariable goalId: Long
    ): ResponseEntity<ApiResponse<WeeklyReportResponse>> {
        require(authMemberId == memberId) { "본인의 목표만 조회할 수 있습니다" }
        val response = learningGoalService.getWeeklyReport(memberId, goalId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 월간 학습 리포트 조회
     * GET /api/v1/members/{memberId}/goals/{goalId}/reports/monthly
     */
    @GetMapping("/{goalId}/reports/monthly")
    fun getMonthlyReport(
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long,
        @PathVariable goalId: Long
    ): ResponseEntity<ApiResponse<MonthlyReportResponse>> {
        require(authMemberId == memberId) { "본인의 목표만 조회할 수 있습니다" }
        val response = learningGoalService.getMonthlyReport(memberId, goalId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}

