/**
 * ApiResponse.kt
 */
package com.kamesh.easyconnectionsdk.domain.model

/**
 * Wrapper class for API responses that handles success, failure and error states
 */
sealed class ApiResponse<out T> {
    /**
     * Successful API response with data
     */
    data class Success<T>(val data: T) : ApiResponse<T>()

    /**
     * API request failed with HTTP error
     */
    data class Failure(val code: Int, val message: String) : ApiResponse<Nothing>()

    /**
     * Exception occurred during API request
     */
    data class Error(val exception: Throwable) : ApiResponse<Nothing>()

    /**
     * Check if the response was successful
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Get data if successful, otherwise null
     */
    fun getOrNull(): T? = if (this is Success) data else null

    /**
     * Transform successful data
     */
    fun <R> map(transform: (T) -> R): ApiResponse<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Failure -> this
            is Error -> this
        }
    }

    /**
     * Handle different states of response
     */
    inline fun onSuccess(action: (T) -> Unit): ApiResponse<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Handle failure state
     */
    inline fun onFailure(action: (code: Int, message: String) -> Unit): ApiResponse<T> {
        if (this is Failure) action(code, message)
        return this
    }

    /**
     * Handle error state
     */
    inline fun onError(action: (Throwable) -> Unit): ApiResponse<T> {
        if (this is Error) action(exception)
        return this
    }

    /**
     * Execute block for all non-success states
     */
    inline fun onFailureOrError(action: () -> Unit): ApiResponse<T> {
        if (this !is Success) action()
        return this
    }

    /**
     * Get data or throw exception
     */
    fun getOrThrow(): T {
        return when (this) {
            is Success -> data
            is Failure -> throw RuntimeException("API Error: $code - $message")
            is Error -> throw exception
        }
    }

    /**
     * Get data or default value
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): T {
        return if (this is Success) data else defaultValue
    }
}