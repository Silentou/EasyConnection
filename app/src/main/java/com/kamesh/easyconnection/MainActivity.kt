package com.kamesh.easyconnection

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.kamesh.easyconnectionsdk.domain.model.ApiResponse
import com.kamesh.easyconnectionsdk.domain.model.NetworkException
import com.kamesh.easyconnectionsdk.domain.model.PagedResponse
import com.kamesh.easyconnectionsdk.data.network.EasyConnectionClient
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

class MainActivity : ComponentActivity() {

    // Define the API service interface with all methods we want to test
    interface JsonPlaceholderApi {
        // Basic CRUD operations
        @GET("posts")
        suspend fun getPosts(): Response<List<Post>>

        @POST("posts")
        suspend fun createPost(@Body post: Post): Response<Post>

        @PUT("posts/{id}")
        suspend fun updatePost(@Path("id") id: Int, @Body post: Post): Response<Post>

        @DELETE("posts/{id}")
        suspend fun deletePost(@Path("id") id: Int): Response<Unit>

        // Additional methods to test pagination
        @GET("posts")
        suspend fun getPagedPosts(
            @Query("_page") page: Int,
            @Query("_limit") limit: Int
        ): Response<List<Post>>

        // Testing safe API call wrapper
        @GET("posts/{id}")
        suspend fun getPost(@Path("id") id: Int): Response<Post>

        // Testing error handling (404)
        @GET("posts/999999")
        suspend fun getNonExistentPost(): Response<Post>

        // Testing invalid endpoint (to test network exception)
        @GET("invalid/endpoint")
        suspend fun getInvalidEndpoint(): Response<Post>
    }

    // Data class for the response
    data class Post(
        val id: Int,
        val userId: Int,
        val title: String,
        val body: String
    )

    private lateinit var resultTextView: TextView
    private lateinit var apiService: JsonPlaceholderApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultTextView = findViewById(R.id.resultTextView)

        // Find all test buttons
        val testGetButton = findViewById<Button>(R.id.getButton)
        val testPostButton = findViewById<Button>(R.id.postButton)
        val testPutButton = findViewById<Button>(R.id.putButton)
        val testDeleteButton = findViewById<Button>(R.id.deleteButton)

        // Add additional test buttons to layout for additional features
        val testPaginationButton = findViewById<Button>(R.id.paginationButton)
        val testSafeApiCallButton = findViewById<Button>(R.id.safeApiCallButton)
        val testErrorHandlingButton = findViewById<Button>(R.id.errorHandlingButton)
        val testNetworkExceptionButton = findViewById<Button>(R.id.networkExceptionButton)
        val testEncryptionButton = findViewById<Button>(R.id.encryptionButton)
        val testRetryButton = findViewById<Button>(R.id.retryButton)
        val testCacheButton = findViewById<Button>(R.id.cacheButton)

        // Initialize the SDK with basic configuration
        initializeSDK()

        // Create the API service
        apiService = EasyConnectionClient.createService(JsonPlaceholderApi::class.java)

        // Set up basic CRUD test buttons
        testGetButton.setOnClickListener { testGetRequest() }
        testPostButton.setOnClickListener { testPostRequest() }
        testPutButton.setOnClickListener { testPutRequest() }
        testDeleteButton.setOnClickListener { testDeleteRequest() }

        // Set up additional feature test buttons
        testPaginationButton.setOnClickListener { testPagination() }
        testSafeApiCallButton.setOnClickListener { testSafeApiCall() }
        testErrorHandlingButton.setOnClickListener { testErrorHandling() }
        testNetworkExceptionButton.setOnClickListener { testNetworkException() }
        testEncryptionButton.setOnClickListener { testEncryption() }
        testRetryButton.setOnClickListener { testRetry() }
        testCacheButton.setOnClickListener { testCache() }
    }

    private fun initializeSDK() {
        // Initialize with basic configuration
        EasyConnectionClient.initialize(
            baseUrl = "https://jsonplaceholder.typicode.com/",
            enableLogging = true,
            additionalHeaders = mapOf("Sample-Header" to "Test-Value"),
            retryCount = 2,
            cacheDurationSeconds = 60,
            forceCacheEnabled = true
        )
    }

    // Basic CRUD test methods
    private fun testGetRequest() {
        resultTextView.text = "Testing GET request..."

        lifecycleScope.launch {
            try {
                val response = apiService.getPosts()

                if (response.isSuccessful) {
                    val posts = response.body()
                    val result = StringBuilder()
                    result.append("GET TEST SUCCESSFUL!\n\n")
                    result.append("Found ${posts?.size} posts\n\n")

                    posts?.take(3)?.forEach { post ->
                        result.append("Post #${post.id}\n")
                        result.append("Title: ${post.title}\n\n")
                    }

                    resultTextView.text = result.toString()
                } else {
                    resultTextView.text = "Error: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                resultTextView.text = "Exception: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    private fun testPostRequest() {
        resultTextView.text = "Testing POST request..."

        lifecycleScope.launch {
            try {
                // Create a new post
                val newPost = Post(
                    id = 0, // The API will assign the actual ID
                    userId = 1,
                    title = "Test Post Title",
                    body = "This is a test post created via the SDK"
                )

                val response = apiService.createPost(newPost)

                if (response.isSuccessful) {
                    val createdPost = response.body()
                    val result = StringBuilder()
                    result.append("POST TEST SUCCESSFUL!\n\n")
                    result.append("Created Post:\n")
                    result.append("ID: ${createdPost?.id}\n")
                    result.append("Title: ${createdPost?.title}\n")
                    result.append("Body: ${createdPost?.body}\n")

                    resultTextView.text = result.toString()
                } else {
                    resultTextView.text = "Error: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                resultTextView.text = "Exception: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    private fun testPutRequest() {
        resultTextView.text = "Testing PUT request..."

        lifecycleScope.launch {
            try {
                // Update an existing post (ID 1)
                val updatedPost = Post(
                    id = 1,
                    userId = 1,
                    title = "Updated Title",
                    body = "This post was updated via the SDK"
                )

                val response = apiService.updatePost(1, updatedPost)

                if (response.isSuccessful) {
                    val result = StringBuilder()
                    result.append("PUT TEST SUCCESSFUL!\n\n")
                    result.append("Updated Post:\n")
                    result.append("ID: ${response.body()?.id}\n")
                    result.append("Title: ${response.body()?.title}\n")
                    result.append("Body: ${response.body()?.body}\n")

                    resultTextView.text = result.toString()
                } else {
                    resultTextView.text = "Error: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                resultTextView.text = "Exception: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    private fun testDeleteRequest() {
        resultTextView.text = "Testing DELETE request..."

        lifecycleScope.launch {
            try {
                // Delete post with ID 1
                val response = apiService.deletePost(1)

                if (response.isSuccessful) {
                    val result = StringBuilder()
                    result.append("DELETE TEST SUCCESSFUL!\n\n")
                    result.append("Post with ID 1 was deleted\n")
                    result.append("Status code: ${response.code()}")

                    resultTextView.text = result.toString()
                } else {
                    resultTextView.text = "Error: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                resultTextView.text = "Exception: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    // Additional feature tests
    private fun testPagination() {
        resultTextView.text = "Testing Pagination..."

        lifecycleScope.launch {
            try {
                // Get first page with 5 items per page
                val page = 1
                val limit = 5
                val response = apiService.getPagedPosts(page, limit)

                if (response.isSuccessful) {
                    val posts = response.body() ?: listOf()

                    // Create a PagedResponse manually since JSONPlaceholder
                    // doesn't return pagination metadata
                    val totalItems = 100 // Assuming 100 total posts
                    val pagedResponse = PagedResponse.create(
                        items = posts,
                        page = page,
                        pageSize = limit,
                        totalItems = totalItems
                    )

                    val result = StringBuilder()
                    result.append("PAGINATION TEST SUCCESSFUL!\n\n")
                    result.append("Page: ${pagedResponse.page}/${pagedResponse.totalPages}\n")
                    result.append("Items: ${pagedResponse.data.size} of ${pagedResponse.totalItems}\n")
                    result.append("Has next page: ${pagedResponse.hasNextPage}\n\n")

                    pagedResponse.data.forEach { post ->
                        result.append("ID: ${post.id} - ${post.title.take(20)}...\n")
                    }

                    resultTextView.text = result.toString()
                } else {
                    resultTextView.text = "Error: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                resultTextView.text = "Exception: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    private fun testSafeApiCall() {
        resultTextView.text = "Testing SafeApiCall..."

        lifecycleScope.launch {
            // Using safeApiCall for cleaner error handling
            val result = EasyConnectionClient.safeApiCall { apiService.getPost(1) }

            val output = StringBuilder()
            output.append("SAFE API CALL TEST\n\n")

            // Using the functional API
            result.onSuccess { post ->
                output.append("SUCCESS!\n\n")
                output.append("Post: ${post.title}\n\n")
            }.onFailure { code, message ->
                output.append("API ERROR: $code\n")
                output.append("Message: $message\n\n")
            }.onError { exception ->
                output.append("EXCEPTION: ${exception.message}\n\n")
            }

            // Alternative approach with when expression
            output.append("Alternative with 'when':\n")
            when (result) {
                is ApiResponse.Success -> {
                    output.append("SUCCESS using when!\n")
                }
                is ApiResponse.Failure -> {
                    output.append("FAILURE using when! ${result.code}\n")
                }
                is ApiResponse.Error -> {
                    output.append("ERROR using when! ${result.exception.message}\n")
                }
            }

            resultTextView.text = output.toString()
        }
    }

    private fun testErrorHandling() {
        resultTextView.text = "Testing Error Handling..."

        lifecycleScope.launch {
            // Test with a non-existent post (will return 404)
            val result = EasyConnectionClient.safeApiCall { apiService.getNonExistentPost() }

            val output = StringBuilder()
            output.append("ERROR HANDLING TEST\n\n")

            result.onSuccess { post ->
                output.append("SUCCESS (unexpected): ${post.title}\n\n")
            }.onFailure { code, message ->
                output.append("Expected API ERROR:\n")
                output.append("Status Code: $code\n")
                output.append("Message: $message\n\n")

                // Show proper error handling
                when (code) {
                    404 -> output.append("Handled as: Resource not found\n")
                    in 400..499 -> output.append("Handled as: Client error\n")
                    in 500..599 -> output.append("Handled as: Server error\n")
                    else -> output.append("Handled as: Unknown error\n")
                }
            }.onError { exception ->
                output.append("EXCEPTION: ${exception.message}\n\n")
            }

            resultTextView.text = output.toString()
        }
    }

    private fun testNetworkException() {
        resultTextView.text = "Testing Network Exception Handling..."

        lifecycleScope.launch {
            // Test with invalid endpoint to trigger exception
            val result = EasyConnectionClient.safeApiCall { apiService.getInvalidEndpoint() }

            val output = StringBuilder()
            output.append("NETWORK EXCEPTION TEST\n\n")

            result.onSuccess { post ->
                output.append("SUCCESS (unexpected): ${post.title}\n\n")
            }.onFailure { code, message ->
                output.append("API ERROR: $code\n")
                output.append("Message: $message\n\n")
            }.onError { exception ->
                output.append("EXCEPTION: ${exception::class.java.simpleName}\n")
                output.append("Message: ${exception.message}\n\n")

                // Demonstrate NetworkException handling
                when (exception) {
                    is NetworkException.ConnectionException -> {
                        output.append("Handled as: Connection issue\n")
                        output.append("User friendly message: Please check your internet connection\n")
                    }
                    is NetworkException.TimeoutException -> {
                        output.append("Handled as: Request timeout\n")
                        output.append("User friendly message: The server is taking too long to respond\n")
                    }
                    is NetworkException.ServerException -> {
                        output.append("Handled as: Server error\n")
                        output.append("User friendly message: The server returned an error\n")
                    }
                    else -> {
                        output.append("Handled as: General error\n")
                        output.append("User friendly message: Something went wrong\n")
                    }
                }
            }

            resultTextView.text = output.toString()
        }
    }

    private fun testEncryption() {
        resultTextView.text = "Testing Encryption..."

        // Re-initialize with encryption enabled in TEST MODE
        EasyConnectionClient.initialize(
            baseUrl = "https://jsonplaceholder.typicode.com/",
            encryptionKey = "ThisIsA32ByteKeyForAES256Encryption",
            encryptionSalt = "ExtraSalt",
            encryptionTestMode = true, // Enable test mode for public APIs
            enableLogging = true
        )

        // Re-create service with updated config
        apiService = EasyConnectionClient.createService(JsonPlaceholderApi::class.java)

        lifecycleScope.launch {
            try {
                // Create a new post with simulated encryption
                val newPost = Post(
                    id = 0,
                    userId = 1,
                    title = "Encrypted Post Title",
                    body = "This post body is being sent with encryption test mode enabled"
                )

                val response = apiService.createPost(newPost)

                if (response.isSuccessful) {
                    val result = StringBuilder()
                    result.append("ENCRYPTION TEST SUCCESSFUL!\n\n")
                    result.append("Created post with encryption headers:\n")
                    result.append("ID: ${response.body()?.id}\n")
                    result.append("Title: ${response.body()?.title}\n")
                    result.append("Body: ${response.body()?.body}\n\n")
                    result.append("Note: Using encryption test mode.\n")
                    result.append("In test mode, we add encryption headers\n")
                    result.append("but don't actually encrypt the payload.\n")
                    result.append("This allows testing with public APIs\n")
                    result.append("that don't support encrypted requests.\n\n")
                    result.append("Check logs to see encryption headers.")

                    resultTextView.text = result.toString()
                } else {
                    resultTextView.text = "Error: ${response.code()} - ${response.message()}"
                }

                // Restore original configuration
                initializeSDK()
                apiService = EasyConnectionClient.createService(JsonPlaceholderApi::class.java)

            } catch (e: Exception) {
                resultTextView.text = "Exception: ${e.message}"
                e.printStackTrace()

                // Restore original configuration on error
                initializeSDK()
                apiService = EasyConnectionClient.createService(JsonPlaceholderApi::class.java)
            }
        }
    }
    private fun testRetry() {
        resultTextView.text = "Testing Retry Logic..."

        // Re-initialize with retry enabled
        EasyConnectionClient.initialize(
            baseUrl = "https://jsonplaceholder.typicode.com/",
            retryCount = 3,
            enableLogging = true
        )

        // Re-create service with updated config
        apiService = EasyConnectionClient.createService(JsonPlaceholderApi::class.java)

        lifecycleScope.launch {
            // Invalid endpoint to trigger retry logic
            val result = EasyConnectionClient.safeApiCall { apiService.getInvalidEndpoint() }

            val output = StringBuilder()
            output.append("RETRY TEST\n\n")
            output.append("Attempted with retry count: 3\n\n")

            result.onSuccess { post ->
                output.append("SUCCESS (unexpected): ${post.title}\n\n")
            }.onFailure { code, message ->
                output.append("API ERROR after retries: $code\n")
                output.append("Message: $message\n\n")
            }.onError { exception ->
                output.append("EXCEPTION after retries: ${exception::class.java.simpleName}\n")
                output.append("Message: ${exception.message}\n\n")
                output.append("Check logs to see retry attempts.")
            }

            resultTextView.text = output.toString()

            // Restore original configuration
            initializeSDK()
            apiService = EasyConnectionClient.createService(JsonPlaceholderApi::class.java)
        }
    }

    private fun testCache() {
        resultTextView.text = "Testing Cache..."

        // Re-initialize with caching enabled
        EasyConnectionClient.initialize(
            baseUrl = "https://jsonplaceholder.typicode.com/",
            cacheDurationSeconds = 60,
            enableLogging = true
        )

        // Re-create service with updated config
        apiService = EasyConnectionClient.createService(JsonPlaceholderApi::class.java)

        lifecycleScope.launch {
            try {
                // First request - from network
                resultTextView.text = "Making first request (from network)..."
                val firstResponse = apiService.getPost(1)

                val result = StringBuilder()
                result.append("CACHE TEST\n\n")
                result.append("First request (from network):\n")

                if (firstResponse.isSuccessful) {
                    result.append("SUCCESS\n")
                    result.append("Post title: ${firstResponse.body()?.title}\n\n")
                } else {
                    result.append("ERROR: ${firstResponse.code()}\n\n")
                }

                // Second request - should use cache
                result.append("Making second request (should use cache)...\n")
                resultTextView.text = result.toString()

                val secondResponse = apiService.getPost(1)

                if (secondResponse.isSuccessful) {
                    result.append("SUCCESS\n")
                    result.append("Post title: ${secondResponse.body()?.title}\n\n")
                    result.append("Check logs to see if response was from cache.")
                } else {
                    result.append("ERROR: ${secondResponse.code()}\n\n")
                }

                resultTextView.text = result.toString()

                // Restore original configuration
                initializeSDK()
                apiService = EasyConnectionClient.createService(JsonPlaceholderApi::class.java)

            } catch (e: Exception) {
                resultTextView.text = "Exception: ${e.message}"
                e.printStackTrace()

                // Restore original configuration on error
                initializeSDK()
                apiService = EasyConnectionClient.createService(JsonPlaceholderApi::class.java)
            }
        }
    }
}