package com.kamesh.easyconnectionsdk.domain.model

/**
 * Custom exceptions for handling network issues
 */
sealed class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /**
     * Error when server returns error response
     */
    class ServerException(val code: Int, val body: String?) :
        NetworkException("Server error with status code: $code", null)

    /**
     * Error when network connection fails
     */
    class ConnectionException(cause: Throwable) :
        NetworkException("Network connection failed", cause)

    /**
     * Error when request times out
     */
    class TimeoutException(cause: Throwable) :
        NetworkException("Request timed out", cause)

    /**
     * Error when parsing response fails
     */
    class ParseException(cause: Throwable) :
        NetworkException("Failed to parse response", cause)

    /**
     * Error when authentication fails
     */
    class AuthenticationException(message: String = "Authentication failed") :
        NetworkException(message)

    /**
     * Error when encryption/decryption fails
     */
    class EncryptionException(cause: Throwable) :
        NetworkException("Encryption error", cause)
}