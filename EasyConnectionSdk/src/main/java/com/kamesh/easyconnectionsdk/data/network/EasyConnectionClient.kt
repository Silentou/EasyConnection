/**
 * EasyConnectionClient.kt
 * Main entry point for the SDK
 */
package com.kamesh.easyconnectionsdk.data.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kamesh.easyconnectionsdk.data.network.interceptors.AuthInterceptor
import com.kamesh.easyconnectionsdk.data.network.interceptors.CacheInterceptor
import com.kamesh.easyconnectionsdk.data.network.interceptors.EncryptionInterceptor
import com.kamesh.easyconnectionsdk.data.network.interceptors.HeaderInterceptor
import com.kamesh.easyconnectionsdk.data.network.interceptors.RetryInterceptor
import com.kamesh.easyconnectionsdk.domain.model.ApiResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Main entry point for Easy Connection SDK.
 * Provides a configurable HTTP client for making API requests with features
 * like encryption, caching, retry logic, and more.
 */
object EasyConnectionClient {

    private var retrofit: Retrofit? = null
    private var configuration: Configuration? = null
    private var appContext: Context? = null
    private var dispatcher: CoroutineDispatcher = Dispatchers.IO

    // Constants
    private const val ERROR_NOT_INITIALIZED = "EasyConnectionClient not initialized. Call initialize() first."
    private const val DEFAULT_TIMEOUT_SECONDS = 30L
    private const val DEFAULT_CACHE_SIZE = 10L * 1024 * 1024 // 10 MB

    /**
     * Configuration class for EasyConnectionClient
     */
    data class Configuration(
        val baseUrl: String,
        val encryptionKey: String? = null,
        val encryptionSalt: String? = null,
        val encryptionTestMode: Boolean = false,
        val authToken: String? = null,
        val apiKey: String? = null,
        val timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
        val enableLogging: Boolean = false,
        val additionalHeaders: Map<String, String> = emptyMap(),
        val retryCount: Int = 0,
        val useSSL: Boolean = true,
        val certificatePinning: List<String> = emptyList(),
        val cacheDurationSeconds: Int = 0,
        val forceCacheEnabled: Boolean = false,
        val cacheSize: Long = DEFAULT_CACHE_SIZE
    )

    /**
     * Builder class for creating Configuration objects with a fluent API
     */
    class Builder(private val baseUrl: String) {
        private var encryptionKey: String? = null
        private var encryptionSalt: String? = null
        private var encryptionTestMode: Boolean = false
        private var authToken: String? = null
        private var apiKey: String? = null
        private var timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS
        private var enableLogging: Boolean = false
        private var additionalHeaders: Map<String, String> = emptyMap()
        private var retryCount: Int = 0
        private var useSSL: Boolean = true
        private var certificatePinning: List<String> = emptyList()
        private var cacheDurationSeconds: Int = 0
        private var forceCacheEnabled: Boolean = false
        private var cacheSize: Long = DEFAULT_CACHE_SIZE

        fun withEncryption(key: String, salt: String? = null, testMode: Boolean = false) = apply {
            this.encryptionKey = key
            this.encryptionSalt = salt
            this.encryptionTestMode = testMode
        }

        fun withAuthentication(token: String? = null, apiKey: String? = null) = apply {
            this.authToken = token
            this.apiKey = apiKey
        }

        fun withTimeout(seconds: Long) = apply {
            this.timeoutSeconds = seconds
        }

        fun withLogging(enabled: Boolean) = apply {
            this.enableLogging = enabled
        }

        fun withHeaders(headers: Map<String, String>) = apply {
            this.additionalHeaders = headers
        }

        fun withRetry(count: Int) = apply {
            this.retryCount = count
        }

        fun withSSL(enabled: Boolean, certificatePins: List<String> = emptyList()) = apply {
            this.useSSL = enabled
            this.certificatePinning = certificatePins
        }

        fun withCache(durationSeconds: Int, forceCache: Boolean = false, size: Long = DEFAULT_CACHE_SIZE) = apply {
            this.cacheDurationSeconds = durationSeconds
            this.forceCacheEnabled = forceCache
            this.cacheSize = size
        }

        fun build() = Configuration(
            baseUrl = baseUrl,
            encryptionKey = encryptionKey,
            encryptionSalt = encryptionSalt,
            encryptionTestMode = encryptionTestMode,
            authToken = authToken,
            apiKey = apiKey,
            timeoutSeconds = timeoutSeconds,
            enableLogging = enableLogging,
            additionalHeaders = additionalHeaders,
            retryCount = retryCount,
            useSSL = useSSL,
            certificatePinning = certificatePinning,
            cacheDurationSeconds = cacheDurationSeconds,
            forceCacheEnabled = forceCacheEnabled,
            cacheSize = cacheSize
        )
    }

    /**
     * Initialize the SDK with application context for cache support
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Set a custom dispatcher for coroutines (useful for testing)
     */
    fun setDispatcher(dispatcher: CoroutineDispatcher) {
        this.dispatcher = dispatcher
    }

    /**
     * Initialize the client with a Configuration object
     */
    fun initialize(configuration: Configuration) {
        this.configuration = configuration

        // Build OkHttpClient
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(configuration.timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(configuration.timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(configuration.timeoutSeconds, TimeUnit.SECONDS)

        // Add logging if enabled
        if (configuration.enableLogging) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            clientBuilder.addInterceptor(loggingInterceptor)
        }

        // Add headers interceptor
        val headers = configuration.additionalHeaders.toMutableMap()

        // Add API key to headers if provided
        configuration.apiKey?.let {
            headers["X-Api-Key"] = it
        }

        if (headers.isNotEmpty()) {
            clientBuilder.addInterceptor(HeaderInterceptor(headers))
        }

        // Add auth interceptor if token is provided
        configuration.authToken?.let {
            clientBuilder.addInterceptor(AuthInterceptor(it))
        }

        // Add encryption interceptor if key is provided
        configuration.encryptionKey?.let {
            val salt = configuration.encryptionSalt ?: ""
            clientBuilder.addInterceptor(
                EncryptionInterceptor(
                    encryptionKey = it,
                    salt = salt,
                    testMode = configuration.encryptionTestMode
                )
            )
        }

        // Add retry interceptor if retry count > 0
        if (configuration.retryCount > 0) {
            clientBuilder.addInterceptor(RetryInterceptor(configuration.retryCount))
        }

        // Add cache support if cacheDurationSeconds > 0
        if (configuration.cacheDurationSeconds > 0) {
            appContext?.let { context ->
                // Set up cache
                val cacheDir = File(context.cacheDir, "http-cache")
                val cache = Cache(cacheDir, configuration.cacheSize)
                clientBuilder.cache(cache)

                // Add cache interceptor
                clientBuilder.addInterceptor(
                    CacheInterceptor(
                        cacheDurationSeconds = configuration.cacheDurationSeconds,
                        forceCache = configuration.forceCacheEnabled
                    )
                )
            }
        }

        // Certificate pinning could be added here
        // if (configuration.certificatePinning.isNotEmpty()) { ... }

        // Create Gson instance with custom type adapters if needed
        val gson: Gson = GsonBuilder()
            .setLenient()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()

        // Build Retrofit instance
        retrofit = Retrofit.Builder()
            .baseUrl(configuration.baseUrl)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * Simplified initialize method that uses the Builder pattern
     */
    fun initialize(baseUrl: String, block: Builder.() -> Unit = {}) {
        val builder = Builder(baseUrl).apply(block)
        initialize(builder.build())
    }

    /**
     * Create an implementation of the API endpoints defined by the service interface.
     *
     * @param service The service class containing API endpoint definitions
     * @return The implementation of the service interface
     * @throws IllegalStateException if initialize() wasn't called before this method
     */
    fun <T> createService(service: Class<T>): T {
        return retrofit?.create(service) ?: throw IllegalStateException(ERROR_NOT_INITIALIZED)
    }

    /**
     * Helper function to safely execute API calls and handle errors
     */
    suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<T>,
        customDispatcher: CoroutineDispatcher = dispatcher
    ): ApiResponse<T> {
        return withContext(customDispatcher) {
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        ApiResponse.Success(body)
                    } else {
                        ApiResponse.Failure(
                            response.code(),
                            "Response body is null"
                        )
                    }
                } else {
                    ApiResponse.Failure(
                        response.code(),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: HttpException) {
                ApiResponse.Failure(e.code(), e.message ?: "Http Exception")
            } catch (e: IOException) {
                ApiResponse.Error(e)
            } catch (e: Exception) {
                ApiResponse.Error(e)
            }
        }
    }

    /**
     * Update configuration after initialization
     */
    fun updateConfiguration(update: (Configuration) -> Configuration) {
        val currentConfig = configuration ?: throw IllegalStateException(ERROR_NOT_INITIALIZED)
        initialize(update(currentConfig))
    }

    /**
     * Update auth token
     */
    fun updateAuthToken(token: String?) {
        updateConfiguration { it.copy(authToken = token) }
    }

    /**
     * Update encryption key
     */
    fun updateEncryptionKey(key: String?) {
        updateConfiguration { it.copy(encryptionKey = key) }
    }

    /**
     * Get current configuration
     */
    fun getConfiguration(): Configuration {
        return configuration ?: throw IllegalStateException(ERROR_NOT_INITIALIZED)
    }
}