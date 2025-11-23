package com.bebeis.skillweaver.core.service.member

import com.bebeis.skillweaver.api.common.exception.*
import com.bebeis.skillweaver.api.common.security.JwtTokenProvider
import com.bebeis.skillweaver.api.member.dto.*
import com.bebeis.skillweaver.core.domain.member.Member
import com.bebeis.skillweaver.core.storage.member.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AuthService(
    private val memberRepository: MemberRepository,
    private val jwtTokenProvider: JwtTokenProvider
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    @Transactional
    fun signup(request: SignupRequest): SignupResponse {
        if (memberRepository.existsByEmail(request.email)) {
            logger.warn("Signup failed: email already exists - ${request.email}")
            conflict(ErrorCode.EMAIL_ALREADY_EXISTS.message)
        }

        val member = Member.create(
            name = request.name,
            email = request.email,
            rawPassword = request.password,
            targetTrack = request.targetTrack,
            experienceLevel = request.experienceLevel,
            learningPreference = request.learningPreference.toDomain()
        )

        val savedMember = memberRepository.save(member)
        logger.info("Member signed up successfully: ${savedMember.memberId}")

        return SignupResponse.from(savedMember)
    }

    fun login(request: LoginRequest): Pair<LoginResponse, String> {
        val member = memberRepository.findByEmail(request.email) ?: run {
            logger.warn("Login failed: member not found - ${request.email}")
            unauthorized(ErrorCode.INVALID_CREDENTIALS.message)
        }

        if (!member.matchesPassword(request.password)) {
            logger.warn("Login failed: invalid password - ${request.email}")
            unauthorized(ErrorCode.INVALID_CREDENTIALS.message)
        }

        val memberId = member.memberId!!
        val accessToken = jwtTokenProvider.generateAccessToken(memberId, member.email)
        val refreshToken = jwtTokenProvider.generateRefreshToken(memberId)

        logger.info("Member logged in successfully: $memberId")

        return LoginResponse(
            accessToken = accessToken,
            memberId = memberId,
            name = member.name,
            email = member.email
        ) to refreshToken
    }

    fun refreshToken(refreshToken: String): RefreshTokenResponse {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            logger.warn("Token refresh failed: invalid refresh token")
            unauthorized(ErrorCode.INVALID_REFRESH_TOKEN.message)
        }

        val memberId = jwtTokenProvider.getMemberIdFromToken(refreshToken)
        val member = memberRepository.findById(memberId).orElse(null) ?: run {
            logger.warn("Token refresh failed: member not found - $memberId")
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        val refreshedMemberId = member.memberId!!
        val newAccessToken = jwtTokenProvider.generateAccessToken(refreshedMemberId, member.email)

        logger.info("Access token refreshed for member: $refreshedMemberId")

        return RefreshTokenResponse(accessToken = newAccessToken)
    }
}
