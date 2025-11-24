package com.bebeis.skillweaver.api.member

import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.api.member.dto.AddMemberSkillRequest
import com.bebeis.skillweaver.api.member.dto.MemberSkillResponse
import com.bebeis.skillweaver.api.member.dto.UpdateMemberSkillRequest
import com.bebeis.skillweaver.core.domain.member.skill.SkillLevel
import com.bebeis.skillweaver.core.domain.technology.TechnologyCategory
import com.bebeis.skillweaver.core.service.member.MemberSkillService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/members/{memberId}/skills")
class MemberSkillController(
    private val memberSkillService: MemberSkillService
) {

    @PostMapping
    fun addMemberSkill(
        @PathVariable memberId: Long,
        @Valid @RequestBody request: AddMemberSkillRequest
    ): ResponseEntity<ApiResponse<MemberSkillResponse>> {
        val response = memberSkillService.addMemberSkill(memberId, request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response))
    }

    @GetMapping
    fun getMemberSkills(
        @PathVariable memberId: Long,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) level: String?
    ): ResponseEntity<ApiResponse<List<MemberSkillResponse>>> {
        // "undefined" 문자열을 null로 처리
        val categoryEnum = category?.takeIf { it != "undefined" }?.let { 
            try { TechnologyCategory.valueOf(it) } catch (e: IllegalArgumentException) { null }
        }
        val levelEnum = level?.takeIf { it != "undefined" }?.let { 
            try { SkillLevel.valueOf(it) } catch (e: IllegalArgumentException) { null }
        }
        
        val response = memberSkillService.getMemberSkills(memberId, categoryEnum, levelEnum)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{memberSkillId}")
    fun getMemberSkill(
        @PathVariable memberId: Long,
        @PathVariable memberSkillId: Long
    ): ResponseEntity<ApiResponse<MemberSkillResponse>> {
        val response = memberSkillService.getMemberSkill(memberId, memberSkillId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{memberSkillId}")
    fun updateMemberSkill(
        @PathVariable memberId: Long,
        @PathVariable memberSkillId: Long,
        @Valid @RequestBody request: UpdateMemberSkillRequest
    ): ResponseEntity<ApiResponse<MemberSkillResponse>> {
        val response = memberSkillService.updateMemberSkill(memberId, memberSkillId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/{memberSkillId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMemberSkill(
        @PathVariable memberId: Long,
        @PathVariable memberSkillId: Long
    ) {
        memberSkillService.deleteMemberSkill(memberId, memberSkillId)
    }
}
