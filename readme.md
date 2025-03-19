# Easy Connection SDK

A lightweight, feature-rich Android networking library that simplifies API integrations with built-in support for request/response encryption, robust error handling, automatic retries, caching, and more.

[![Maven Central](https://img.shields.io/maven-central/v/com.kamesh.easyconnectionsdk/easyconnectionsdk.svg)](https://search.maven.org/artifact/com.kamesh.easyconnectionsdk/easyconnectionsdk)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## Features

- 🔒 **Request/Response Encryption** - Optional AES-256 encryption for sensitive data
- 🔄 **Auto-Retry Logic** - Configurable retry mechanism for failed network requests
- 💾 **Response Caching** - Built-in cache support with improved control (maxAge, maxStale)
- 🛡️ **Comprehensive Error Handling** - Type-safe API response wrapper with detailed error states
- 📄 **Pagination Support** - Helper objects for paginated API responses
- 🔑 **Authentication Support** - Built-in header and token-based authentication
- 📋 **Flexible Headers** - Easily add custom headers to requests
- 🔍 **Detailed Logging** - Optional request/response logging for debugging
- 💻 **Modern Architecture** - Built with Kotlin, Coroutines, and Retrofit
- 🔨 **Fluent Builder API** - Easy, readable configuration using builder pattern

## Installation

### Gradle

Add the dependency to your app's `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("com.kamesh.easyconnectionsdk:easyconnectionsdk:1.0.0")
}
```

Or if using the older Groovy syntax:

```groovy
dependencies {
    implementation 'com.kamesh.easyconnectionsdk:easyconnectionsdk:1.0.0'
}
```

## Quick Start

### Initialize the SDK

Initialize the SDK in your Application class:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize with application context for caching
        EasyConnectionClient.init(this)
        
        // Configure the client using the new Builder pattern
        EasyConnectionClient.initialize(
            "https://api.example.com/",
            block = {
                withLogging(BuildConfig.DEBUG)
                withRetry(2)
                withTimeout(30)
                withCache(60)
            }
        )
    }
}
```

### Define Your API Service

Define your API service interface using Retrofit annotations:

```kotlin
interface MyApiService {
    @GET("users")
    suspend fun getUsers(): Response<List<User>>
    
    @POST("users")
    suspend fun createUser(@Body user: User): Response<User>
}
```

### Create the Service and Make Requests

Create your API service and make requests:

```kotlin
// Create API service
val apiService = EasyConnectionClient.createService(MyApiService::class.java)

// In your ViewModel or Repository
viewModelScope.launch {
    // Use the safe API call wrapper
    val result = EasyConnectionClient.safeApiCall { apiService.getUsers() }
    
    // Handle the result with functional API
    result.onSuccess { users ->
        // Handle successful response
        userLiveData.value = users
    }.onFailure { code, message ->
        // Handle API error (4xx, 5xx)
        errorLiveData.value = "Error $code: $message"
    }.onError { exception ->
        // Handle network or other exceptions
        errorLiveData.value = "Network error: ${exception.message}"
    }
}
```

## Detailed Usage

### Configuration with Builder Pattern

The SDK now offers a fluent Builder pattern for more readable configuration:

```kotlin
EasyConnectionClient.initialize(
    "https://api.example.com/",
    block = {
        // Security options
        withEncryption(
            key = "YourSecretKey123456789012345678901234", 
            salt = "SaltValue", 
            testMode = false
        )
        withAuthentication(
            token = "Bearer your-auth-token",
            apiKey = "your-api-key"
        )
        
        // Performance options
        withTimeout(30)
        withRetry(2)
        withCache(
            durationSeconds = 60,
            forceCache = false,
            size = 10 * 1024 * 1024 // 10 MB
        )
        
        // Debug options
        withLogging(BuildConfig.DEBUG)
        
        // Misc options
        withHeaders(mapOf(
            "Device-Id" to deviceId,
            "App-Version" to BuildConfig.VERSION_NAME
        ))
        
        withSSL(
            enabled = true,
            certificatePins = listOf("sha256/...")
        )
    }
)
```

You can also use the traditional approach with a Configuration object:

```kotlin
val config = EasyConnectionClient.Builder("https://api.example.com/")
    .withLogging(true)
    .withRetry(3)
    .build()

EasyConnectionClient.initialize(config)
```

### Custom Dispatcher for Testing

The SDK now supports setting a custom dispatcher for coroutines, which is useful for testing:

```kotlin
// In your tests
val testDispatcher = TestCoroutineDispatcher()
EasyConnectionClient.setDispatcher(testDispatcher)
```

### Working with Responses

The SDK provides a typesafe `ApiResponse` wrapper for handling different response states:

```kotlin
when (result) {
    is ApiResponse.Success -> {
        // Handle successful response
        val data = result.data
    }
    is ApiResponse.Failure -> {
        // Handle API error (e.g., 404, 500)
        val errorCode = result.code
        val errorMessage = result.message
    }
    is ApiResponse.Error -> {
        // Handle exceptions (e.g., network issues)
        val exception = result.exception
    }
}
```

Or use the functional approach:

```kotlin
result
    .onSuccess { data -> 
        // Handle success
    }
    .onFailure { code, message -> 
        // Handle API errors
    }
    .onError { exception -> 
        // Handle exceptions
    }
```

### Pagination Support

For paginated APIs, use the `PagedResponse` helper:

```kotlin
// API service method
@GET("posts")
suspend fun getPosts(
    @Query("page") page: Int,
    @Query("limit") limit: Int
): Response<List<Post>>

// In your repository
suspend fun fetchPosts(page: Int, limit: Int): ApiResponse<PagedResponse<Post>> {
    return EasyConnectionClient.safeApiCall { 
        // Call API
        val response = apiService.getPosts(page, limit)
        
        // Convert to PagedResponse
        val posts = response.body() ?: listOf()
        val totalItems = response.headers()["X-Total-Count"]?.toInt() ?: 0
        val pagedResponse = PagedResponse.create(
            items = posts,
            page = page,
            pageSize = limit,
            totalItems = totalItems
        )
        
        // Return transformed response
        Response.success(pagedResponse)
    }
}
```

### Encryption Support

For APIs that support encrypted payloads:

```kotlin
// Enable encryption in your configuration
EasyConnectionClient.initialize(
    "https://api.secure-example.com/",
    block = {
        withEncryption(
            key = "YourSecretKey123456789012345678901234",
            salt = "SaltValue"
        )
    }
)
```

For APIs that don't support encryption, you can use test mode:

```kotlin
EasyConnectionClient.initialize(
    "https://api.example.com/",
    block = {
        withEncryption(
            key = "YourSecretKey123456789012345678901234",
            salt = "SaltValue",
            testMode = true  // Add headers but don't actually encrypt
        )
    }
)
```

### Updating Configuration

You can update the configuration at runtime:

```kotlin
// Update auth token after login
EasyConnectionClient.updateAuthToken("Bearer new-token")

// Update encryption key
EasyConnectionClient.updateEncryptionKey("NewEncryptionKey")

// Custom update
EasyConnectionClient.updateConfiguration { currentConfig ->
    currentConfig.copy(
        timeoutSeconds = 60,
        retryCount = 3
    )
}
```

### Network Exception Handling

The SDK provides specific exception types for better error handling:

```kotlin
when (exception) {
    is NetworkException.ConnectionException -> {
        // Handle connection issues
    }
    is NetworkException.TimeoutException -> {
        // Handle timeouts
    }
    is NetworkException.ServerException -> {
        // Handle server errors
    }
    is NetworkException.AuthenticationException -> {
        // Handle auth failures
    }
    is NetworkException.ParseException -> {
        // Handle parse errors
    }
}
```

## Requirements

- Android API level 24 or higher
- Kotlin 1.5.0 or higher

## Dependencies

This library depends on:

- Retrofit 2.9.0
- OkHttp 4.10.0
- Kotlin Coroutines
- AndroidX Lifecycle components

## License

```
Copyright 2025 Kamesh

Licensed under the MIT License (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   https://opensource.org/licenses/MIT

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Contact

Kamesh - [@kamesh_twitter](https://twitter.com/kamesh_twitter) - kameshrajanitha@gmail.com

Project Link: [https://github.com/Silentou/EasyConnection](https://github.com/Silentou/EasyConnection)
