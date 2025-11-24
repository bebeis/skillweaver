package com.bebeis.skillweaver.api.common.dto

data class PaginationResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
