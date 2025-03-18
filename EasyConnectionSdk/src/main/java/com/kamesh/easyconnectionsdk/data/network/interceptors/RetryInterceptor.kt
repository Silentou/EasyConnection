/**
 * RetryInterceptor.kt
 */
package com.kamesh.easyconnectionsdk.data.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor to automatically retry failed requests
 */
class RetryInterceptor(private val maxRetries: Int) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var exception: IOException? = null

        var retriesRemaining = maxRetries

        while (retriesRemaining > 0) {
            try {
                // In case of a previous failure and retry
                if (response != null) {
                    response.close()
                }

                // Attempt the request
                response = chain.proceed(request)

                // If successful and in the 2xx range, return the response
                if (response.isSuccessful) {
                    return response
                }

                // If server error (5xx), retry
                if (response.code >= 500) {
                    response.close()
                    retriesRemaining--

                    // Add exponential backoff here if desired
                    Thread.sleep(calculateBackoffTime(maxRetries - retriesRemaining))
                    continue
                }

                // For other status codes, don't retry
                return response

            } catch (e: IOException) {
                // Save the last exception
                exception = e

                // Retry on network errors
                retriesRemaining--

                // Add exponential backoff
                if (retriesRemaining > 0) {
                    Thread.sleep(calculateBackoffTime(maxRetries - retriesRemaining))
                }
            }
        }

        // If we got here, we failed after all retries
        response?.let { return it }

        // If we have an exception but no response, throw the exception
        throw exception ?: IOException("Unexpected error during request execution")
    }

    /**
     * Calculate backoff time with exponential increase
     */
    private fun calculateBackoffTime(retryCount: Int): Long {
        // Exponential backoff: 100ms, 200ms, 400ms, 800ms, etc.
        val baseBackoffMs = 100L
        return baseBackoffMs * (1L shl retryCount)
    }
}