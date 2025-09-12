package com.rallymaster.model

import kotlinx.serialization.Serializable

/**
 * Storage mode for rally data
 */
@Serializable
enum class StorageMode {
    LOCAL, // Rally data stored locally only
    SERVER, // Rally data stored on server with local backup
}

/**
 * Rally status enumeration for tracking rally lifecycle
 */
@Serializable
enum class RallyStatus {
    DRAFT, // Rally being created, not visible to riders
    PUBLISHED, // Rally published, riders can register
    ACTIVE, // Rally is currently running
    COMPLETED, // Rally finished, scoring in progress
    CANCELLED, // Rally was cancelled
}

/**
 * Bonus point categories for organization and scoring
 */
@Serializable
enum class BonusPointCategory {
    STANDARD, // Regular bonus points
    BONUS, // Higher value bonus points
    SPECIAL, // Special category with unique rules
    TIMED, // Time-restricted bonus points
    MYSTERY, // Mystery bonus revealed during rally
    CHECKPOINT, // Mandatory checkpoint (usually 0 points)
}

/**
 * Verification methods for bonus points
 */
@Serializable
enum class VerificationMethod {
    PHOTO, // Photo required at location
    RECEIPT, // Receipt from business required
    CODE, // Special code to be entered
    QUESTION, // Answer specific question
    GPS_ONLY, // GPS coordinates sufficient
    SELFIE, // Selfie with landmark required
    COMBO, // Multiple verification methods
}

/**
 * User roles for role-based access control
 */
@Serializable
enum class UserRole {
    RALLY_MASTER, // Can create and manage rallies
    RIDER, // Can register for rallies and submit routes
    SCORER, // Can score rider submissions
}

/**
 * Rally registration status for participants
 */
@Serializable
enum class RegistrationStatus {
    REGISTERED, // Active registration
    CANCELLED, // Rider cancelled registration
    DNF, // Did not finish rally
    FINISHED, // Completed rally
}

/**
 * Export formats for route planning
 */
@Serializable
enum class ExportFormat {
    GPX, // GPS Exchange format for Garmin devices
    KML, // Google Earth format
    KMZ, // Compressed KML
    CSV, // Comma-separated values for printing
    JSON, // Machine-readable format
}

/**
 * File storage types for local JSON management
 */
@Serializable
enum class StorageType {
    RALLY, // Rally definition files
    BONUS_POINTS, // Bonus points for a rally
    COMBINATIONS, // Combinations for a rally
    ROUTES, // Planned routes
    RESULTS, // Rally results and scoring
    BACKUP, // Backup files
    REGISTRATIONS, // Rally registrations
    USERS, // User data (for local storage)
}
