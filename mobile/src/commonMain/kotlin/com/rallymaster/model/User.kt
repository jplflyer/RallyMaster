package com.rallymaster.model

import com.benasher44.uuid.uuid4
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * User data model for local JSON storage.
 *
 * System user with role-based access (Rally Master, Rider, Scorer).
 * For desktop-first approach, this represents local user profiles.
 */
@Serializable
data class User(
    val id: String = uuid4().toString(),
    val username: String,
    val email: String,
    val passwordHash: String = "", // For future server integration
    val firstName: String,
    val lastName: String,
    val roles: Set<UserRole> = setOf(UserRole.RALLY_MASTER),
    val isActive: Boolean = true,
    val createdAt: String = Clock.System.now().toString(),
    val lastLogin: String? = null,

    // Additional profile information
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val country: String = "USA",

    // Rally-specific preferences
    val preferredUnits: String = "IMPERIAL", // IMPERIAL or METRIC
    val timezone: String = "America/Chicago", // Default timezone
    val notes: String = "",
) {

    /**
     * Gets full display name
     */
    fun getFullName(): String {
        return "$firstName $lastName".trim()
    }

    /**
     * Gets display name with username fallback
     */
    fun getDisplayName(): String {
        val fullName = getFullName()
        return if (fullName.isNotBlank()) fullName else username
    }

    /**
     * Checks if user has specific role
     */
    fun hasRole(role: UserRole): Boolean {
        return roles.contains(role)
    }

    /**
     * Checks if user can create rallies
     */
    fun canCreateRallies(): Boolean {
        return hasRole(UserRole.RALLY_MASTER)
    }

    /**
     * Checks if user can register for rallies
     */
    fun canRegisterForRallies(): Boolean {
        return hasRole(UserRole.RIDER)
    }

    /**
     * Checks if user can score rallies
     */
    fun canScoreRallies(): Boolean {
        return hasRole(UserRole.SCORER)
    }

    /**
     * Gets role display text
     */
    fun getRoleText(): String {
        return when {
            roles.size == 1 -> roles.first().name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
            roles.contains(UserRole.RALLY_MASTER) -> "Rally Master"
            roles.contains(UserRole.SCORER) -> "Scorer"
            roles.contains(UserRole.RIDER) -> "Rider"
            else -> "User"
        }
    }

    /**
     * Creates updated copy with new login timestamp
     */
    fun withLogin(): User = copy(
        lastLogin = Clock.System.now().toString(),
    )

    /**
     * Validates user information
     */
    fun isValid(): Boolean {
        return username.isNotBlank() &&
            username.length in 3..30 &&
            username.all { it.isLetterOrDigit() || it == '_' } &&
            email.isNotBlank() &&
            isValidEmail(email) &&
            firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            roles.isNotEmpty()
    }

    /**
     * Basic email format validation
     */
    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") &&
            email.split("@").size == 2 &&
            email.split("@")[1].contains(".")
    }

    /**
     * Gets initials for display
     */
    fun getInitials(): String {
        val first = firstName.firstOrNull()?.uppercase() ?: ""
        val last = lastName.firstOrNull()?.uppercase() ?: ""
        return "$first$last"
    }

    companion object {
        /**
         * Creates a new Rally Master user
         */
        fun createRallyMaster(
            username: String,
            email: String,
            firstName: String,
            lastName: String,
            phone: String = "",
        ): User? {
            val user = User(
                username = username,
                email = email,
                firstName = firstName,
                lastName = lastName,
                phone = phone,
                roles = setOf(UserRole.RALLY_MASTER),
            )

            return if (user.isValid()) user else null
        }

        /**
         * Creates a new Rider user
         */
        fun createRider(
            username: String,
            email: String,
            firstName: String,
            lastName: String,
        ): User? {
            val user = User(
                username = username,
                email = email,
                firstName = firstName,
                lastName = lastName,
                roles = setOf(UserRole.RIDER),
            )

            return if (user.isValid()) user else null
        }

        /**
         * Creates a multi-role user
         */
        fun createMultiRole(
            username: String,
            email: String,
            firstName: String,
            lastName: String,
            roles: Set<UserRole>,
        ): User? {
            val user = User(
                username = username,
                email = email,
                firstName = firstName,
                lastName = lastName,
                roles = roles,
            )

            return if (user.isValid()) user else null
        }
    }
}
