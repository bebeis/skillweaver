package com.bebeis.skillweaver.api.common.exception

enum class ErrorCode(
    val code: String,
    val message: String
) {
    INVALID_REQUEST("INVALID_REQUEST", "잘못된 요청입니다"),
    UNPROCESSABLE_ENTITY("UNPROCESSABLE_ENTITY", "입력값 유효성 검증에 실패했습니다"),
    
    UNAUTHORIZED("UNAUTHORIZED", "인증에 실패했습니다"),
    INVALID_TOKEN("INVALID_TOKEN", "유효하지 않은 토큰입니다"),
    INVALID_REFRESH_TOKEN("INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "토큰이 만료되었습니다"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다"),
    
    FORBIDDEN("FORBIDDEN", "접근 권한이 없습니다"),
    
    NOT_FOUND("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다"),
    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다"),
    
    CONFLICT("CONFLICT", "중복된 데이터가 존재합니다"),
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다"),
    
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다"),
    
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "서비스를 일시적으로 사용할 수 없습니다")
}
