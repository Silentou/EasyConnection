/**
 * AuthInterceptor.kt
 */
package com.kamesh.easyconnectionsdk.data.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor to handle authentication by adding auth token to requests
 */
class AuthInterceptor(private val authToken: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Add auth header to the request
        val requestWithAuth = original.newBuilder()
            .header("Authorization", "Bearer $authToken")
            .build()

        return chain.proceed(requestWithAuth)
    }
}
