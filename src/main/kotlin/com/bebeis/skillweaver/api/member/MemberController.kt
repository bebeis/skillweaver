package com.bebeis.skillweaver.api.member

import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.api.common.auth.AuthUser
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
    fun getCurrentMember(@AuthUser memberId: Long): ResponseEntity<ApiResponse<MemberResponse>> {
        val response = memberService.getMember(memberId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{memberId}")
    fun getMember(
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long
    ): ResponseEntity<ApiResponse<MemberResponse>> {
        require(authMemberId == memberId) { "본인의 회원 정보만 조회할 수 있습니다" }
        val response = memberService.getMember(memberId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{memberId}")
    fun updateMember(
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long,
        @Valid @RequestBody request: UpdateMemberRequest
    ): ResponseEntity<ApiResponse<MemberResponse>> {
        require(authMemberId == memberId) { "본인의 회원 정보만 수정할 수 있습니다" }
        val response = memberService.updateMember(memberId, request)
        return ResponseEntity.ok(ApiResponse.success(response, "회원 정보가 수정되었습니다."))
    }

    @DeleteMapping("/{memberId}")
    fun deleteMember(
        @AuthUser authMemberId: Long,
        @PathVariable memberId: Long
    ): ResponseEntity<Void> {
        require(authMemberId == memberId) { "본인의 회원 정보만 삭제할 수 있습니다" }
        memberService.deleteMember(memberId)
        return ResponseEntity.noContent().build()
    }
}
