/**
 * EncryptionInterceptor.kt
 * Handles encryption/decryption of request and response bodies
 */
package com.kamesh.easyconnectionsdk.data.network.interceptors

import android.util.Base64
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

/**
 * Interceptor to encrypt request bodies and decrypt response bodies
 * Can operate in "test mode" for APIs that don't support encrypted requests
 */
class EncryptionInterceptor(
    private val encryptionKey: String,
    private val salt: String = "",
    private val testMode: Boolean = false
) : Interceptor {

    companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
        private const val KEY_SIZE_BYTES = 32 // 256 bits
        private const val IV_SIZE_BYTES = 16 // 128 bits
    }

    private val secretKey: SecretKey by lazy {
        generateKey(encryptionKey, salt)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Only encrypt non-GET requests that have a body
        val request = if (original.method != "GET" && original.body != null) {
            val requestBody = original.body?.let {
                // Read the original request body
                val buffer = okio.Buffer()
                it.writeTo(buffer)
                val bodyString = buffer.readUtf8()

                if (testMode) {
                    // In test mode, add encryption headers but don't actually encrypt
                    // This allows testing with APIs that don't support encrypted bodies
                    bodyString.toRequestBody("application/json".toMediaTypeOrNull())
                } else {
                    // Encrypt the body in normal mode
                    val encryptionResult = encrypt(bodyString)
                    encryptionResult.encryptedData.toRequestBody("application/json".toMediaTypeOrNull())
                }
            }

            // Create a new request with the appropriate headers
            val requestBuilder = original.newBuilder()
                .method(original.method, requestBody)

            if (testMode) {
                // In test mode, just add headers to simulate encryption
                requestBuilder.header("X-Encryption-Enabled", "True")
                requestBuilder.header("X-Encryption-Algorithm", TRANSFORMATION)
                requestBuilder.header("X-Encryption-Test-Mode", "True")
            } else {
                // In normal mode, add proper encryption headers
                requestBuilder.header("Content-Encryption", "AES")
                requestBuilder.header("Encryption-IV", encryptIv())
            }

            requestBuilder.build()
        } else {
            original
        }

        // Proceed with the request
        val response = chain.proceed(request)

        // Only attempt decryption in normal mode and if response is encrypted
        if (!testMode && response.header("Content-Encryption") == "AES") {
            val responseBody = response.body
            if (responseBody != null) {
                val contentType = responseBody.contentType()
                val encryptedContent = responseBody.string()
                val iv = decryptIv(response.header("Encryption-IV") ?: "")
                val decryptedContent = decrypt(encryptedContent, iv)

                // Create a new response with the decrypted body
                return response.newBuilder()
                    .body(decryptedContent.toResponseBody(contentType))
                    .removeHeader("Content-Encryption")
                    .removeHeader("Encryption-IV")
                    .build()
            }
        }

        return response
    }

    /**
     * Generate a secure encryption key using the provided key and salt
     */
    private fun generateKey(key: String, salt: String): SecretKey {
        val combinedKey = key + salt
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(combinedKey.toByteArray())
        return SecretKeySpec(keyBytes.copyOf(KEY_SIZE_BYTES), ALGORITHM)
    }

    /**
     * Encrypt a string using the selected algorithm
     */
    private fun encrypt(text: String): EncryptionResult {
        val iv = generateIv()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        val encryptedBytes = cipher.doFinal(text.toByteArray())
        return EncryptionResult(
            encryptedData = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
            iv = iv
        )
    }

    /**
     * Decrypt a string using the selected algorithm
     */
    private fun decrypt(encryptedText: String, iv: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        val encryptedBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes)
    }

    /**
     * Generate a cryptographically secure random initialization vector
     */
    private fun generateIv(): ByteArray {
        val iv = ByteArray(IV_SIZE_BYTES)
        SecureRandom().nextBytes(iv)
        return iv
    }

    /**
     * Encrypt IV for transport
     */
    private fun encryptIv(): String {
        val iv = generateIv()
        return Base64.encodeToString(iv, Base64.NO_WRAP)
    }

    /**
     * Decrypt IV from transport
     */
    private fun decryptIv(encodedIv: String): ByteArray {
        return Base64.decode(encodedIv, Base64.NO_WRAP)
    }

    /**
     * Data class to hold encryption results
     */
    private data class EncryptionResult(
        val encryptedData: String,
        val iv: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EncryptionResult

            if (encryptedData != other.encryptedData) return false
            if (!iv.contentEquals(other.iv)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = encryptedData.hashCode()
            result = 31 * result + iv.contentHashCode()
            return result
        }
    }
}