package com.bebeis.skillweaver.api.common.exception

/**
 * 비즈니스 예외 기본 클래스
 */
open class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message
) : RuntimeException(message)

/**
 * 리소스를 찾을 수 없을 때
 */
class ResourceNotFoundException(
    message: String = "요청한 리소스를 찾을 수 없습니다"
) : BusinessException(ErrorCode.NOT_FOUND, message)

/**
 * 중복된 리소스가 있을 때
 */
class DuplicateResourceException(
    message: String = "이미 존재하는 리소스입니다"
) : BusinessException(ErrorCode.CONFLICT, message)

/**
 * 인증 실패
 */
class AuthenticationException(
    message: String = "인증에 실패했습니다"
) : BusinessException(ErrorCode.UNAUTHORIZED, message)

/**
 * 권한 없음
 */
class AuthorizationException(
    message: String = "접근 권한이 없습니다"
) : BusinessException(ErrorCode.FORBIDDEN, message)

/**
 * 유효성 검증 실패
 */
class ValidationException(
    message: String = "입력값이 올바르지 않습니다"
) : BusinessException(ErrorCode.INVALID_REQUEST, message)
