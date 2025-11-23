package com.bebeis.skillweaver.api.common.exception

inline fun fail(message: String): Nothing = throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, message)

inline fun fail(errorCode: ErrorCode): Nothing = throw BusinessException(errorCode)

inline fun notFound(errorCode: ErrorCode): Nothing = throw ResourceNotFoundException(errorCode.message)

inline fun unauthorized(message: String): Nothing = throw AuthenticationException(message)

inline fun conflict(message: String): Nothing = throw DuplicateResourceException(message)

inline fun badRequest(message: String): Nothing = throw BusinessException(ErrorCode.INVALID_REQUEST, message)
