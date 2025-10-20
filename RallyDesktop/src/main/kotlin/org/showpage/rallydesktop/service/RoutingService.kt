package org.showpage.rallydesktop.service

import org.slf4j.LoggerFactory

/**
 * Service for calculating routes and distances between points.
 * Uses GraphHopper for offline routing capabilities.
 */
class RoutingService {
    private val logger = LoggerFactory.getLogger(javaClass)

    // TODO: Initialize GraphHopper with map data
    // This will require downloading OSM map data files
    // For now, this is a placeholder for the routing functionality

    data class RouteResult(
        val distanceMeters: Double,
        val durationSeconds: Long,
        val points: List<LatLng>
    )

    data class LatLng(
        val latitude: Double,
        val longitude: Double
    )

    /**
     * Calculate a route between multiple waypoints.
     *
     * @param waypoints List of coordinates to route through
     * @return RouteResult with distance, duration, and route points
     */
    fun calculateRoute(waypoints: List<LatLng>): Result<RouteResult> {
        logger.warn("GraphHopper routing not yet implemented - returning placeholder data")

        // TODO: Implement GraphHopper routing
        // This requires:
        // 1. Downloaded OSM map data
        // 2. GraphHopper initialization
        // 3. Routing calculation

        // For now, return a simple straight-line calculation
        return try {
            val distance = calculateStraightLineDistance(waypoints)
            val estimatedDuration = (distance / 100.0 * 3600.0).toLong() // Rough estimate: 100 km/h average

            Result.success(
                RouteResult(
                    distanceMeters = distance,
                    durationSeconds = estimatedDuration,
                    points = waypoints
                )
            )
        } catch (e: Exception) {
            logger.error("Error calculating route", e)
            Result.failure(e)
        }
    }

    /**
     * Calculate straight-line distance between waypoints using Haversine formula.
     */
    private fun calculateStraightLineDistance(waypoints: List<LatLng>): Double {
        if (waypoints.size < 2) return 0.0

        var totalDistance = 0.0
        for (i in 0 until waypoints.size - 1) {
            totalDistance += haversineDistance(
                waypoints[i].latitude, waypoints[i].longitude,
                waypoints[i + 1].latitude, waypoints[i + 1].longitude
            )
        }
        return totalDistance
    }

    /**
     * Calculate distance between two points using Haversine formula.
     * Returns distance in meters.
     */
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Earth's radius in meters

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Format distance for display.
     */
    fun formatDistance(meters: Double): String {
        return if (meters < 1000) {
            String.format("%.0f m", meters)
        } else {
            String.format("%.1f km", meters / 1000.0)
        }
    }

    /**
     * Format duration for display.
     */
    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) {
            String.format("%d h %d min", hours, minutes)
        } else {
            String.format("%d min", minutes)
        }
    }
}
