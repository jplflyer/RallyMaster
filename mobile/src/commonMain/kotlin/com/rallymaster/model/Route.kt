package com.rallymaster.model

import com.benasher44.uuid.uuid4
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Route data model for local JSON storage.
 *
 * Represents a planned sequence of bonus point visits with timing and alternatives
 * for Rally Rider desktop functionality.
 */
@Serializable
data class Route(
    val id: String = uuid4().toString(),
    val rallyRegistrationId: String, // Associated registration
    val name: String, // Route name (e.g., "Northern Loop Strategy")
    val plannedBonusPoints: List<String>, // Ordered list of bonus point IDs

    // Route planning details
    val estimatedDurationHours: Double = 0.0, // Total estimated time in hours
    val estimatedDistanceMiles: Double = 0.0, // Total distance in miles
    val startTime: String = "", // Planned start time (ISO string)
    val endTime: String = "", // Planned end time (ISO string)

    // Alternative routes for flexibility
    val alternativeRoutes: Map<String, List<String>> = emptyMap(), // Named alternative sub-routes

    // Export and sharing
    val exportFormats: Set<ExportFormat> = setOf(ExportFormat.JSON),
    val isShared: Boolean = false, // Whether route is shared with others

    // Status and metadata
    val isActive: Boolean = false, // Current active route for rider
    val notes: String = "",
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String = Clock.System.now().toString(),
) {

    /**
     * Creates a copy with updated timestamp
     */
    fun withUpdatedTimestamp(): Route = copy(
        updatedAt = Clock.System.now().toString(),
    )

    /**
     * Validates route information
     */
    fun isValid(): Boolean {
        return name.isNotBlank() &&
            rallyRegistrationId.isNotBlank() &&
            plannedBonusPoints.isNotEmpty() &&
            estimatedDurationHours >= 0 &&
            estimatedDistanceMiles >= 0
    }

    /**
     * Gets the number of planned stops
     */
    fun getStopCount(): Int = plannedBonusPoints.size

    /**
     * Gets estimated average speed in mph
     */
    fun getAverageSpeed(): Double {
        return if (estimatedDurationHours > 0) {
            estimatedDistanceMiles / estimatedDurationHours
        } else {
            0.0
        }
    }

    /**
     * Checks if route has alternative options
     */
    fun hasAlternatives(): Boolean = alternativeRoutes.isNotEmpty()

    /**
     * Gets all bonus point IDs including alternatives
     */
    fun getAllBonusPointIds(): Set<String> {
        val allIds = plannedBonusPoints.toMutableSet()
        alternativeRoutes.values.forEach { altRoute ->
            allIds.addAll(altRoute)
        }
        return allIds
    }

    /**
     * Gets route summary text
     */
    fun getSummaryText(): String {
        val duration = if (estimatedDurationHours > 0) {
            "${String.format("%.1f", estimatedDurationHours)} hrs"
        } else {
            "No estimate"
        }

        val distance = if (estimatedDistanceMiles > 0) {
            "${String.format("%.1f", estimatedDistanceMiles)} mi"
        } else {
            "No estimate"
        }

        return "${getStopCount()} stops • $distance • $duration"
    }

    /**
     * Checks if route is exportable in given format
     */
    fun canExportAs(format: ExportFormat): Boolean {
        return exportFormats.contains(format)
    }

    /**
     * Activates this route (sets isActive = true)
     */
    fun activate(): Route = copy(
        isActive = true,
        updatedAt = Clock.System.now().toString(),
    )

    /**
     * Deactivates this route (sets isActive = false)
     */
    fun deactivate(): Route = copy(
        isActive = false,
        updatedAt = Clock.System.now().toString(),
    )

    /**
     * Adds an alternative route option
     */
    fun addAlternative(name: String, bonusPointIds: List<String>): Route {
        val updatedAlternatives = alternativeRoutes + (name to bonusPointIds)
        return copy(
            alternativeRoutes = updatedAlternatives,
            updatedAt = Clock.System.now().toString(),
        )
    }

    /**
     * Removes an alternative route option
     */
    fun removeAlternative(name: String): Route {
        val updatedAlternatives = alternativeRoutes - name
        return copy(
            alternativeRoutes = updatedAlternatives,
            updatedAt = Clock.System.now().toString(),
        )
    }

    /**
     * Updates route timing estimates
     */
    fun updateEstimates(durationHours: Double, distanceMiles: Double): Route = copy(
        estimatedDurationHours = durationHours,
        estimatedDistanceMiles = distanceMiles,
        updatedAt = Clock.System.now().toString(),
    )

    /**
     * Reorders bonus points in the route
     */
    fun reorderBonusPoints(newOrder: List<String>): Route {
        // Validate that newOrder contains same points as current route
        if (newOrder.toSet() == plannedBonusPoints.toSet()) {
            return copy(
                plannedBonusPoints = newOrder,
                updatedAt = Clock.System.now().toString(),
            )
        }
        return this // Return unchanged if invalid reorder
    }

    companion object {
        /**
         * Creates a new route with validation
         */
        fun create(
            rallyRegistrationId: String,
            name: String,
            plannedBonusPoints: List<String>,
            estimatedDurationHours: Double = 0.0,
            estimatedDistanceMiles: Double = 0.0,
        ): Route? {
            val route = Route(
                rallyRegistrationId = rallyRegistrationId,
                name = name,
                plannedBonusPoints = plannedBonusPoints,
                estimatedDurationHours = estimatedDurationHours,
                estimatedDistanceMiles = estimatedDistanceMiles,
            )

            return if (route.isValid()) route else null
        }

        /**
         * Creates a route with alternatives
         */
        fun createWithAlternatives(
            rallyRegistrationId: String,
            name: String,
            plannedBonusPoints: List<String>,
            alternativeRoutes: Map<String, List<String>>,
            estimatedDurationHours: Double = 0.0,
            estimatedDistanceMiles: Double = 0.0,
        ): Route? {
            val route = Route(
                rallyRegistrationId = rallyRegistrationId,
                name = name,
                plannedBonusPoints = plannedBonusPoints,
                alternativeRoutes = alternativeRoutes,
                estimatedDurationHours = estimatedDurationHours,
                estimatedDistanceMiles = estimatedDistanceMiles,
            )

            return if (route.isValid()) route else null
        }
    }
}
