package com.rallymaster.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * JSON serialization service for local file storage.
 *
 * Provides centralized JSON encoding/decoding with consistent formatting
 * for Rally Master desktop application's offline-first approach.
 */
class JsonStorageService {

    companion object {
        /**
         * JSON configuration for pretty-printed, stable output
         */
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    /**
     * Serializes any serializable object to JSON string
     */
    inline fun <reified T : @Serializable Any> encode(data: T): String {
        return json.encodeToString(data)
    }

    /**
     * Deserializes JSON string to specified type
     */
    inline fun <reified T : @Serializable Any> decode(jsonString: String): T {
        return json.decodeFromString(jsonString)
    }

    /**
     * Safely deserializes JSON string, returning null on failure
     */
    inline fun <reified T : @Serializable Any> safeDecode(jsonString: String): T? {
        return try {
            json.decodeFromString<T>(jsonString)
        } catch (e: Exception) {
            println("JSON decode error: ${e.message}")
            null
        }
    }

    /**
     * Validates JSON string format
     */
    fun isValidJson(jsonString: String): Boolean {
        return try {
            json.parseToJsonElement(jsonString)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Creates a backup wrapper for any data with metadata
     */
    @Serializable
    data class BackupWrapper<T>(
        val version: String = "1.0.0",
        val timestamp: String = kotlinx.datetime.Clock.System.now().toString(),
        val type: String,
        val data: T,
    )

    /**
     * Wraps data with backup metadata for safer storage
     */
    inline fun <reified T : @Serializable Any> createBackup(data: T, type: String): BackupWrapper<T> {
        return BackupWrapper(
            type = type,
            data = data,
        )
    }
}
