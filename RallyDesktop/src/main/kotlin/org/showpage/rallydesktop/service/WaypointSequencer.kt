package org.showpage.rallydesktop.service

import org.showpage.rallyserver.ui.UiWaypoint
import org.showpage.rallyserver.ui.UpdateWaypointRequest
import org.slf4j.LoggerFactory

object WaypointSequencer {
    private val logger = LoggerFactory.getLogger(WaypointSequencer::class.java)

    fun nextSequence(waypoints: List<UiWaypoint>): Int {
        return waypoints.size + 1
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
