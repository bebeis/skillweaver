package com.bebeis.skillweaver.api.auth

import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.api.common.security.JwtProperties
import com.bebeis.skillweaver.api.member.dto.LoginRequest
import com.bebeis.skillweaver.api.member.dto.LoginResponse
import com.bebeis.skillweaver.api.member.dto.RefreshTokenResponse
import com.bebeis.skillweaver.api.member.dto.SignupRequest
import com.bebeis.skillweaver.api.member.dto.SignupResponse
import com.bebeis.skillweaver.core.service.member.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseCookie
import org.springframework.web.bind.annotation.*
import java.time.Duration

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtProperties: JwtProperties
) {
    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/signup/email")
    fun signup(@Valid @RequestBody request: SignupRequest): ResponseEntity<ApiResponse<SignupResponse>> {
        val response = authService.signup(request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "회원가입이 완료되었습니다."))
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpResponse: HttpServletResponse
    ): ResponseEntity<ApiResponse<LoginResponse>> {
        val (loginResponse, refreshToken) = authService.login(request)

        val refreshCookie = ResponseCookie.from(jwtProperties.refreshCookieName, refreshToken)
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ofMillis(jwtProperties.refreshTokenValidity).seconds)
            .build()
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString())

        return ResponseEntity.ok(ApiResponse.success(loginResponse, "로그인 성공"))
    }

    @PostMapping("/refresh")
    fun refresh(
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<RefreshTokenResponse>> {
        val refreshToken = httpRequest.cookies
            ?.find { it.name == jwtProperties.refreshCookieName }
            ?.value
            ?: run {
                logger.warn("Refresh token cookie not found")
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("INVALID_TOKEN", "리프레시 토큰이 없습니다"))
            }

        val response = authService.refreshToken(refreshToken)
        return ResponseEntity.ok(ApiResponse.success(response, "토큰이 재발급되었습니다."))
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    fun logout(httpResponse: HttpServletResponse): ResponseEntity<Void> {
        val deleteCookie = ResponseCookie.from(jwtProperties.refreshCookieName, "")
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(0)
            .build()
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString())

        logger.info("User logged out successfully")
        return ResponseEntity.noContent().build()
    }
}
