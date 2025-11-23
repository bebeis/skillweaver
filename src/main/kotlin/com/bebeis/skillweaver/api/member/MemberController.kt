package com.bebeis.skillweaver.api.member

import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.api.member.dto.MemberResponse
import com.bebeis.skillweaver.api.member.dto.UpdateMemberRequest
import com.bebeis.skillweaver.core.service.member.MemberService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val memberService: MemberService
) {

    @GetMapping("/me")
    fun getCurrentMember(@RequestHeader("X-Member-Id", required = false) memberId: Long?): ResponseEntity<ApiResponse<MemberResponse>> {
        requireNotNull(memberId) { "회원 ID가 필요합니다" }
        val response = memberService.getMember(memberId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{memberId}")
    fun getMember(@PathVariable memberId: Long): ResponseEntity<ApiResponse<MemberResponse>> {
        val response = memberService.getMember(memberId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{memberId}")
    fun updateMember(
        @PathVariable memberId: Long,
        @Valid @RequestBody request: UpdateMemberRequest
    ): ResponseEntity<ApiResponse<MemberResponse>> {
        val response = memberService.updateMember(memberId, request)
        return ResponseEntity.ok(ApiResponse.success(response, "회원 정보가 수정되었습니다."))
    }

    @DeleteMapping("/{memberId}")
    fun deleteMember(@PathVariable memberId: Long): ResponseEntity<Void> {
        memberService.deleteMember(memberId)
        return ResponseEntity.noContent().build()
    }
}
