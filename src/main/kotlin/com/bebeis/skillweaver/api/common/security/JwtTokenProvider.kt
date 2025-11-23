package com.bebeis.skillweaver.api.common.security

import com.bebeis.skillweaver.api.common.exception.AuthenticationException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties
) {
    private val logger = LoggerFactory.getLogger(JwtTokenProvider::class.java)
    
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())

    fun generateAccessToken(memberId: Long, email: String): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtProperties.accessTokenValidity)

        return Jwts.builder()
            .subject(memberId.toString())
            .claim("email", email)
            .claim("type", "access")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    fun generateRefreshToken(memberId: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtProperties.refreshTokenValidity)

        return Jwts.builder()
            .subject(memberId.toString())
            .claim("type", "refresh")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    fun getMemberIdFromToken(token: String): Long {
        val claims = parseToken(token)
        return claims.subject.toLong()
    }

    fun getEmailFromToken(token: String): String? {
        val claims = parseToken(token)
        return claims["email"] as? String
    }

    fun validateToken(token: String): Boolean {
        try {
            parseToken(token)
            return true
        } catch (ex: Exception) {
            logger.warn("Invalid JWT token: ${ex.message}")
            return false
        }
    }

    private fun parseToken(token: String): Claims {
        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (ex: Exception) {
            logger.error("Failed to parse JWT token", ex)
            throw AuthenticationException("유효하지 않은 토큰입니다")
        }
    }
}
