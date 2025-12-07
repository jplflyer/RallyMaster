package org.showpage.rallydesktop.service

import com.sun.jna.Platform
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Platform-independent interface for storing and retrieving credentials.
 * Uses macOS Keychain on Mac and Windows Credential Manager on Windows.
 */
interface CredentialService {
    /**
     * Store a password for the given service and account.
     */
    fun storePassword(service: String, account: String, password: String): Boolean

    /**
     * Retrieve a password for the given service and account.
     */
    fun getPassword(service: String, account: String): String?

    /**
     * Delete a password for the given service and account.
     */
    fun deletePassword(service: String, account: String): Boolean

    companion object {
        private const val SERVICE_NAME = "RallyMaster"

        /**
         * Factory method to get the appropriate credential service for the current platform.
         */
        fun create(): CredentialService {
            return when {
                Platform.isMac() -> MacOSKeychainService()
                Platform.isWindows() -> WindowsCredentialService()
                else -> throw UnsupportedOperationException("Credential storage not supported on this platform")
            }
        }
    }
}

/**
 * macOS Keychain implementation using the security command-line tool.
 */
class MacOSKeychainService : CredentialService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun storePassword(service: String, account: String, password: String): Boolean {
        return try {
            // First try to delete existing entry
            deletePassword(service, account)

            // Add new password
            val process = ProcessBuilder(
                "security",
                "add-generic-password",
                "-a", account,
                "-s", service,
                "-w", password,
                "-U"  // Update if exists
            ).start()

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                logger.info("Successfully stored password in Keychain for account: {}", account)
                true
            } else {
                logger.error("Failed to store password in Keychain. Exit code: {}", exitCode)
                false
            }
        } catch (e: Exception) {
            logger.error("Error storing password in Keychain", e)
            false
        }
    }

    override fun getPassword(service: String, account: String): String? {
        return try {
            val process = ProcessBuilder(
                "security",
                "find-generic-password",
                "-a", account,
                "-s", service,
                "-w"  // Output password only
            ).redirectErrorStream(true)
                .start()

            val password = BufferedReader(InputStreamReader(process.inputStream))
                .use { it.readText().trim() }

            val exitCode = process.waitFor()
            if (exitCode == 0 && password.isNotEmpty()) {
                logger.debug("Successfully retrieved password from Keychain for account: {}", account)
                password
            } else {
                logger.debug("No password found in Keychain for account: {}", account)
                null
            }
        } catch (e: Exception) {
            logger.error("Error retrieving password from Keychain", e)
            null
        }
    }

    override fun deletePassword(service: String, account: String): Boolean {
        return try {
            val process = ProcessBuilder(
                "security",
                "delete-generic-password",
                "-a", account,
                "-s", service
            ).start()

            process.waitFor()
            // Exit code 0 = deleted, 44 = not found (also success for our purposes)
            true
        } catch (e: Exception) {
            logger.debug("Error deleting password from Keychain (may not exist)", e)
            true  // Not an error if it doesn't exist
        }
    }
}

/**
 * Windows Credential Manager implementation using cmdkey command-line tool.
 */
class WindowsCredentialService : CredentialService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun storePassword(service: String, account: String, password: String): Boolean {
        return try {
            val target = "$service:$account"

            val process = ProcessBuilder(
                "cmdkey",
                "/generic:$target",
                "/user:$account",
                "/pass:$password"
            ).start()

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                logger.info("Successfully stored password in Credential Manager for account: {}", account)
                true
            } else {
                logger.error("Failed to store password in Credential Manager. Exit code: {}", exitCode)
                false
            }
        } catch (e: Exception) {
            logger.error("Error storing password in Credential Manager", e)
            false
        }
    }

    override fun getPassword(service: String, account: String): String? {
        // Windows doesn't provide a simple way to retrieve passwords via cmdkey
        // We'll need to use JNA to call Windows API directly
        logger.warn("Password retrieval not yet fully implemented for Windows")
        return null
    }

    override fun deletePassword(service: String, account: String): Boolean {
        return try {
            val target = "$service:$account"

            val process = ProcessBuilder(
                "cmdkey",
                "/delete:$target"
            ).start()

            process.waitFor()
            true
        } catch (e: Exception) {
            logger.debug("Error deleting password from Credential Manager (may not exist)", e)
            true
        }
    }
}
