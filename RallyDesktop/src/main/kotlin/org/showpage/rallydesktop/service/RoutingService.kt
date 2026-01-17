package org.showpage.rallydesktop.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.awt.Color
import java.util.concurrent.TimeUnit

/**
 * A single point in a route's geometry.
 */
data class RoutePoint(
    val latitude: Double,
    val longitude: Double
)

/**
 * A segment of a route between waypoints, belonging to a specific leg.
 */
data class RouteSegment(
    val legIndex: Int,
    val points: List<RoutePoint>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val color: Color
)

/**
 * Complete route calculation result.
 */
data class RouteResult(
    val segments: List<RouteSegment>,
    val totalDistanceMeters: Double,
    val totalDurationSeconds: Double
) {
    val totalDistanceMiles: Double
        get() = totalDistanceMeters / 1609.344

    val totalDistanceKilometers: Double
        get() = totalDistanceMeters / 1000.0

    fun formattedDuration(): String {
        val hours = (totalDurationSeconds / 3600).toInt()
        val minutes = ((totalDurationSeconds % 3600) / 60).toInt()
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
}

/**
 * Input waypoint for routing - just coordinates and leg assignment.
 */
data class RoutingWaypoint(
    val latitude: Double,
    val longitude: Double,
    val legIndex: Int
)

/**
 * Service for calculating routes between waypoints using OSRM (Open Source Routing Machine).
 * Uses the public demo server at router.project-osrm.org.
 */
class RoutingService {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    companion object {
        private const val OSRM_BASE_URL = "https://router.project-osrm.org"

        // Distinct colors for different legs
        val LEG_COLORS = listOf(
            Color(0, 102, 204),    // Blue
            Color(204, 0, 102),    // Magenta
            Color(0, 153, 76),     // Green
            Color(255, 128, 0),    // Orange
            Color(102, 0, 204),    // Purple
            Color(0, 153, 153),    // Teal
            Color(204, 102, 0),    // Brown
            Color(102, 153, 0)     // Olive
        )

        fun getColorForLeg(legIndex: Int): Color {
            return LEG_COLORS[legIndex % LEG_COLORS.size]
        }
    }

    /**
     * Calculate a route through the given waypoints.
     *
     * @param waypoints List of waypoints with their leg assignments
     * @return RouteResult containing segments for each leg, or null if routing fails
     */
    fun calculateRoute(waypoints: List<RoutingWaypoint>): RouteResult? {
        if (waypoints.size < 2) {
            logger.warn("Need at least 2 waypoints to calculate a route")
            return null
        }

        logger.info("Calculating route through {} waypoints", waypoints.size)

        // Build coordinates string for OSRM: lon,lat;lon,lat;...
        val coordinates = waypoints.joinToString(";") { wp ->
            "${wp.longitude},${wp.latitude}"
        }

        // Request full geometry and step-by-step information
        val url = "$OSRM_BASE_URL/route/v1/driving/$coordinates" +
                "?overview=full" +
                "&geometries=geojson" +
                "&steps=true"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "RallyMaster/1.0")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.error("OSRM request failed with status: {}", response.code)
                    return null
                }

                val body = response.body?.string() ?: return null
                parseOsrmResponse(body, waypoints)
            }
        } catch (e: Exception) {
            logger.error("Error calculating route", e)
            null
        }
    }

    /**
     * Parse OSRM JSON response into RouteResult.
     */
    private fun parseOsrmResponse(json: String, waypoints: List<RoutingWaypoint>): RouteResult? {
        return try {
            val response: Map<String, Any> = objectMapper.readValue(json)

            val code = response["code"] as? String
            if (code != "Ok") {
                logger.warn("OSRM returned non-OK code: {}", code)
                return null
            }

            @Suppress("UNCHECKED_CAST")
            val routes = response["routes"] as? List<Map<String, Any>> ?: return null
            if (routes.isEmpty()) {
                logger.warn("OSRM returned no routes")
                return null
            }

            val route = routes[0]
            val totalDistance = (route["distance"] as Number).toDouble()
            val totalDuration = (route["duration"] as Number).toDouble()

            @Suppress("UNCHECKED_CAST")
            val geometry = route["geometry"] as? Map<String, Any> ?: return null

            @Suppress("UNCHECKED_CAST")
            val coordinates = geometry["coordinates"] as? List<List<Double>> ?: return null

            // Convert coordinates to RoutePoints
            val allPoints = coordinates.map { coord ->
                RoutePoint(latitude = coord[1], longitude = coord[0])
            }

            // Get legs information for splitting the route
            @Suppress("UNCHECKED_CAST")
            val legs = route["legs"] as? List<Map<String, Any>> ?: emptyList()

            // Build segments based on OSRM legs (each leg = between consecutive waypoints)
            val segments = mutableListOf<RouteSegment>()
            var pointIndex = 0

            for ((osrmLegIdx, leg) in legs.withIndex()) {
                val legDistance = (leg["distance"] as Number).toDouble()
                val legDuration = (leg["duration"] as Number).toDouble()

                @Suppress("UNCHECKED_CAST")
                val steps = leg["steps"] as? List<Map<String, Any>> ?: emptyList()

                // Count points in this leg by summing step geometry points
                var legPointCount = 0
                for (step in steps) {
                    @Suppress("UNCHECKED_CAST")
                    val stepGeometry = step["geometry"] as? Map<String, Any>
                    @Suppress("UNCHECKED_CAST")
                    val stepCoords = stepGeometry?.get("coordinates") as? List<List<Double>>
                    legPointCount += (stepCoords?.size ?: 1) - 1
                }
                legPointCount++ // Add one for the final point

                // If we can't determine points from steps, estimate based on fraction
                if (legPointCount <= 1 && allPoints.isNotEmpty() && legs.isNotEmpty()) {
                    legPointCount = maxOf(2, allPoints.size / legs.size)
                }

                // Get the ride leg index from our waypoints (for color assignment)
                // The OSRM leg index corresponds to the segment between waypoints[i] and waypoints[i+1]
                val rideLegIndex = if (osrmLegIdx < waypoints.size) waypoints[osrmLegIdx].legIndex else 0

                // Extract points for this leg
                val endIndex = minOf(pointIndex + legPointCount, allPoints.size)
                val legPoints = if (pointIndex < allPoints.size) {
                    allPoints.subList(pointIndex, endIndex)
                } else {
                    emptyList()
                }

                if (legPoints.isNotEmpty()) {
                    segments.add(
                        RouteSegment(
                            legIndex = rideLegIndex,
                            points = legPoints,
                            distanceMeters = legDistance,
                            durationSeconds = legDuration,
                            color = getColorForLeg(rideLegIndex)
                        )
                    )
                }

                pointIndex = maxOf(pointIndex, endIndex - 1)
            }

            // If we couldn't split by legs, just create one segment with all points
            if (segments.isEmpty() && allPoints.isNotEmpty()) {
                segments.add(
                    RouteSegment(
                        legIndex = 0,
                        points = allPoints,
                        distanceMeters = totalDistance,
                        durationSeconds = totalDuration,
                        color = getColorForLeg(0)
                    )
                )
            }

            logger.info(
                "Route calculated: {} segments, {:.1f} miles, {}",
                segments.size,
                totalDistance / 1609.344,
                formatDuration(totalDuration)
            )

            RouteResult(
                segments = segments,
                totalDistanceMeters = totalDistance,
                totalDurationSeconds = totalDuration
            )
        } catch (e: Exception) {
            logger.error("Error parsing OSRM response", e)
            null
        }
    }

    /**
     * Format distance for display.
     */
    fun formatDistance(meters: Double, useMiles: Boolean = true): String {
        return if (useMiles) {
            String.format("%.1f mi", meters / 1609.344)
        } else {
            String.format("%.1f km", meters / 1000.0)
        }
    }

    /**
     * Format duration for display.
     */
    fun formatDuration(seconds: Double): String {
        val hours = (seconds / 3600).toInt()
        val minutes = ((seconds % 3600) / 60).toInt()
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    /**
     * Calculate straight-line distance between two points using Haversine formula.
     * Returns distance in meters.
     */
    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Earth's radius in meters

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }
}
