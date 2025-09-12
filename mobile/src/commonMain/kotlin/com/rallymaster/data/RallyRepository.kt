package com.rallymaster.data

import com.rallymaster.model.BonusPoint
import com.rallymaster.model.Combination
import com.rallymaster.model.Rally
import com.rallymaster.model.StorageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Rally repository for local JSON storage operations.
 *
 * Provides a clean API for Rally Master desktop application to manage
 * rallies, bonus points, and combinations using local JSON files.
 * Implements offline-first approach with reactive state management.
 */
class RallyRepository(
    private val fileManager: FileManager,
    private val jsonStorage: JsonStorageService = JsonStorageService(),
) {

    // State management for reactive UI
    private val _rallies = MutableStateFlow<List<Rally>>(emptyList())
    val rallies: StateFlow<List<Rally>> = _rallies

    private val _currentRally = MutableStateFlow<Rally?>(null)
    val currentRally: StateFlow<Rally?> = _currentRally

    private val _bonusPoints = MutableStateFlow<List<BonusPoint>>(emptyList())
    val bonusPoints: StateFlow<List<BonusPoint>> = _bonusPoints

    private val _combinations = MutableStateFlow<List<Combination>>(emptyList())
    val combinations: StateFlow<List<Combination>> = _combinations

    /**
     * Initializes repository and loads existing rallies
     */
    suspend fun initialize() {
        loadAllRallies()
    }

    // Rally Operations

    /**
     * Saves rally to local JSON storage
     */
    suspend fun saveRally(rally: Rally): Result<Rally> {
        return try {
            // Ensure directory structure exists
            fileManager.createRallyStructure(rally.id)

            // Update timestamp
            val updatedRally = rally.withUpdatedTimestamp()

            // Save to JSON file
            val filePath = fileManager.getFilePath(rally.id, StorageType.RALLY)
            val jsonContent = jsonStorage.encode(updatedRally)

            val success = fileManager.writeFile(filePath, jsonContent)

            if (success) {
                updateRallyInState(updatedRally)
                Result.success(updatedRally)
            } else {
                Result.failure(Exception("Failed to write rally file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads rally by ID from local storage
     */
    suspend fun loadRally(rallyId: String): Result<Rally> {
        return try {
            val filePath = fileManager.getFilePath(rallyId, StorageType.RALLY)
            val jsonContent = fileManager.readFile(filePath)

            if (jsonContent != null) {
                val rally = jsonStorage.decode<Rally>(jsonContent)
                _currentRally.value = rally
                Result.success(rally)
            } else {
                Result.failure(Exception("Rally file not found: $rallyId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads all rallies from local storage
     */
    private suspend fun loadAllRallies() {
        try {
            val ralliesDir = "rallies"
            fileManager.ensureDirectoryExists(ralliesDir)

            val rallyDirectories = fileManager.listFiles(ralliesDir)
            val loadedRallies = mutableListOf<Rally>()

            for (rallyDir in rallyDirectories) {
                val filePath = "$ralliesDir/$rallyDir/rally.json"
                val jsonContent = fileManager.readFile(filePath)

                jsonContent?.let {
                    jsonStorage.safeDecode<Rally>(it)?.let { rally ->
                        loadedRallies.add(rally)
                    }
                }
            }

            _rallies.value = loadedRallies.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            println("Error loading rallies: ${e.message}")
        }
    }

    /**
     * Deletes rally and all associated data
     */
    suspend fun deleteRally(rallyId: String): Result<Unit> {
        return try {
            val rallyPath = fileManager.getFilePath(rallyId, StorageType.RALLY)
            val bonusPointsPath = fileManager.getFilePath(rallyId, StorageType.BONUS_POINTS)
            val combinationsPath = fileManager.getFilePath(rallyId, StorageType.COMBINATIONS)

            // Create backup before deletion
            fileManager.backupFile(rallyPath)

            // Delete files
            fileManager.deleteFile(rallyPath)
            fileManager.deleteFile(bonusPointsPath)
            fileManager.deleteFile(combinationsPath)

            // Update state
            removeRallyFromState(rallyId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Bonus Points Operations

    /**
     * Saves bonus points for a rally
     */
    suspend fun saveBonusPoints(rallyId: String, bonusPoints: List<BonusPoint>): Result<Unit> {
        return try {
            val filePath = fileManager.getFilePath(rallyId, StorageType.BONUS_POINTS)
            val jsonContent = jsonStorage.encode(bonusPoints)

            val success = fileManager.writeFile(filePath, jsonContent)

            if (success) {
                _bonusPoints.value = bonusPoints
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to write bonus points file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads bonus points for a rally
     */
    suspend fun loadBonusPoints(rallyId: String): Result<List<BonusPoint>> {
        return try {
            val filePath = fileManager.getFilePath(rallyId, StorageType.BONUS_POINTS)
            val jsonContent = fileManager.readFile(filePath)

            if (jsonContent != null) {
                val bonusPoints = jsonStorage.decode<List<BonusPoint>>(jsonContent)
                _bonusPoints.value = bonusPoints
                Result.success(bonusPoints)
            } else {
                // No bonus points file exists yet, return empty list
                _bonusPoints.value = emptyList()
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Combinations Operations

    /**
     * Saves combinations for a rally
     */
    suspend fun saveCombinations(rallyId: String, combinations: List<Combination>): Result<Unit> {
        return try {
            val filePath = fileManager.getFilePath(rallyId, StorageType.COMBINATIONS)
            val jsonContent = jsonStorage.encode(combinations)

            val success = fileManager.writeFile(filePath, jsonContent)

            if (success) {
                _combinations.value = combinations
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to write combinations file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads combinations for a rally
     */
    suspend fun loadCombinations(rallyId: String): Result<List<Combination>> {
        return try {
            val filePath = fileManager.getFilePath(rallyId, StorageType.COMBINATIONS)
            val jsonContent = fileManager.readFile(filePath)

            if (jsonContent != null) {
                val combinations = jsonStorage.decode<List<Combination>>(jsonContent)
                _combinations.value = combinations
                Result.success(combinations)
            } else {
                // No combinations file exists yet, return empty list
                _combinations.value = emptyList()
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // State Management Helpers

    private fun updateRallyInState(rally: Rally) {
        val currentList = _rallies.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == rally.id }

        if (existingIndex >= 0) {
            currentList[existingIndex] = rally
        } else {
            currentList.add(rally)
        }

        _rallies.value = currentList.sortedByDescending { it.createdAt }

        // Update current rally if it's the same
        if (_currentRally.value?.id == rally.id) {
            _currentRally.value = rally
        }
    }

    private fun removeRallyFromState(rallyId: String) {
        _rallies.value = _rallies.value.filter { it.id != rallyId }

        if (_currentRally.value?.id == rallyId) {
            _currentRally.value = null
        }
    }
}
