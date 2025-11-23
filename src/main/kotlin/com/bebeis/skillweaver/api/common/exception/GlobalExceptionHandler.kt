package com.bebeis.skillweaver.api.common.exception

import com.bebeis.skillweaver.api.common.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class SkillWeaverExceptionHandler {

    private val logger = LoggerFactory.getLogger(SkillWeaverExceptionHandler::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Business exception occurred: ${ex.errorCode.code} - ${ex.message}")
        
        val status = when (ex.errorCode) {
            ErrorCode.INVALID_REQUEST, ErrorCode.UNPROCESSABLE_ENTITY -> HttpStatus.BAD_REQUEST
            ErrorCode.UNAUTHORIZED, ErrorCode.INVALID_TOKEN, ErrorCode.TOKEN_EXPIRED, 
            ErrorCode.INVALID_REFRESH_TOKEN, ErrorCode.INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED
            ErrorCode.FORBIDDEN -> HttpStatus.FORBIDDEN
            ErrorCode.NOT_FOUND, ErrorCode.MEMBER_NOT_FOUND -> HttpStatus.NOT_FOUND
            ErrorCode.CONFLICT, ErrorCode.EMAIL_ALREADY_EXISTS -> HttpStatus.CONFLICT
            ErrorCode.SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        val response = ApiResponse.error<Nothing>(
            errorCode = ex.errorCode.code,
            message = ex.message
        )

        return ResponseEntity.status(status).body(response)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Map<String, String>>> {
        logger.warn("Validation failed: ${ex.bindingResult}")

        val errors = ex.bindingResult.allErrors.associate { error ->
            val fieldName = (error as FieldError).field
            val errorMessage = error.defaultMessage ?: "Invalid value"
            fieldName to errorMessage
        }

        val response = ApiResponse.error<Map<String, String>>(
            errorCode = ErrorCode.UNPROCESSABLE_ENTITY.code,
            message = ErrorCode.UNPROCESSABLE_ENTITY.message
        ).copy(data = errors)

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Illegal argument: ${ex.message}")

        val response = ApiResponse.error<Nothing>(
            errorCode = ErrorCode.INVALID_REQUEST.code,
            message = ex.message ?: ErrorCode.INVALID_REQUEST.message
        )

        return ResponseEntity.badRequest().body(response)
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(ex: NoResourceFoundException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Resource not found: ${ex.resourcePath}")

        val response = ApiResponse.error<Nothing>(
            errorCode = ErrorCode.NOT_FOUND.code,
            message = ErrorCode.NOT_FOUND.message
        )

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Unexpected error occurred", ex)

        val response = ApiResponse.error<Nothing>(
            errorCode = ErrorCode.INTERNAL_SERVER_ERROR.code,
            message = ErrorCode.INTERNAL_SERVER_ERROR.message
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}
