/**
 * EasyConnectionClient.kt
 * Main entry point for the SDK
 */
package com.kamesh.easyconnectionsdk.data.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kamesh.easyconnectionsdk.domain.model.ApiResponse
import com.kamesh.easyconnectionsdk.data.network.interceptors.AuthInterceptor
import com.kamesh.easyconnectionsdk.data.network.interceptors.CacheInterceptor
import com.kamesh.easyconnectionsdk.data.network.interceptors.EncryptionInterceptor
import com.kamesh.easyconnectionsdk.data.network.interceptors.HeaderInterceptor
import com.kamesh.easyconnectionsdk.data.network.interceptors.RetryInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import android.content.Context
import okhttp3.Cache

object EasyConnectionClient {

    private var retrofit: Retrofit? = null
    private var configuration: Configuration? = null
    private var appContext: Context? = null

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
        val timeoutSeconds: Long = 30,
        val enableLogging: Boolean = false,
        val additionalHeaders: Map<String, String> = emptyMap(),
        val retryCount: Int = 0,
        val useSSL: Boolean = true,
        val certificatePinning: List<String> = emptyList(),
        val cacheDurationSeconds: Int = 0,
        val forceCacheEnabled: Boolean = false,
        val cacheSize: Long = 10 * 1024 * 1024 // 10 MB default cache size
    )

    /**
     * Initialize the SDK with application context for cache support
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Initialize the Retrofit client with customizable configuration
     *
     * @param baseUrl The base URL for all API calls (Required)
     * @param encryptionKey The key used for request/response encryption (Optional)
     * @param encryptionSalt Salt value used for encryption (Optional)
     * @param authToken Authentication token to add to requests (Optional)
     * @param apiKey API key for service authentication (Optional)
     * @param timeoutSeconds Connection and read timeout in seconds (Default: 30)
     * @param enableLogging Whether to enable HTTP request/response logging (Default: false)
     * @param additionalHeaders Additional headers to add to each request (Optional)
     * @param retryCount Number of times to retry failed requests (Default: 0)
     * @param useSSL Whether to use SSL for connections (Default: true)
     * @param certificatePinning List of certificate hashes for certificate pinning (Optional)
     * @param cacheDurationSeconds Duration to cache responses in seconds (Default: 0, no cache)
     * @param forceCacheEnabled Whether to force response caching (Default: false)
     */
    fun initialize(
        baseUrl: String,
        encryptionKey: String? = null,
        encryptionSalt: String? = null,
        encryptionTestMode: Boolean = false,
        authToken: String? = null,
        apiKey: String? = null,
        timeoutSeconds: Long = 30,
        enableLogging: Boolean = false,
        additionalHeaders: Map<String, String> = emptyMap(),
        retryCount: Int = 0,
        useSSL: Boolean = true,
        certificatePinning: List<String> = emptyList(),
        cacheDurationSeconds: Int = 0,
        forceCacheEnabled: Boolean = false
    ) {
        val config = Configuration(
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
            forceCacheEnabled = forceCacheEnabled
        )

        initialize(config)
    }

    /**
     * Initialize with Configuration object
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

        // Update the encryption interceptor creation in your initialize(configuration) method
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
     * Create an implementation of the API endpoints defined by the service interface.
     *
     * @param service The service class containing API endpoint definitions
     * @return The implementation of the service interface
     * @throws IllegalStateException if initialize() wasn't called before this method
     */
    fun <T> createService(service: Class<T>): T {
        return retrofit?.create(service) ?: throw IllegalStateException(
            "EasyConnectionClient not initialized. Call initialize() first."
        )
    }

    /**
     * Helper function to safely execute API calls and handle errors
     */
    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): ApiResponse<T> {
        return withContext(Dispatchers.IO) {
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
        val currentConfig = configuration ?: throw IllegalStateException(
            "EasyConnectionClient not initialized. Call initialize() first."
        )
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
        return configuration ?: throw IllegalStateException(
            "EasyConnectionClient not initialized. Call initialize() first."
        )
    }
}