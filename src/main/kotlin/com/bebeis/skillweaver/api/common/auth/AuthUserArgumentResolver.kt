package com.bebeis.skillweaver.api.common.auth

import com.bebeis.skillweaver.api.common.exception.AuthenticationException
import com.bebeis.skillweaver.api.common.security.JwtTokenProvider
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AuthUserArgumentResolver(
    private val jwtTokenProvider: JwtTokenProvider
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthUser::class.java) && 
               parameter.parameterType == Long::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Long {
        val authHeader = webRequest.getHeader("Authorization")
            ?: throw AuthenticationException("인증 토큰이 없습니다")

        if (!authHeader.startsWith("Bearer ")) {
            throw AuthenticationException("올바르지 않은 토큰 형식입니다")
        }

        val token = authHeader.substring(7)
        
        if (!jwtTokenProvider.validateToken(token)) {
            throw AuthenticationException("유효하지 않은 토큰입니다")
        }

        return jwtTokenProvider.getMemberIdFromToken(token)
    }
}
