package com.rallymaster.data

import com.rallymaster.model.StorageType

/**
 * Abstract file manager for local JSON rally storage.
 *
 * Defines the interface for file operations that will be implemented
 * differently on each platform (Desktop, Android, iOS).
 * Desktop implementation will use standard file I/O.
 */
abstract class FileManager {

    /**
     * Gets the base directory for rally data storage
     */
    abstract suspend fun getBaseDirectory(): String

    /**
     * Creates directory structure if it doesn't exist
     */
    abstract suspend fun ensureDirectoryExists(path: String): Boolean

    /**
     * Writes content to file
     */
    abstract suspend fun writeFile(filePath: String, content: String): Boolean

    /**
     * Reads content from file
     */
    abstract suspend fun readFile(filePath: String): String?

    /**
     * Checks if file exists
     */
    abstract suspend fun fileExists(filePath: String): Boolean

    /**
     * Lists files in directory
     */
    abstract suspend fun listFiles(directoryPath: String): List<String>

    /**
     * Deletes file
     */
    abstract suspend fun deleteFile(filePath: String): Boolean

    /**
     * Creates backup of file
     */
    abstract suspend fun backupFile(filePath: String): String?

    /**
     * Gets file path for specific rally and storage type
     */
    fun getFilePath(rallyId: String, storageType: StorageType, fileName: String = ""): String {
        return when (storageType) {
            StorageType.RALLY -> "rallies/$rallyId/rally.json"
            StorageType.BONUS_POINTS -> "rallies/$rallyId/bonus-points.json"
            StorageType.COMBINATIONS -> "rallies/$rallyId/combinations.json"
            StorageType.ROUTES -> "rallies/$rallyId/routes.json"
            StorageType.RESULTS -> "rallies/$rallyId/results.json"
            StorageType.REGISTRATIONS -> "rallies/$rallyId/registrations.json"
            StorageType.USERS -> "users/users.json" // Global user storage
            StorageType.BACKUP -> "backups/$rallyId/${fileName.ifEmpty { "backup-${kotlinx.datetime.Clock.System.now()}.json" }}"
        }
    }

    /**
     * Gets directory path for rally data
     */
    fun getRallyDirectory(rallyId: String): String {
        return "rallies/$rallyId"
    }

    /**
     * Creates the standard directory structure for a rally
     */
    suspend fun createRallyStructure(rallyId: String): Boolean {
        val rallyDir = getRallyDirectory(rallyId)
        return ensureDirectoryExists(rallyDir) &&
            ensureDirectoryExists("backups/$rallyId")
    }
}
