package com.rallymaster.model

import com.benasher44.uuid.uuid4
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Combination data model for local JSON storage.
 *
 * Represents a combination of bonus points that, when visited according to rules,
 * provides additional points beyond the individual bonus point values.
 * Based on data-model.md specification.
 */
@Serializable
data class Combination(
    val id: String = uuid4().toString(),
    val rallyId: String,
    val name: String, // Combination name (e.g., "Lakes Loop")
    val description: String = "", // Optional description

    // Scoring configuration
    val points: Int, // Bonus points for completion (200-1000 typical)
    val isRequired: Boolean = true, // Whether all bonus points required (vs. minimum count)
    val minimumCount: Int? = null, // Minimum bonus points needed (if not isRequired)

    // Bonus point associations
    val bonusPointIds: List<String>, // All bonus points in this combination

    // Sequence and timing constraints
    val sequenceRequired: Boolean = false, // Must be visited in specific order
    val sequenceOrder: List<String> = emptyList(), // Order of bonus points if sequence required
    val timeLimit: String = "", // Optional time limit like "4 hours" or "within same day"

    // Administrative
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String = Clock.System.now().toString(),
) {

    /**
     * Creates a copy with updated timestamp
     */
    fun withUpdatedTimestamp(): Combination = copy(
        updatedAt = Clock.System.now().toString(),
    )

    /**
     * Validates that combination has required information per data-model.md rules
     */
    fun isValid(): Boolean {
        return name.isNotBlank() &&
            rallyId.isNotBlank() &&
            bonusPointIds.size >= 2 && // Must contain at least 2 bonus points
            points > 0 &&
            isValidRequirementSettings() &&
            isValidSequenceSettings()
    }

    /**
     * Validates requirement settings are consistent
     */
    private fun isValidRequirementSettings(): Boolean {
        return if (isRequired) {
            minimumCount == null || minimumCount == bonusPointIds.size
        } else {
            minimumCount != null && minimumCount >= 2 && minimumCount <= bonusPointIds.size
        }
    }

    /**
     * Validates sequence settings are consistent
     */
    private fun isValidSequenceSettings(): Boolean {
        return if (sequenceRequired) {
            sequenceOrder.isNotEmpty() &&
                sequenceOrder.toSet() == bonusPointIds.toSet() // Same points, different order
        } else {
            true // No sequence validation needed
        }
    }

    /**
     * Checks if a list of visited bonus point IDs completes this combination
     */
    fun isCompletedBy(visitedBonusPointIds: List<String>): Boolean {
        val visitedInCombination = bonusPointIds.filter { it in visitedBonusPointIds }

        val requiredCount = if (isRequired) bonusPointIds.size else (minimumCount ?: bonusPointIds.size)
        val hasEnoughPoints = visitedInCombination.size >= requiredCount

        return if (sequenceRequired && hasEnoughPoints) {
            isSequenceCompleted(visitedBonusPointIds)
        } else {
            hasEnoughPoints
        }
    }

    /**
     * Checks if visited points follow required sequence
     */
    private fun isSequenceCompleted(visitedBonusPointIds: List<String>): Boolean {
        if (!sequenceRequired) return true

        val requiredSequence = if (isRequired) {
            sequenceOrder
        } else {
            // For minimum count, check if any valid subsequence of required length exists
            sequenceOrder.take(minimumCount ?: sequenceOrder.size)
        }

        // Check if visited points contain the required sequence in order
        var sequenceIndex = 0
        for (visitedId in visitedBonusPointIds) {
            if (sequenceIndex < requiredSequence.size && visitedId == requiredSequence[sequenceIndex]) {
                sequenceIndex++
            }
        }

        val requiredLength = minimumCount ?: requiredSequence.size
        return sequenceIndex >= requiredLength
    }

    /**
     * Calculates completion percentage based on visited bonus points
     */
    fun getCompletionPercentage(visitedBonusPointIds: List<String>): Int {
        val visitedInCombination = bonusPointIds.count { it in visitedBonusPointIds }
        val totalRequired = if (isRequired) bonusPointIds.size else (minimumCount ?: bonusPointIds.size)

        return ((visitedInCombination.toDouble() / totalRequired) * 100).toInt().coerceAtMost(100)
    }

    /**
     * Gets the points that would be awarded for the current visit status
     */
    fun getPointsAwarded(visitedBonusPointIds: List<String>): Int {
        return if (isCompletedBy(visitedBonusPointIds)) points else 0
    }

    /**
     * Gets remaining bonus points needed to complete combination
     */
    fun getRemainingBonusPoints(visitedBonusPointIds: List<String>): List<String> {
        val visited = bonusPointIds.filter { it in visitedBonusPointIds }
        val requiredCount = if (isRequired) bonusPointIds.size else (minimumCount ?: bonusPointIds.size)

        return if (visited.size >= requiredCount) {
            emptyList() // Already completed
        } else {
            bonusPointIds.filter { it !in visitedBonusPointIds }
        }
    }

    /**
     * Gets display name with point value
     */
    fun getDisplayName(): String {
        return "$name (${points}pts)"
    }

    /**
     * Gets requirement summary text
     */
    fun getRequirementText(): String {
        return if (isRequired) {
            "All ${bonusPointIds.size} points required"
        } else {
            "${minimumCount ?: bonusPointIds.size} of ${bonusPointIds.size} points required"
        }
    }

    companion object {
        /**
         * Creates a new combination with validation per data-model.md rules
         */
        fun create(
            rallyId: String,
            name: String,
            bonusPointIds: List<String>,
            points: Int,
            description: String = "",
            isRequired: Boolean = true,
            minimumCount: Int? = null,
            sequenceRequired: Boolean = false,
        ): Combination? {
            val combination = Combination(
                rallyId = rallyId,
                name = name,
                bonusPointIds = bonusPointIds,
                points = points,
                description = description,
                isRequired = isRequired,
                minimumCount = minimumCount,
                sequenceRequired = sequenceRequired,
                sequenceOrder = if (sequenceRequired) bonusPointIds else emptyList(),
            )

            return if (combination.isValid()) combination else null
        }

        /**
         * Creates a sequence-based combination
         */
        fun createSequence(
            rallyId: String,
            name: String,
            orderedBonusPointIds: List<String>,
            points: Int,
            description: String = "",
            isRequired: Boolean = true,
            minimumCount: Int? = null,
        ): Combination? {
            val combination = Combination(
                rallyId = rallyId,
                name = name,
                bonusPointIds = orderedBonusPointIds,
                points = points,
                description = description,
                isRequired = isRequired,
                minimumCount = minimumCount,
                sequenceRequired = true,
                sequenceOrder = orderedBonusPointIds,
            )

            return if (combination.isValid()) combination else null
        }
    }
}
