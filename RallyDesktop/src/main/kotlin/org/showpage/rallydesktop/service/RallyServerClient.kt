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
     * Get a rally by ID.
     */
    fun getRally(rallyId: Int): Result<UiRally> {
        logger.info("Getting rally with ID: {}", rallyId)
        return authenticatedGet("/api/rally/$rallyId")
    }

    /**
     * Create a new rally.
     */
    fun createRally(request: CreateRallyRequest): Result<UiRally> {
        logger.info("Creating rally: {}", request.name)
        return authenticatedPost("/api/rally", request)
    }

    /**
     * Get all bonus points for a rally.
     */
    fun listBonusPoints(rallyId: Int): Result<List<UiBonusPoint>> {
        logger.info("Listing bonus points for rally: {}", rallyId)
        return authenticatedGet("/api/rally/$rallyId/bonuspoints")
    }

    /**
     * Create a new bonus point in a rally.
     */
    fun createBonusPoint(rallyId: Int, request: CreateBonusPointRequest): Result<UiBonusPoint> {
        logger.info("Creating bonus point in rally {}: {}", rallyId, request.code)
        return authenticatedPost("/api/rally/$rallyId/bonuspoint", request)
    }

    /**
     * Update an existing bonus point.
     */
    fun updateBonusPoint(bonusPointId: Int, request: UpdateBonusPointRequest): Result<UiBonusPoint> {
        logger.info("Updating bonus point: {}", bonusPointId)
        return authenticatedPut("/api/bonuspoint/$bonusPointId", request)
    }

    /**
     * Delete a bonus point.
     */
    fun deleteBonusPoint(bonusPointId: Int): Result<Unit> {
        logger.info("Deleting bonus point: {}", bonusPointId)
        return authenticatedDelete("/api/bonuspoint/$bonusPointId")
    }

    /**
     * Get all combinations for a rally.
     */
    fun listCombinations(rallyId: Int): Result<List<UiCombination>> {
        logger.info("Listing combinations for rally: {}", rallyId)
        return authenticatedGet("/api/rally/$rallyId/combinations")
    }

    /**
     * Create a new combination in a rally.
     */
    fun createCombination(rallyId: Int, request: CreateCombinationRequest): Result<UiCombination> {
        logger.info("Creating combination in rally {}: {}", rallyId, request.code)
        return authenticatedPost("/api/rally/$rallyId/combination", request)
    }

    /**
     * Update an existing combination.
     */
    fun updateCombination(combinationId: Int, request: UpdateCombinationRequest): Result<UiCombination> {
        logger.info("Updating combination: {}", combinationId)
        return authenticatedPut("/api/combination/$combinationId", request)
    }

    /**
     * Delete a combination.
     */
    fun deleteCombination(combinationId: Int): Result<Unit> {
        logger.info("Deleting combination: {}", combinationId)
        return authenticatedDelete("/api/combination/$combinationId")
    }

    //======================================================================
    // Ride Management
    //======================================================================

    /**
     * Create a new ride.
     */
    fun createRide(request: CreateRideRequest): Result<UiRide> {
        logger.info("Creating ride: {}", request.name)
        return authenticatedPost("/api/ride", request)
    }

    /**
     * Update an existing ride.
     */
    fun updateRide(rideId: Int, request: UpdateRideRequest): Result<UiRide> {
        logger.info("Updating ride: {}", rideId)
        return authenticatedPut("/api/ride/$rideId", request)
    }

    /**
     * Get a ride by ID.
     */
    fun getRide(rideId: Int): Result<UiRide> {
        logger.info("Getting ride with ID: {}", rideId)
        return authenticatedGet("/api/ride/$rideId")
    }

    /**
     * List all rides for the current user.
     */
    fun listRides(): Result<List<UiRide>> {
        logger.info("Listing rides")
        return authenticatedGet("/api/rides")
    }

    /**
     * Get the user's ride for a specific rally, if one exists.
     * Returns null if no ride exists for this rally.
     */
    fun getRideForRally(rallyId: Int): Result<UiRide?> {
        logger.info("Getting ride for rally: {}", rallyId)
        return listRides().map { rides ->
            rides.find { it.rallyId == rallyId }
        }
    }

    /**
     * Delete a ride.
     */
    fun deleteRide(rideId: Int): Result<Unit> {
        logger.info("Deleting ride: {}", rideId)
        return authenticatedDelete("/api/ride/$rideId")
    }

    //======================================================================
    // Route Management
    //======================================================================

    /**
     * Create a new route for a ride.
     */
    fun createRoute(rideId: Int, request: CreateRouteRequest): Result<UiRoute> {
        logger.info("Creating route for ride {}: {}", rideId, request.name)
        return authenticatedPost("/api/ride/$rideId/route", request)
    }

    /**
     * Update an existing route.
     */
    fun updateRoute(routeId: Int, request: UpdateRouteRequest): Result<UiRoute> {
        logger.info("Updating route: {}", routeId)
        return authenticatedPut("/api/route/$routeId", request)
    }

    /**
     * Get a route by ID.
     */
    fun getRoute(routeId: Int): Result<UiRoute> {
        logger.info("Getting route with ID: {}", routeId)
        return authenticatedGet("/api/route/$routeId")
    }

    /**
     * List all routes for a ride.
     */
    fun listRoutes(rideId: Int): Result<List<UiRoute>> {
        logger.info("Listing routes for ride: {}", rideId)
        return authenticatedGet("/api/ride/$rideId/routes")
    }

    /**
     * Delete a route.
     */
    fun deleteRoute(routeId: Int): Result<Unit> {
        logger.info("Deleting route: {}", routeId)
        return authenticatedDelete("/api/route/$routeId")
    }

    //======================================================================
    // Ride Leg Management
    //======================================================================

    /**
     * Create a new ride leg for a route.
     */
    fun createRideLeg(routeId: Int, request: CreateRideLegRequest): Result<UiRideLeg> {
        logger.info("Creating ride leg for route {}: {}", routeId, request.name)
        return authenticatedPost("/api/route/$routeId/leg", request)
    }

    /**
     * Update an existing ride leg.
     */
    fun updateRideLeg(legId: Int, request: UpdateRideLegRequest): Result<UiRideLeg> {
        logger.info("Updating ride leg: {}", legId)
        return authenticatedPut("/api/leg/$legId", request)
    }

    /**
     * Get a ride leg by ID.
     */
    fun getRideLeg(legId: Int): Result<UiRideLeg> {
        logger.info("Getting ride leg with ID: {}", legId)
        return authenticatedGet("/api/leg/$legId")
    }

    /**
     * List all ride legs for a route.
     */
    fun listRideLegs(routeId: Int): Result<List<UiRideLeg>> {
        logger.info("Listing ride legs for route: {}", routeId)
        return authenticatedGet("/api/route/$routeId/legs")
    }

    /**
     * Delete a ride leg.
     */
    fun deleteRideLeg(legId: Int): Result<Unit> {
        logger.info("Deleting ride leg: {}", legId)
        return authenticatedDelete("/api/leg/$legId")
    }

    //======================================================================
    // Waypoint Management
    //======================================================================

    /**
     * Create a new waypoint for a ride leg.
     */
    fun createWaypoint(legId: Int, request: CreateWaypointRequest): Result<UiWaypoint> {
        logger.info("Creating waypoint for leg {}: {}", legId, request.name)
        return authenticatedPost("/api/leg/$legId/waypoint", request)
    }

    /**
     * Update an existing waypoint.
     */
    fun updateWaypoint(waypointId: Int, request: UpdateWaypointRequest): Result<UiWaypoint> {
        logger.info("Updating waypoint: {}", waypointId)
        return authenticatedPut("/api/waypoint/$waypointId", request)
    }

    /**
     * Get a waypoint by ID.
     */
    fun getWaypoint(waypointId: Int): Result<UiWaypoint> {
        logger.info("Getting waypoint with ID: {}", waypointId)
        return authenticatedGet("/api/waypoint/$waypointId")
    }

    /**
     * List all waypoints for a ride leg.
     */
    fun listWaypoints(legId: Int): Result<List<UiWaypoint>> {
        logger.info("Listing waypoints for leg: {}", legId)
        return authenticatedGet("/api/leg/$legId/waypoints")
    }

    /**
     * Delete a waypoint.
     */
    fun deleteWaypoint(waypointId: Int): Result<Unit> {
        logger.info("Deleting waypoint: {}", waypointId)
        return authenticatedDelete("/api/waypoint/$waypointId")
    }

    /**
     * Search rallies with optional filters.
     * For "My Rallies", use all=true to include all rallies user is involved with.
     */
    fun searchRallies(
        name: String? = null,
        from: String? = null,  // ISO date format
        to: String? = null,    // ISO date format
        all: Boolean? = null,
        page: Int = 0,
        size: Int = 20
    ): Result<RestPage<UiRally>> {
        val urlBuilder = StringBuilder("$serverUrl/api/rallies?page=$page&size=$size")
        name?.let { urlBuilder.append("&name=$it") }
        from?.let { urlBuilder.append("&from=$it") }
        to?.let { urlBuilder.append("&to=$it") }
        all?.let { urlBuilder.append("&all=$it") }

        return authenticatedGet(urlBuilder.toString().removePrefix(serverUrl))
    }

    /**
     * Make an authenticated GET request.
     */
    private inline fun <reified T> authenticatedGet(path: String): Result<T> {
        val token = accessToken ?: return Result.failure(Exception("Not authenticated"))

        val fullUrl = "$serverUrl$path"
        logger.debug("Making authenticated GET request to: {}", fullUrl)

        val request = Request.Builder()
            .url(fullUrl)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                logger.debug("Response code: {}, body length: {}", response.code, body?.length ?: 0)

                if (response.isSuccessful && body != null) {
                    val restResponse: RestResponse<T> = objectMapper.readValue(body)
                    if (restResponse.isSuccess && restResponse.data != null) {
                        Result.success(restResponse.data)
                    } else {
                        logger.error("API request failed: {}", restResponse.message)
                        Result.failure(Exception(restResponse.message ?: "Request failed"))
                    }
                } else if (response.code == 401) {
                    // Token expired - return error (caller should handle refresh)
                    logger.error("Authentication failed (401) for URL: {}", fullUrl)
                    Result.failure(Exception("Authentication failed: Token expired"))
                } else {
                    logger.error("Request failed with code {} for URL: {}, body: {}", response.code, fullUrl, body)
                    Result.failure(Exception("Request failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            logger.error("Error during authenticated request to {}", fullUrl, e)
            Result.failure(e)
        }
    }

    /**
     * Make an authenticated POST request.
     */
    private inline fun <reified T> authenticatedPost(path: String, body: Any): Result<T> {
        val token = accessToken ?: return Result.failure(Exception("Not authenticated"))

        val json = objectMapper.writeValueAsString(body)
        val fullUrl = "$serverUrl$path"
        logger.debug("Making authenticated POST request to: {}", fullUrl)

        val request = Request.Builder()
            .url(fullUrl)
            .header("Authorization", "Bearer $token")
            .post(json.toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                logger.debug("Response code: {}, body length: {}", response.code, responseBody?.length ?: 0)

                if (response.isSuccessful && responseBody != null) {
                    val restResponse: RestResponse<T> = objectMapper.readValue(responseBody)
                    if (restResponse.isSuccess && restResponse.data != null) {
                        Result.success(restResponse.data)
                    } else {
                        logger.error("API request failed: {}", restResponse.message)
                        Result.failure(Exception(restResponse.message ?: "Request failed"))
                    }
                } else if (response.code == 401) {
                    logger.error("Authentication failed (401) for URL: {}", fullUrl)
                    Result.failure(Exception("Authentication failed: Token expired"))
                } else {
                    logger.error("Request failed with code {} for URL: {}, body: {}", response.code, fullUrl, responseBody)
                    Result.failure(Exception("Request failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            logger.error("Error during authenticated POST request to {}", fullUrl, e)
            Result.failure(e)
        }
    }

    /**
     * Make an authenticated PUT request.
     */
    private inline fun <reified T> authenticatedPut(path: String, body: Any): Result<T> {
        val token = accessToken ?: return Result.failure(Exception("Not authenticated"))

        val json = objectMapper.writeValueAsString(body)
        val fullUrl = "$serverUrl$path"
        logger.debug("Making authenticated PUT request to: {}", fullUrl)

        val request = Request.Builder()
            .url(fullUrl)
            .header("Authorization", "Bearer $token")
            .put(json.toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                logger.debug("Response code: {}, body length: {}", response.code, responseBody?.length ?: 0)

                if (response.isSuccessful && responseBody != null) {
                    val restResponse: RestResponse<T> = objectMapper.readValue(responseBody)
                    if (restResponse.isSuccess && restResponse.data != null) {
                        Result.success(restResponse.data)
                    } else {
                        logger.error("API request failed: {}", restResponse.message)
                        Result.failure(Exception(restResponse.message ?: "Request failed"))
                    }
                } else if (response.code == 401) {
                    logger.error("Authentication failed (401) for URL: {}", fullUrl)
                    Result.failure(Exception("Authentication failed: Token expired"))
                } else {
                    logger.error("Request failed with code {} for URL: {}, body: {}", response.code, fullUrl, responseBody)
                    Result.failure(Exception("Request failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            logger.error("Error during authenticated PUT request to {}", fullUrl, e)
            Result.failure(e)
        }
    }

    /**
     * Make an authenticated DELETE request.
     */
    private fun authenticatedDelete(path: String): Result<Unit> {
        val token = accessToken ?: return Result.failure(Exception("Not authenticated"))

        val fullUrl = "$serverUrl$path"
        logger.debug("Making authenticated DELETE request to: {}", fullUrl)

        val request = Request.Builder()
            .url(fullUrl)
            .header("Authorization", "Bearer $token")
            .delete()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                logger.debug("Response code: {}", response.code)

                if (response.isSuccessful) {
                    Result.success(Unit)
                } else if (response.code == 401) {
                    logger.error("Authentication failed (401) for URL: {}", fullUrl)
                    Result.failure(Exception("Authentication failed: Token expired"))
                } else {
                    val responseBody = response.body?.string()
                    logger.error("Request failed with code {} for URL: {}, body: {}", response.code, fullUrl, responseBody)
                    Result.failure(Exception("Request failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            logger.error("Error during authenticated DELETE request to {}", fullUrl, e)
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
