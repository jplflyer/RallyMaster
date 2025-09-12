package com.rallymaster.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Desktop-specific file manager implementation for local JSON rally storage.
 *
 * Uses standard Java File I/O operations to manage rally data in the user's
 * home directory under "RallyMaster" folder.
 */
class DesktopFileManager : FileManager() {

    companion object {
        private const val APP_DIRECTORY = "RallyMaster"
    }

    /**
     * Gets the base directory in user's home folder
     */
    override suspend fun getBaseDirectory(): String = withContext(Dispatchers.IO) {
        val homeDir = System.getProperty("user.home")
        val rallyMasterDir = File(homeDir, APP_DIRECTORY)

        if (!rallyMasterDir.exists()) {
            rallyMasterDir.mkdirs()
        }

        rallyMasterDir.absolutePath
    }

    /**
     * Creates directory structure if it doesn't exist
     */
    override suspend fun ensureDirectoryExists(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseDir = getBaseDirectory()
            val fullPath = File(baseDir, path)
            fullPath.mkdirs()
        } catch (e: IOException) {
            println("Failed to create directory: $path - ${e.message}")
            false
        }
    }

    /**
     * Writes content to file atomically
     */
    override suspend fun writeFile(filePath: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseDir = getBaseDirectory()
            val file = File(baseDir, filePath)

            // Ensure parent directory exists
            file.parentFile?.mkdirs()

            // Write to temporary file first, then rename (atomic operation)
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            tempFile.writeText(content)

            // Atomic rename
            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (e: IOException) {
            println("Failed to write file: $filePath - ${e.message}")
            false
        }
    }

    /**
     * Reads content from file
     */
    override suspend fun readFile(filePath: String): String? = withContext(Dispatchers.IO) {
        try {
            val baseDir = getBaseDirectory()
            val file = File(baseDir, filePath)

            if (file.exists() && file.isFile) {
                file.readText()
            } else {
                null
            }
        } catch (e: IOException) {
            println("Failed to read file: $filePath - ${e.message}")
            null
        }
    }

    /**
     * Checks if file exists
     */
    override suspend fun fileExists(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseDir = getBaseDirectory()
            val file = File(baseDir, filePath)
            file.exists() && file.isFile
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Lists files in directory
     */
    override suspend fun listFiles(directoryPath: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val baseDir = getBaseDirectory()
            val directory = File(baseDir, directoryPath)

            if (directory.exists() && directory.isDirectory) {
                directory.listFiles()
                    ?.filter { it.isFile }
                    ?.map { it.name }
                    ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("Failed to list files in: $directoryPath - ${e.message}")
            emptyList()
        }
    }

    /**
     * Deletes file
     */
    override suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseDir = getBaseDirectory()
            val file = File(baseDir, filePath)

            if (file.exists() && file.isFile) {
                file.delete()
            } else {
                true // File doesn't exist, consider it deleted
            }
        } catch (e: Exception) {
            println("Failed to delete file: $filePath - ${e.message}")
            false
        }
    }

    /**
     * Creates backup of file with timestamp
     */
    override suspend fun backupFile(filePath: String): String? = withContext(Dispatchers.IO) {
        try {
            val baseDir = getBaseDirectory()
            val originalFile = File(baseDir, filePath)

            if (!originalFile.exists()) {
                return@withContext null
            }

            val timestamp = kotlinx.datetime.Clock.System.now().toString()
                .replace(":", "-")
                .replace(".", "-")

            val backupDir = File(baseDir, "backups")
            backupDir.mkdirs()

            val backupFile = File(backupDir, "${originalFile.nameWithoutExtension}-$timestamp.${originalFile.extension}")

            Files.copy(originalFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

            backupFile.absolutePath
        } catch (e: Exception) {
            println("Failed to backup file: $filePath - ${e.message}")
            null
        }
    }
}
