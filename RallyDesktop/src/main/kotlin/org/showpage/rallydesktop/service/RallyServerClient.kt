package org.showpage.rallydesktop.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.showpage.rallyserver.RestResponse
import org.showpage.rallyserver.ui.*
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.Base64

/**
 * REST client for communicating with the RallyServer API.
 * Uses RallyCommon DTOs for request/response objects.
 */
class RallyServerClient(private val serverUrl: String) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private var accessToken: String? = null
    private var refreshToken: String? = null

    /**
     * Login with email and password using Basic Authentication.
     * Returns AuthResponse with access and refresh tokens.
     */
    fun login(email: String, password: String): Result<AuthResponse> {
        logger.info("Attempting login for user: {}", email)

        val credentials = "$email:$password"
        val encodedCredentials = Base64.getEncoder().encodeToString(credentials.toByteArray())

        val request = Request.Builder()
            .url("$serverUrl/api/auth/login")
            .header("Authorization", "Basic $encodedCredentials")
            .post("".toRequestBody())
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    val restResponse: RestResponse<AuthResponse> = objectMapper.readValue(body)
                    if (restResponse.isSuccess && restResponse.data != null) {
                        val authResponse = restResponse.data
                        this.accessToken = authResponse.accessToken
                        this.refreshToken = authResponse.refreshToken
                        logger.info("Login successful for user: {}", email)
                        Result.success(authResponse)
                    } else {
                        logger.error("Login failed: {}", restResponse.message)
                        Result.failure(Exception(restResponse.message ?: "Login failed"))
                    }
                } else {
                    logger.error("Login failed with status: {} - {}", response.code, body)
                    Result.failure(Exception("Login failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            logger.error("Error during login", e)
            Result.failure(e)
        }
    }

    /**
     * Register a new user.
     */
    fun register(email: String, password: String): Result<UiMember> {
        logger.info("Attempting registration for user: {}", email)

        val request = Request.Builder()
            .url("$serverUrl/api/auth/register?email=$email&password=$password")
            .post("".toRequestBody())
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    val restResponse: RestResponse<UiMember> = objectMapper.readValue(body)
                    if (restResponse.isSuccess && restResponse.data != null) {
                        logger.info("Registration successful for user: {}", email)
                        Result.success(restResponse.data)
                    } else {
                        logger.error("Registration failed: {}", restResponse.message)
                        Result.failure(Exception(restResponse.message ?: "Registration failed"))
                    }
                } else {
                    logger.error("Registration failed with status: {} - {}", response.code, body)
                    Result.failure(Exception("Registration failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            logger.error("Error during registration", e)
            Result.failure(e)
        }
    }

    /**
     * Refresh the access token using the refresh token.
     */
    fun refreshAccessToken(): Result<AuthResponse> {
        val currentRefreshToken = this.refreshToken
            ?: return Result.failure(Exception("No refresh token available"))

        logger.debug("Refreshing access token")

        val tokenRequest = TokenRequest(currentRefreshToken)
        val json = objectMapper.writeValueAsString(tokenRequest)

        val request = Request.Builder()
            .url("$serverUrl/api/auth/token")
            .post(json.toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    val restResponse: RestResponse<AuthResponse> = objectMapper.readValue(body)
                    if (restResponse.isSuccess && restResponse.data != null) {
                        val authResponse = restResponse.data
                        this.accessToken = authResponse.accessToken
                        this.refreshToken = authResponse.refreshToken
                        logger.debug("Token refresh successful")
                        Result.success(authResponse)
                    } else {
                        logger.error("Token refresh failed: {}", restResponse.message)
                        Result.failure(Exception(restResponse.message ?: "Token refresh failed"))
                    }
                } else {
                    logger.error("Token refresh failed with status: {}", response.code)
                    Result.failure(Exception("Token refresh failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            logger.error("Error during token refresh", e)
            Result.failure(e)
        }
    }

    /**
     * Get current member info.
     */
    fun getMemberInfo(): Result<UiMember> {
        return authenticatedGet("/api/member/info")
    }

    /**
     * Get all rallies.
     */
    fun getAllRallies(): Result<PageResponse<UiRally>> {
        return authenticatedGet("/api/rally")
    }

    /**
     * Make an authenticated GET request.
     */
    private inline fun <reified T> authenticatedGet(path: String): Result<T> {
        val token = accessToken ?: return Result.failure(Exception("Not authenticated"))

        val request = Request.Builder()
            .url("$serverUrl$path")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    val restResponse: RestResponse<T> = objectMapper.readValue(body)
                    if (restResponse.isSuccess && restResponse.data != null) {
                        Result.success(restResponse.data)
                    } else {
                        Result.failure(Exception(restResponse.message ?: "Request failed"))
                    }
                } else if (response.code == 401) {
                    // Token expired - return error (caller should handle refresh)
                    Result.failure(Exception("Authentication failed: Token expired"))
                } else {
                    Result.failure(Exception("Request failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            logger.error("Error during authenticated request", e)
            Result.failure(e)
        }
    }

    /**
     * Check if the client is authenticated.
     */
    fun isAuthenticated(): Boolean {
        return accessToken != null
    }

    /**
     * Clear authentication tokens.
     */
    fun logout() {
        logger.info("Logging out")
        accessToken = null
        refreshToken = null
    }
}
