package com.rallymaster.model

import com.benasher44.uuid.uuid4
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

/**
 * Rally data model for local JSON storage.
 *
 * Represents a motorcycle rally event with all configuration and metadata
 * for Rally Masters to manage bonus points, combinations, and participant registration.
 */
@Serializable
data class Rally(
    val id: String = uuid4().toString(),
    val name: String,
    val description: String = "",

    // Date/Time (stored as ISO 8601 strings for JSON compatibility)
    val startDate: String, // LocalDateTime as ISO string
    val endDate: String, // LocalDateTime as ISO string

    // Location and organization
    val location: String = "", // Base location (e.g., "Brainerd, Minnesota")
    val createdBy: String = "", // Rally Master user ID (empty for local-only)

    // Storage and status
    val storageMode: StorageMode = StorageMode.LOCAL,
    val status: RallyStatus = RallyStatus.DRAFT,

    // Timestamps
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String = Clock.System.now().toString(),

    // Rally Master information (for local storage)
    val organizerName: String = "",
    val organizerEmail: String = "",
    val organizerPhone: String = "",

    // Rally configuration
    val registrationFee: Double = 0.0,
    val maxParticipants: Int? = null,
    val rules: String = "",

    // Related entities (references for local JSON storage)
    val bonusPointIds: List<String> = emptyList(),
    val combinationIds: List<String> = emptyList(),
    val registrationIds: List<String> = emptyList(), // For future use
) {

    /**
     * Creates a copy with updated timestamp
     */
    fun withUpdatedTimestamp(): Rally = copy(
        updatedAt = Clock.System.now().toString(),
    )

    /**
     * Validates that the rally has required information
     */
    fun isValid(): Boolean {
        return name.isNotBlank() &&
            startDate.isNotBlank() &&
            endDate.isNotBlank() &&
            organizerName.isNotBlank() &&
            location.isNotBlank() &&
            isValidDateRange()
    }

    /**
     * Validates that end date is after start date
     */
    private fun isValidDateRange(): Boolean {
        return try {
            val start = Instant.parse(startDate)
            val end = Instant.parse(endDate)
            end > start
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if the rally can be modified (not published or later)
     */
    fun canBeModified(): Boolean {
        return status == RallyStatus.DRAFT
    }

    /**
     * Gets the rally duration in days
     */
    fun getDurationDays(): Int? {
        return try {
            val start = Instant.parse(startDate).toLocalDateTime(TimeZone.UTC)
            val end = Instant.parse(endDate).toLocalDateTime(TimeZone.UTC)
            (end.date.toEpochDays() - start.date.toEpochDays())
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if the rally is currently active (between start and end dates)
     */
    fun isCurrentlyActive(): Boolean {
        return try {
            val now = Clock.System.now()
            val start = Instant.parse(startDate)
            val end = Instant.parse(endDate)
            now >= start && now <= end && status == RallyStatus.ACTIVE
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets display name with location
     */
    fun getDisplayName(): String {
        return if (location.isNotEmpty()) {
            "$name - $location"
        } else {
            name
        }
    }

    /**
     * Creates start/end LocalDateTime objects for date manipulation
     */
    fun getStartDateTime(): LocalDateTime? {
        return try {
            Instant.parse(startDate).toLocalDateTime(TimeZone.UTC)
        } catch (e: Exception) {
            null
        }
    }

    fun getEndDateTime(): LocalDateTime? {
        return try {
            Instant.parse(endDate).toLocalDateTime(TimeZone.UTC)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /**
         * Creates a new rally with properly formatted datetime strings
         */
        fun create(
            name: String,
            description: String = "",
            startDateTime: LocalDateTime,
            endDateTime: LocalDateTime,
            location: String = "",
            organizerName: String = "",
            organizerEmail: String = "",
            organizerPhone: String = "",
        ): Rally {
            return Rally(
                name = name,
                description = description,
                startDate = startDateTime.toInstant(TimeZone.UTC).toString(),
                endDate = endDateTime.toInstant(TimeZone.UTC).toString(),
                location = location,
                organizerName = organizerName,
                organizerEmail = organizerEmail,
                organizerPhone = organizerPhone,
            )
        }
    }
}
