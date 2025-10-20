package org.showpage.rallydesktop.service

import org.slf4j.LoggerFactory
import java.util.prefs.Preferences

/**
 * Service for managing application preferences using java.util.prefs.Preferences.
 * Handles non-sensitive configuration data like server URL and user email.
 * Passwords are stored separately using the CredentialService.
 */
class PreferencesService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val prefs: Preferences = Preferences.userNodeForPackage(PreferencesService::class.java)

    companion object {
        private const val KEY_SERVER_URL = "serverUrl"
        private const val KEY_EMAIL = "email"
        private const val DEFAULT_SERVER_URL = "http://localhost:8080"
    }

    /**
     * Get the RallyServer URL.
     * Defaults to localhost:8080 for development.
     */
    fun getServerUrl(): String {
        return prefs.get(KEY_SERVER_URL, DEFAULT_SERVER_URL)
    }

    /**
     * Set the RallyServer URL.
     */
    fun setServerUrl(url: String) {
        logger.info("Setting server URL: {}", url)
        prefs.put(KEY_SERVER_URL, url)
        prefs.flush()
    }

    /**
     * Get the stored email address.
     */
    fun getEmail(): String? {
        return prefs.get(KEY_EMAIL, null)
    }

    /**
     * Set the email address.
     */
    fun setEmail(email: String?) {
        if (email != null) {
            prefs.put(KEY_EMAIL, email)
        } else {
            prefs.remove(KEY_EMAIL)
        }
        prefs.flush()
    }

    /**
     * Clear all preferences (useful for logout).
     */
    fun clear() {
        logger.info("Clearing all preferences")
        prefs.clear()
        prefs.flush()
    }

    /**
     * Check if login credentials are stored.
     */
    fun hasStoredCredentials(): Boolean {
        return getEmail() != null
    }
}
