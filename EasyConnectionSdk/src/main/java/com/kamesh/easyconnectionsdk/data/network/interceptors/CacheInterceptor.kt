/**
 * CacheInterceptor.kt
 */
package com.kamesh.easyconnectionsdk.data.network.interceptors

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Interceptor to handle caching of responses
 */
class CacheInterceptor(
    private val cacheDurationSeconds: Int = 60,
    private val forceCache: Boolean = false
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Skip cache for non-GET requests
        if (request.method != "GET") {
            return chain.proceed(request)
        }

        // Apply cache control based on settings
        val cacheControl = if (forceCache) {
            // Force network response to be cached
            CacheControl.Builder()
                .maxAge(cacheDurationSeconds, TimeUnit.SECONDS)
                .build()
        } else {
            // Use normal cache behavior
            CacheControl.Builder()
                .maxAge(cacheDurationSeconds, TimeUnit.SECONDS)
                .build()
        }

        // Add cache headers to request
        val requestWithCache = request.newBuilder()
            .cacheControl(cacheControl)
            .build()

        val response = chain.proceed(requestWithCache)

        // Force response caching if needed
        return if (forceCache) {
            response.newBuilder()
                .header("Cache-Control", "public, max-age=$cacheDurationSeconds")
                .removeHeader("Pragma")
                .build()
        } else {
            response
        }
    }
}