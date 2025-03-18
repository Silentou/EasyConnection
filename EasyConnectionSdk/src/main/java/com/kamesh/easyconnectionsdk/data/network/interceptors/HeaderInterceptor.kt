/**
 * HeaderInterceptor.kt
 */
package com.kamesh.easyconnectionsdk.data.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor to add headers to every request
 */
class HeaderInterceptor(private val headers: Map<String, String>) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Add all headers to the request
        val requestBuilder = original.newBuilder()
        headers.forEach { (name, value) ->
            requestBuilder.header(name, value)
        }

        return chain.proceed(requestBuilder.build())
    }
}