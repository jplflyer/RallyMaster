package com.rallymaster.model

import com.benasher44.uuid.uuid4
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * BonusPoint data model for local JSON storage.
 *
 * Represents a single bonus point location in a motorcycle rally with GPS coordinates,
 * point values, and verification requirements.
 */
@Serializable
data class BonusPoint(
    val id: String = uuid4().toString(),
    val rallyId: String,
    val name: String,
    val abbreviation: String, // Short code like "BP01" or "MIN01" (max 6 chars)
    val description: String = "",

    // GPS coordinates (stored as doubles, no PostGIS complexity)
    val latitude: Double, // -90.0 to 90.0 (WGS84)
    val longitude: Double, // -180.0 to 180.0 (WGS84)

    // Point value and categorization
    val points: Int, // Point value (10-75 typical range)
    val category: BonusPointCategory = BonusPointCategory.STANDARD,

    // Location details
    val address: String = "", // Optional street address
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val country: String = "USA",

    // Verification requirements
    val verificationMethod: VerificationMethod = VerificationMethod.PHOTO,
    val verificationCode: String = "", // Optional verification code
    val question: String = "", // Optional question to answer
    val answer: String = "", // Expected answer
    val timeWindow: String = "", // Optional time restrictions like "9AM-5PM"

    // Administrative
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String = Clock.System.now().toString(),
) {

    /**
     * Validates GPS coordinates are within valid ranges
     */
    fun hasValidCoordinates(): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    /**
     * Creates a copy with updated timestamp
     */
    fun withUpdatedTimestamp(): BonusPoint = copy(
        updatedAt = Clock.System.now().toString(),
    )

    /**
     * Validates that bonus point has required information
     */
    fun isValid(): Boolean {
        return name.isNotBlank() &&
            abbreviation.isNotBlank() &&
            abbreviation.length <= 6 &&
            points > 0 &&
            hasValidCoordinates() &&
            rallyId.isNotBlank() &&
            isValidVerification()
    }

    /**
     * Validates verification requirements are consistent
     */
    private fun isValidVerification(): Boolean {
        return when (verificationMethod) {
            VerificationMethod.CODE -> verificationCode.isNotBlank()
            VerificationMethod.QUESTION -> question.isNotBlank() && answer.isNotBlank()
            VerificationMethod.COMBO -> verificationCode.isNotBlank() || (question.isNotBlank() && answer.isNotBlank())
            else -> true // PHOTO, RECEIPT, GPS_ONLY, SELFIE don't require additional fields
        }
    }

    /**
     * Calculates distance to another bonus point using Haversine formula
     * Returns distance in miles
     */
    fun distanceTo(other: BonusPoint): Double {
        return calculateDistance(latitude, longitude, other.latitude, other.longitude)
    }

    /**
     * Calculates distance to GPS coordinates using Haversine formula
     * Returns distance in miles
     */
    fun distanceTo(lat: Double, lon: Double): Double {
        return calculateDistance(latitude, longitude, lat, lon)
    }

    /**
     * Gets display name with abbreviation
     */
    fun getDisplayName(): String {
        return "$abbreviation - $name"
    }

    /**
     * Gets full location string
     */
    fun getFullLocation(): String {
        val parts = listOfNotNull(
            address.takeIf { it.isNotBlank() },
            city.takeIf { it.isNotBlank() },
            state.takeIf { it.isNotBlank() },
            zipCode.takeIf { it.isNotBlank() },
        )
        return parts.joinToString(", ")
    }

    /**
     * Gets coordinates as a formatted string
     */
    fun getCoordinatesString(): String {
        return "${String.format("%.6f", latitude)}, ${String.format("%.6f", longitude)}"
    }

    /**
     * Checks if this bonus point has time restrictions
     */
    fun hasTimeRestrictions(): Boolean {
        return timeWindow.isNotBlank()
    }

    /**
     * Checks if verification requires user input
     */
    fun requiresUserInput(): Boolean {
        return verificationMethod in listOf(
            VerificationMethod.CODE,
            VerificationMethod.QUESTION,
            VerificationMethod.COMBO,
        )
    }

    companion object {
        /**
         * Earth radius in miles for distance calculations
         */
        private const val EARTH_RADIUS_MILES = 3959.0

        /**
         * Calculates distance between two GPS points using Haversine formula
         * Returns distance in miles
         */
        private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return EARTH_RADIUS_MILES * c
        }

        /**
         * Creates a new bonus point with validation
         */
        fun create(
            rallyId: String,
            name: String,
            abbreviation: String,
            latitude: Double,
            longitude: Double,
            points: Int,
            description: String = "",
            category: BonusPointCategory = BonusPointCategory.STANDARD,
            verificationMethod: VerificationMethod = VerificationMethod.PHOTO,
        ): BonusPoint? {
            val bonusPoint = BonusPoint(
                rallyId = rallyId,
                name = name,
                abbreviation = abbreviation,
                latitude = latitude,
                longitude = longitude,
                points = points,
                description = description,
                category = category,
                verificationMethod = verificationMethod,
            )

            return if (bonusPoint.isValid()) bonusPoint else null
        }
    }
}
