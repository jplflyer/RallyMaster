package org.showpage.rallydesktop.service

import org.showpage.rallyserver.ui.UiBonusPoint
import org.showpage.rallyserver.ui.UiWaypoint
import org.showpage.rallyserver.ui.UpdateWaypointRequest
import org.slf4j.LoggerFactory
import kotlin.math.sqrt

/**
 * Represents an existing point in the route used for proximity calculations.
 * This could be a waypoint, or the ride's start/end bonus point.
 */
data class InsertionCandidate(
    val latitude: Double,
    val longitude: Double,
    /** Sequence position to insert AFTER (0 = insert at position 1, before all existing waypoints) */
    val insertAfterSequence: Int,
    val name: String
)

object WaypointSequencer {
    private val logger = LoggerFactory.getLogger(WaypointSequencer::class.java)

    fun nextSequence(waypoints: List<UiWaypoint>): Int {
        return waypoints.size + 1
    }
    
    /**
     * Calculate squared Euclidean distance between two lat/lng points.
     * Using squared distance avoids expensive sqrt for comparison purposes.
     */
    private fun distanceSquared(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = lat1 - lat2
        val dLon = lon1 - lon2
        return dLat * dLat + dLon * dLon
    }
    
    /**
     * Find the best sequence position to insert a new waypoint based on geographic proximity.
     * 
     * The new waypoint will be inserted AFTER the closest existing point (waypoint, start, or end).
     * 
     * @param newLat latitude of the new bonus point
     * @param newLon longitude of the new bonus point
     * @param existingWaypoints current waypoints in the leg
     * @param startBonusPoint optional ride start bonus point
     * @param endBonusPoint optional ride end bonus point
     * @return the sequence order for the new waypoint
     */
    fun findBestInsertionSequence(
        newLat: Double,
        newLon: Double,
        existingWaypoints: List<UiWaypoint>,
        startBonusPoint: UiBonusPoint?,
        endBonusPoint: UiBonusPoint?
    ): Int {
        if (existingWaypoints.isEmpty()) {
            // No existing waypoints - just use sequence 1
            return 1
        }
        
        val sortedWaypoints = existingWaypoints.sortedBy { it.sequenceOrder ?: Int.MAX_VALUE }
        
        // Build list of all route points we can compare against
        val candidates = mutableListOf<InsertionCandidate>()
        
        // Add start point (insert after it = sequence 1, before all existing waypoints)
        if (startBonusPoint?.latitude != null && startBonusPoint.longitude != null) {
            candidates.add(InsertionCandidate(
                latitude = startBonusPoint.latitude.toDouble(),
                longitude = startBonusPoint.longitude.toDouble(),
                insertAfterSequence = 0, // means insert at position 1
                name = "Start: ${startBonusPoint.code ?: startBonusPoint.name}"
            ))
        }
        
        // Add each existing waypoint
        sortedWaypoints.forEachIndexed { index, wp ->
            if (wp.latitude != null && wp.longitude != null) {
                candidates.add(InsertionCandidate(
                    latitude = wp.latitude.toDouble(),
                    longitude = wp.longitude.toDouble(),
                    insertAfterSequence = wp.sequenceOrder ?: (index + 1),
                    name = wp.name ?: "Waypoint ${index + 1}"
                ))
            }
        }
        
        // Add end point (insert after it = at the very end)
        if (endBonusPoint?.latitude != null && endBonusPoint.longitude != null) {
            val lastSeq = sortedWaypoints.lastOrNull()?.sequenceOrder ?: 0
            candidates.add(InsertionCandidate(
                latitude = endBonusPoint.latitude.toDouble(),
                longitude = endBonusPoint.longitude.toDouble(),
                insertAfterSequence = lastSeq, // Insert at the end (after last waypoint, before finish)
                name = "End: ${endBonusPoint.code ?: endBonusPoint.name}"
            ))
        }
        
        if (candidates.isEmpty()) {
            // No valid coordinates to compare against - append at end
            return sortedWaypoints.size + 1
        }
        
        // Find the closest route point
        var closestCandidate: InsertionCandidate? = null
        var closestDistance = Double.MAX_VALUE
        
        for (candidate in candidates) {
            val dist = distanceSquared(newLat, newLon, candidate.latitude, candidate.longitude)
            if (dist < closestDistance) {
                closestDistance = dist
                closestCandidate = candidate
            }
        }
        
        val insertAfterSeq = closestCandidate?.insertAfterSequence ?: sortedWaypoints.size
        val newSequence = insertAfterSeq + 1
        
        logger.info(
            "New waypoint at ({}, {}) closest to '{}' - inserting at sequence {}",
            newLat, newLon, closestCandidate?.name ?: "unknown", newSequence
        )
        
        return newSequence
    }
    
    /**
     * Shift waypoints at or after the given sequence to make room for an insertion.
     * 
     * @param waypoints current waypoints in the leg
     * @param insertAtSequence the sequence number where a new waypoint will be inserted
     * @param serverClient client for updating waypoints
     * @return updated list of waypoints with shifted sequence numbers
     */
    suspend fun makeRoomForInsertion(
        waypoints: List<UiWaypoint>,
        insertAtSequence: Int,
        serverClient: RallyServerClient
    ): Result<List<UiWaypoint>> {
        val sorted = waypoints.sortedBy { it.sequenceOrder ?: Int.MAX_VALUE }
        
        // Find waypoints that need to be shifted (those at or after insertAtSequence)
        val toShift = sorted.filter { (it.sequenceOrder ?: Int.MAX_VALUE) >= insertAtSequence }
        
        if (toShift.isEmpty()) {
            return Result.success(sorted)
        }
        
        return try {
            // Shift in reverse order to avoid conflicts
            val shifted = toShift.reversed().map { wp ->
                val newSeq = (wp.sequenceOrder ?: 0) + 1
                val updateRequest = UpdateWaypointRequest.builder()
                    .sequenceOrder(newSeq)
                    .build()
                serverClient.updateWaypoint(wp.id!!, updateRequest).getOrThrow()
                logger.debug("Shifted waypoint {} from seq {} to {}", wp.name, wp.sequenceOrder, newSeq)
                wp.setSequenceOrder(newSeq)
            }.reversed()
            
            // Rebuild the list with updated sequences
            val result = sorted.map { wp ->
                shifted.find { it.id == wp.id } ?: wp
            }
            
            Result.success(result)
        } catch (e: Exception) {
            logger.error("Failed to make room for insertion at sequence {}", insertAtSequence, e)
            Result.failure(e)
        }
    }

    suspend fun moveUp(
        waypoints: List<UiWaypoint>,
        waypoint: UiWaypoint,
        serverClient: RallyServerClient
    ): Result<List<UiWaypoint>> {
        val sorted = waypoints.sortedBy { it.sequenceOrder ?: Int.MAX_VALUE }
        val index = sorted.indexOfFirst { it.id == waypoint.id }
        
        if (index <= 0) {
            return Result.success(sorted)
        }
        
        val above = sorted[index - 1]
        val aboveSeq = above.sequenceOrder ?: index
        val currentSeq = waypoint.sequenceOrder ?: (index + 1)
        
        val updateAbove = UpdateWaypointRequest.builder()
            .sequenceOrder(currentSeq)
            .build()
        val updateCurrent = UpdateWaypointRequest.builder()
            .sequenceOrder(aboveSeq)
            .build()
        
        return try {
            serverClient.updateWaypoint(above.id!!, updateAbove).getOrThrow()
            serverClient.updateWaypoint(waypoint.id!!, updateCurrent).getOrThrow()
            
            logger.info("Swapped waypoint {} (seq {}) with {} (seq {})", 
                waypoint.name, currentSeq, above.name, aboveSeq)
            
            Result.success(sorted.map { wp ->
                when (wp.id) {
                    above.id -> wp.setSequenceOrder(currentSeq)
                    waypoint.id -> wp.setSequenceOrder(aboveSeq)
                    else -> wp
                }
            }.sortedBy { it.sequenceOrder })
        } catch (e: Exception) {
            logger.error("Failed to move waypoint up", e)
            Result.failure(e)
        }
    }

    suspend fun moveDown(
        waypoints: List<UiWaypoint>,
        waypoint: UiWaypoint,
        serverClient: RallyServerClient
    ): Result<List<UiWaypoint>> {
        val sorted = waypoints.sortedBy { it.sequenceOrder ?: Int.MAX_VALUE }
        val index = sorted.indexOfFirst { it.id == waypoint.id }
        
        if (index < 0 || index >= sorted.size - 1) {
            return Result.success(sorted)
        }
        
        val below = sorted[index + 1]
        val belowSeq = below.sequenceOrder ?: (index + 2)
        val currentSeq = waypoint.sequenceOrder ?: (index + 1)
        
        val updateBelow = UpdateWaypointRequest.builder()
            .sequenceOrder(currentSeq)
            .build()
        val updateCurrent = UpdateWaypointRequest.builder()
            .sequenceOrder(belowSeq)
            .build()
        
        return try {
            serverClient.updateWaypoint(below.id!!, updateBelow).getOrThrow()
            serverClient.updateWaypoint(waypoint.id!!, updateCurrent).getOrThrow()
            
            logger.info("Swapped waypoint {} (seq {}) with {} (seq {})", 
                waypoint.name, currentSeq, below.name, belowSeq)
            
            Result.success(sorted.map { wp ->
                when (wp.id) {
                    below.id -> wp.setSequenceOrder(currentSeq)
                    waypoint.id -> wp.setSequenceOrder(belowSeq)
                    else -> wp
                }
            }.sortedBy { it.sequenceOrder })
        } catch (e: Exception) {
            logger.error("Failed to move waypoint down", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAndRenumber(
        waypoints: List<UiWaypoint>,
        waypoint: UiWaypoint,
        serverClient: RallyServerClient
    ): Result<List<UiWaypoint>> {
        return try {
            serverClient.deleteWaypoint(waypoint.id!!).getOrThrow()
            logger.info("Deleted waypoint: {}", waypoint.name)
            
            val remaining = waypoints.filter { it.id != waypoint.id }
                .sortedBy { it.sequenceOrder ?: Int.MAX_VALUE }
            
            val renumbered = remaining.mapIndexed { index, wp ->
                val newSeq = index + 1
                if (wp.sequenceOrder != newSeq) {
                    val updateRequest = UpdateWaypointRequest.builder()
                        .sequenceOrder(newSeq)
                        .build()
                    serverClient.updateWaypoint(wp.id!!, updateRequest).getOrThrow()
                    wp.setSequenceOrder(newSeq)
                } else {
                    wp
                }
            }
            
            Result.success(renumbered)
        } catch (e: Exception) {
            logger.error("Failed to delete and renumber waypoints", e)
            Result.failure(e)
        }
    }
}
