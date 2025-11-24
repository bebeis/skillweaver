package com.bebeis.skillweaver.api.common.security

import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.api.common.auth.AuthUserArgumentResolver
import com.bebeis.skillweaver.api.common.exception.ErrorCode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AuthenticationInterceptor(
    private val jwtTokenProvider: JwtTokenProvider,
    private val objectMapper: ObjectMapper
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (request.method.equals("OPTIONS", ignoreCase = true)) {
            return true
        }

        val path = request.requestURI
        if (path.startsWith("/api/v1/auth")) {
            return true
        }

        val tokenExtraction = extractToken(request)
            ?: return unauthorized(response, "인증 토큰이 없습니다")

        val (tokenSource, rawToken) = tokenExtraction

        if (tokenSource == TokenSource.AUTHORIZATION_HEADER && rawToken.isBlank()) {
            return unauthorized(response, "올바르지 않은 토큰 형식입니다")
        }

        val token = if (tokenSource == TokenSource.AUTHORIZATION_HEADER) {
            if (!rawToken.startsWith("Bearer ")) {
                return unauthorized(response, "올바르지 않은 토큰 형식입니다")
            }
            rawToken.substring(7)
        } else {
            rawToken
        }

        if (!jwtTokenProvider.validateToken(token)) {
            return unauthorized(response, "유효하지 않은 토큰입니다")
        }

        val memberId = jwtTokenProvider.getMemberIdFromToken(token)
        request.setAttribute(AuthUserArgumentResolver.AUTH_MEMBER_ID_ATTRIBUTE, memberId)
        return true
    }

    private fun extractToken(request: HttpServletRequest): Pair<TokenSource, String>? {
        val authHeader = request.getHeader("Authorization")
        if (!authHeader.isNullOrBlank()) {
            return TokenSource.AUTHORIZATION_HEADER to authHeader
        }

        val queryToken = request.getParameter("access_token")
            ?: request.getParameter("accessToken")
            ?: request.getParameter("token")

        return queryToken?.takeIf { it.isNotBlank() }?.let {
            TokenSource.QUERY_PARAMETER to it
        }
    }

    private fun unauthorized(response: HttpServletResponse, message: String): Boolean {
        if (!response.isCommitted) {
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = "UTF-8"
            val body = ApiResponse.error<Nothing>(
                errorCode = ErrorCode.UNAUTHORIZED.code,
                message = message
            )
            response.writer.write(objectMapper.writeValueAsString(body))
            response.writer.flush()
        }
        return false
    }

    private enum class TokenSource {
        AUTHORIZATION_HEADER,
        QUERY_PARAMETER
    }
}
