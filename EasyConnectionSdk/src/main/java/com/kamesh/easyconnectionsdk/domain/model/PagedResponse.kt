/**
 * Paged response wrapper
 */
package com.kamesh.easyconnectionsdk.domain.model

/**
 * Generic class to handle paginated responses
 */
data class PagedResponse<T>(
    val data: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalItems: Int,
    val hasNextPage: Boolean = false,
    val hasPreviousPage: Boolean = false
) {
    companion object {
        /**
         * Create a paged response from a list and pagination info
         */
        fun <T> create(
            items: List<T>,
            page: Int,
            pageSize: Int,
            totalItems: Int
        ): PagedResponse<T> {
            val totalPages = if (totalItems % pageSize == 0) {
                totalItems / pageSize
            } else {
                (totalItems / pageSize) + 1
            }

            return PagedResponse(
                data = items,
                page = page,
                pageSize = pageSize,
                totalPages = totalPages,
                totalItems = totalItems,
                hasNextPage = page < totalPages,
                hasPreviousPage = page > 1
            )
        }
    }
}