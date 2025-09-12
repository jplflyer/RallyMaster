package com.rallymaster.model

import com.benasher44.uuid.uuid4
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * RallyRegistration data model for local JSON storage.
 *
 * Represents a rider registration for a specific rally with assigned rider number.
 */
@Serializable
data class RallyRegistration(
    val id: String = uuid4().toString(),
    val rallyId: String,
    val riderId: String, // User reference (with RIDER role)
    val riderNumber: Int, // Assigned sequential number within rally
    val registrationDate: String = Clock.System.now().toString(),
    val status: RegistrationStatus = RegistrationStatus.REGISTERED,

    // Registration details
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val motorcycleMake: String = "",
    val motorcycleModel: String = "",
    val motorcycleYear: String = "",
    val licensePlate: String = "",

    // Rally-specific information
    val tshirtSize: String = "",
    val dietaryRestrictions: String = "",
    val specialNeeds: String = "",
    val notes: String = "",

    // Tracking
    val checkInTime: String? = null, // When rider checked in at rally start
    val checkOutTime: String? = null, // When rider finished/checked out
    val updatedAt: String = Clock.System.now().toString(),
) {

    /**
     * Creates a copy with updated timestamp
     */
    fun withUpdatedTimestamp(): RallyRegistration = copy(
        updatedAt = Clock.System.now().toString(),
    )

    /**
     * Validates registration information
     */
    fun isValid(): Boolean {
        return rallyId.isNotBlank() &&
            riderId.isNotBlank() &&
            riderNumber > 0 &&
            isValidRegistrationDate()
    }

    /**
     * Validates registration date format
     */
    private fun isValidRegistrationDate(): Boolean {
        return try {
            Instant.parse(registrationDate)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if registration is active (can participate)
     */
    fun isActive(): Boolean {
        return status == RegistrationStatus.REGISTERED
    }

    /**
     * Checks if rider has finished the rally
     */
    fun isFinished(): Boolean {
        return status == RegistrationStatus.FINISHED
    }

    /**
     * Checks if rider did not finish
     */
    fun isDidNotFinish(): Boolean {
        return status == RegistrationStatus.DNF
    }

    /**
     * Checks if registration was cancelled
     */
    fun isCancelled(): Boolean {
        return status == RegistrationStatus.CANCELLED
    }

    /**
     * Gets status display text
     */
    fun getStatusText(): String {
        return when (status) {
            RegistrationStatus.REGISTERED -> "Registered"
            RegistrationStatus.CANCELLED -> "Cancelled"
            RegistrationStatus.DNF -> "Did Not Finish"
            RegistrationStatus.FINISHED -> "Finished"
        }
    }

    /**
     * Gets motorcycle display text
     */
    fun getMotorcycleText(): String {
        val parts = listOfNotNull(
            motorcycleYear.takeIf { it.isNotBlank() },
            motorcycleMake.takeIf { it.isNotBlank() },
            motorcycleModel.takeIf { it.isNotBlank() },
        )
        return parts.joinToString(" ")
    }

    /**
     * Checks if emergency contact information is complete
     */
    fun hasCompleteEmergencyContact(): Boolean {
        return emergencyContactName.isNotBlank() && emergencyContactPhone.isNotBlank()
    }

    /**
     * Checks if motorcycle information is complete
     */
    fun hasCompleteMotorcycleInfo(): Boolean {
        return motorcycleMake.isNotBlank() &&
            motorcycleModel.isNotBlank() &&
            licensePlate.isNotBlank()
    }

    /**
     * Marks rider as checked in
     */
    fun checkIn(): RallyRegistration = copy(
        checkInTime = Clock.System.now().toString(),
        updatedAt = Clock.System.now().toString(),
    )

    /**
     * Marks rider as finished
     */
    fun finish(): RallyRegistration = copy(
        status = RegistrationStatus.FINISHED,
        checkOutTime = Clock.System.now().toString(),
        updatedAt = Clock.System.now().toString(),
    )

    /**
     * Marks rider as DNF (Did Not Finish)
     */
    fun markDidNotFinish(): RallyRegistration = copy(
        status = RegistrationStatus.DNF,
        checkOutTime = Clock.System.now().toString(),
        updatedAt = Clock.System.now().toString(),
    )

    /**
     * Cancels registration
     */
    fun cancel(): RallyRegistration = copy(
        status = RegistrationStatus.CANCELLED,
        updatedAt = Clock.System.now().toString(),
    )

    /**
     * Gets rally duration if both check-in and check-out times exist
     */
    fun getRallyDurationHours(): Double? {
        return try {
            val checkIn = checkInTime?.let { Instant.parse(it) }
            val checkOut = checkOutTime?.let { Instant.parse(it) }

            if (checkIn != null && checkOut != null) {
                (checkOut.epochSeconds - checkIn.epochSeconds) / 3600.0
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /**
         * Creates a new registration with validation
         */
        fun create(
            rallyId: String,
            riderId: String,
            riderNumber: Int,
            emergencyContactName: String = "",
            emergencyContactPhone: String = "",
            motorcycleMake: String = "",
            motorcycleModel: String = "",
            licensePlate: String = "",
        ): RallyRegistration? {
            val registration = RallyRegistration(
                rallyId = rallyId,
                riderId = riderId,
                riderNumber = riderNumber,
                emergencyContactName = emergencyContactName,
                emergencyContactPhone = emergencyContactPhone,
                motorcycleMake = motorcycleMake,
                motorcycleModel = motorcycleModel,
                licensePlate = licensePlate,
            )

            return if (registration.isValid()) registration else null
        }
    }
}
