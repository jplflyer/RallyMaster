package org.showpage.rallydesktop.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import kotlinx.coroutines.launch
import org.jxmapviewer.viewer.GeoPosition
import org.showpage.rallydesktop.service.PreferencesService
import org.showpage.rallydesktop.service.RallyServerClient
import org.showpage.rallydesktop.service.RouteResult
import org.showpage.rallydesktop.service.RoutingService
import org.showpage.rallydesktop.service.RoutingWaypoint
import org.showpage.rallydesktop.service.WaypointSequencer
import org.showpage.rallyserver.ui.*
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.time.format.DateTimeFormatter

private val logger = LoggerFactory.getLogger("RidePlanningScreen")

/**
 * Ride Planning workspace with sidebar layout:
 * - Left: Ride info, Routes/Legs/Waypoints tree, Bonus Points (if rally-associated)
 * - Right: Map showing waypoints and bonus points
 */
@Composable
fun RidePlanningScreen(
    rideId: Int,
    serverClient: RallyServerClient,
    preferencesService: PreferencesService,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var ride by remember { mutableStateOf<UiRide?>(null) }
    var rally by remember { mutableStateOf<UiRally?>(null) }
    var routes by remember { mutableStateOf(emptyList<UiRoute>()) }
    var bonusPoints by remember { mutableStateOf(emptyList<UiBonusPoint>()) }
    var combinations by remember { mutableStateOf(emptyList<UiCombination>()) }
    var allWaypoints by remember { mutableStateOf(emptyList<UiWaypoint>()) }
    var selectedLegId by remember { mutableStateOf<Int?>(null) }
    var selectedBonusPointId by remember { mutableStateOf<Int?>(null) }
    var selectedCombinationId by remember { mutableStateOf<Int?>(null) }
    var selectedWaypointId by remember { mutableStateOf<Int?>(null) }
    var activeRouteId by remember { mutableStateOf<Int?>(null) }
    var waypointReloadTrigger by remember { mutableStateOf(0) }
    var showNoLegSelectedMessage by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var routeResult by remember { mutableStateOf<RouteResult?>(null) }
    var isCalculatingRoute by remember { mutableStateOf(false) }
    var showRideEditDialog by remember { mutableStateOf(false) }
    
    val routingService = remember { RoutingService() }

    // Load ride data on first composition
    LaunchedEffect(rideId) {
        isLoading = true
        errorMessage = null

        logger.info("Loading ride with ID: {}", rideId)

        // Load the ride
        serverClient.getRide(rideId).fold(
            onSuccess = { loadedRide ->
                logger.info("Ride loaded: {}", loadedRide.name)
                ride = loadedRide

                // If rally-associated, load rally details and bonus points
                if (loadedRide.rallyId != null) {
                    serverClient.getRally(loadedRide.rallyId).fold(
                        onSuccess = { loadedRally ->
                            logger.info("Rally loaded: {}", loadedRally.name)
                            rally = loadedRally
                        },
                        onFailure = { error ->
                            logger.error("Failed to load rally", error)
                        }
                    )
                    
                    // Load bonus points for the rally
                    serverClient.listBonusPoints(loadedRide.rallyId).fold(
                        onSuccess = { loadedBonusPoints ->
                            logger.info("Loaded {} bonus points", loadedBonusPoints.size)
                            bonusPoints = loadedBonusPoints
                        },
                        onFailure = { error ->
                            logger.error("Failed to load bonus points", error)
                        }
                    )
                    
                    // Load combinations for the rally
                    serverClient.listCombinations(loadedRide.rallyId).fold(
                        onSuccess = { loadedCombinations ->
                            logger.info("Loaded {} combinations", loadedCombinations.size)
                            combinations = loadedCombinations
                        },
                        onFailure = { error ->
                            logger.error("Failed to load combinations", error)
                        }
                    )
                }

                // Load routes for the ride
                serverClient.listRoutes(rideId).fold(
                    onSuccess = { loadedRoutes ->
                        logger.info("Loaded {} routes", loadedRoutes.size)
                        routes = loadedRoutes
                    },
                    onFailure = { error ->
                        logger.error("Failed to load routes", error)
                    }
                )

                isLoading = false
            },
            onFailure = { error ->
                logger.error("Failed to load ride", error)
                errorMessage = "Failed to load ride: ${error.message}"
                isLoading = false
            }
        )
    }
    
    LaunchedEffect(routes) {
        if (selectedLegId == null && routes.isNotEmpty()) {
            val allLegs = routes.flatMap { route ->
                serverClient.listRideLegs(route.id!!).getOrElse { emptyList() }
            }
            
            val lastLegId = preferencesService.getLastSelectedLegId(rideId)
            val restoredLeg = if (lastLegId != null) allLegs.find { it.id == lastLegId } else null
            
            val targetLeg = restoredLeg ?: allLegs.firstOrNull()
            if (targetLeg != null) {
                selectedLegId = targetLeg.id
                logger.info("Selected leg: {} (restored={})", targetLeg.name, restoredLeg != null)
            }
        }
    }
    
    LaunchedEffect(selectedLegId) {
        if (selectedLegId != null) {
            preferencesService.setLastSelectedLegId(rideId, selectedLegId)
            
            for (route in routes) {
                val legs = serverClient.listRideLegs(route.id!!).getOrElse { emptyList() }
                if (legs.any { it.id == selectedLegId }) {
                    activeRouteId = route.id
                    logger.info("Active route set to {} based on selected leg {}", route.id, selectedLegId)
                    break
                }
            }
        }
    }

    LaunchedEffect(routes, waypointReloadTrigger, activeRouteId) {
        val aggregatedWaypoints = mutableListOf<UiWaypoint>()
        var globalSequence = 1
        
        val routeToLoad = if (activeRouteId != null) {
            routes.filter { it.id == activeRouteId }
        } else {
            routes.take(1)
        }
        
        for (route in routeToLoad) {
            val legs = serverClient.listRideLegs(route.id!!).getOrElse { emptyList() }
                .sortedBy { it.sequenceOrder }
            
            for (leg in legs) {
                val legWaypoints = serverClient.listWaypoints(leg.id!!).getOrElse { emptyList() }
                    .sortedBy { it.sequenceOrder }
                
                for (wp in legWaypoints) {
                    aggregatedWaypoints.add(
                        UiWaypoint.builder()
                            .id(wp.id)
                            .rideLegId(wp.rideLegId)
                            .bonusPointId(wp.bonusPointId)
                            .name(wp.name)
                            .description(wp.description)
                            .sequenceOrder(globalSequence++)
                            .latitude(wp.latitude)
                            .longitude(wp.longitude)
                            .address(wp.address)
                            .markerColor(wp.markerColor)
                            .markerIcon(wp.markerIcon)
                            .build()
                    )
                }
            }
        }
        
        allWaypoints = aggregatedWaypoints
        logger.info("Aggregated {} waypoints from active route {}", aggregatedWaypoints.size, activeRouteId)
    }

    LaunchedEffect(selectedBonusPointId, combinations) {
        if (selectedBonusPointId != null) {
            val combo = combinations.firstOrNull { combo ->
                combo.combinationPoints?.any { it.bonusPointId == selectedBonusPointId } == true
            }
            selectedCombinationId = combo?.id
            if (combo != null) {
                logger.info("Selected BP {} belongs to combo {}", selectedBonusPointId, combo.name)
            }
        } else {
            selectedCombinationId = null
        }
    }

    LaunchedEffect(selectedWaypointId, allWaypoints) {
        if (selectedWaypointId != null) {
            val waypoint = allWaypoints.find { it.id == selectedWaypointId }
            if (waypoint?.bonusPointId != null) {
                selectedBonusPointId = waypoint.bonusPointId
                logger.info("Selected waypoint {} links to BP {}", selectedWaypointId, waypoint.bonusPointId)
            }
        }
    }
    
    LaunchedEffect(allWaypoints, ride) {
        if (allWaypoints.size >= 2) {
            isCalculatingRoute = true
            
            val routingWaypoints = mutableListOf<RoutingWaypoint>()
            
            val startBpId = ride?.startingBonusPointId
            val startBp = if (startBpId != null) bonusPoints.find { it.id == startBpId } else null
            if (startBp?.latitude != null && startBp.longitude != null) {
                routingWaypoints.add(RoutingWaypoint(startBp.latitude, startBp.longitude, 0))
            }
            
            for (wp in allWaypoints.sortedBy { it.sequenceOrder }) {
                if (wp.latitude != null && wp.longitude != null) {
                    routingWaypoints.add(RoutingWaypoint(wp.latitude.toDouble(), wp.longitude.toDouble(), 0))
                }
            }
            
            val endBpId = ride?.endingBonusPointId
            val endBp = if (endBpId != null) bonusPoints.find { it.id == endBpId } else null
            if (endBp?.latitude != null && endBp.longitude != null) {
                routingWaypoints.add(RoutingWaypoint(endBp.latitude, endBp.longitude, 0))
            }
            
            if (routingWaypoints.size >= 2) {
                logger.info("Calculating route through {} waypoints", routingWaypoints.size)
                routeResult = routingService.calculateRoute(routingWaypoints)
            } else {
                routeResult = null
            }
            isCalculatingRoute = false
        } else {
            routeResult = null
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(showNoLegSelectedMessage) {
        if (showNoLegSelectedMessage) {
            snackbarHostState.showSnackbar(
                message = "Please select a leg first",
                duration = SnackbarDuration.Short
            )
            showNoLegSelectedMessage = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).padding(paddingValues)
        ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ride Planning",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedButton(onClick = onBack) {
                Text("Back to Home")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Loading/error states
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                serverClient.getRide(rideId).fold(
                                    onSuccess = { loadedRide ->
                                        ride = loadedRide
                                        isLoading = false
                                    },
                                    onFailure = { error ->
                                        errorMessage = "Failed to load ride: ${error.message}"
                                        isLoading = false
                                    }
                                )
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }
            ride != null -> {
                // Main layout: Sidebar + Map
                var sidebarCollapsed by remember { mutableStateOf(false) }
                var sidebarWidth by remember { mutableStateOf(350.dp) }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Left sidebar
                    if (!sidebarCollapsed) {
                        RidePlanSidebar(
                            ride = ride!!,
                            rally = rally,
                            routes = routes,
                            activeRouteId = activeRouteId,
                            serverClient = serverClient,
                            selectedLegId = selectedLegId,
                            selectedBonusPointId = selectedBonusPointId,
                            selectedCombinationId = selectedCombinationId,
                            selectedWaypointId = selectedWaypointId,
                            waypointReloadTrigger = waypointReloadTrigger,
                            routeResult = routeResult,
                            isCalculatingRoute = isCalculatingRoute,
                            onLegSelected = { legId -> selectedLegId = legId },
                            onBonusPointSelected = { bpId -> 
                                selectedBonusPointId = bpId
                                selectedWaypointId = null
                            },
                            onCombinationSelected = { comboId ->
                                selectedCombinationId = comboId
                                selectedBonusPointId = null
                                selectedWaypointId = null
                            },
                            onWaypointSelected = { wpId ->
                                selectedWaypointId = wpId
                            },
                            onCollapse = { sidebarCollapsed = true },
                            width = sidebarWidth,
                            onWidthChange = { sidebarWidth = it },
                            onRoutesChanged = { updatedRoutes ->
                                routes = updatedRoutes
                            },
                            onReloadRoutes = {
                                scope.launch {
                                    serverClient.listRoutes(rideId).fold(
                                        onSuccess = { loadedRoutes ->
                                            logger.info("Reloaded {} routes", loadedRoutes.size)
                                            routes = loadedRoutes
                                        },
                                        onFailure = { error ->
                                            logger.error("Failed to reload routes", error)
                                        }
                                    )
                                }
                            },
                            onWaypointAdded = { waypointReloadTrigger++ },
                            onNoLegSelected = { showNoLegSelectedMessage = true },
                            onRideUpdated = { updatedRide -> ride = updatedRide },
                            onEditRideClicked = { showRideEditDialog = true },
                            modifier = Modifier.fillMaxHeight()
                        )
                    } else {
                        // Collapsed sidebar
                        Card(
                            modifier = Modifier.width(40.dp).fillMaxHeight(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                            ) {
                                Spacer(modifier = Modifier.height(16.dp))
                                IconButton(onClick = { sidebarCollapsed = false }) {
                                    Text("▶", style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }

                    // Map panel
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        val mapRoute = routeResult?.let { result ->
                            MapRoute(
                                segments = result.segments.map { segment ->
                                    MapRouteSegment(
                                        points = segment.points.map { pt ->
                                            GeoPosition(pt.latitude, pt.longitude)
                                        },
                                        color = segment.color
                                    )
                                }
                            )
                        }
                        
                        MapViewer(
                            bonusPoints = bonusPoints,
                            combinations = combinations,
                            rideWaypoints = allWaypoints,
                            route = mapRoute,
                            centerLatitude = rally?.latitude?.toDouble(),
                            centerLongitude = rally?.longitude?.toDouble(),
                            selectedBonusPointId = selectedBonusPointId,
                            selectedCombinationId = selectedCombinationId,
                            onBonusPointClicked = { bpId ->
                                selectedBonusPointId = bpId
                                selectedWaypointId = null
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
    }
    
    if (showRideEditDialog && ride != null) {
        RideDetailsEditDialog(
            ride = ride!!,
            serverClient = serverClient,
            onDismiss = { showRideEditDialog = false },
            onRideUpdated = { updatedRide ->
                ride = updatedRide
                showRideEditDialog = false
            }
        )
    }
}

/**
 * Sidebar containing ride info, routes/legs/waypoints tree, and bonus points
 */
@Composable
fun RidePlanSidebar(
    ride: UiRide,
    rally: UiRally?,
    routes: List<UiRoute>,
    activeRouteId: Int?,
    serverClient: RallyServerClient,
    selectedLegId: Int?,
    selectedBonusPointId: Int?,
    selectedCombinationId: Int?,
    selectedWaypointId: Int?,
    waypointReloadTrigger: Int,
    routeResult: RouteResult?,
    isCalculatingRoute: Boolean,
    onLegSelected: (Int?) -> Unit,
    onBonusPointSelected: (Int?) -> Unit,
    onCombinationSelected: (Int?) -> Unit,
    onWaypointSelected: (Int?) -> Unit,
    onCollapse: () -> Unit,
    width: Dp,
    onWidthChange: (Dp) -> Unit,
    onRoutesChanged: (List<UiRoute>) -> Unit,
    onReloadRoutes: () -> Unit,
    onWaypointAdded: () -> Unit,
    onNoLegSelected: () -> Unit,
    onRideUpdated: (UiRide) -> Unit,
    onEditRideClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    Row(modifier = modifier) {
        Card(
            modifier = Modifier.width(width).fillMaxHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with collapse button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ride Planning",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onCollapse) {
                        Text("◀")  // Left arrow to collapse
                    }
                }

                HorizontalDivider()

                // Three collapsible sections
                var rideInfoCollapsed by remember { mutableStateOf(false) }
                var routesCollapsed by remember { mutableStateOf(false) }
                var bonusPointsCollapsed by remember { mutableStateOf(rally == null) }

                // Ride Info section
                CollapsibleSection(
                    title = "Ride Info",
                    isCollapsed = rideInfoCollapsed,
                    onToggleCollapse = { rideInfoCollapsed = !rideInfoCollapsed },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CompactRideInfo(
                        ride = ride,
                        rally = rally,
                        routeResult = routeResult,
                        isCalculatingRoute = isCalculatingRoute,
                        onEditClicked = onEditRideClicked,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }

                HorizontalDivider()

                // Routes/Legs/Waypoints section
                CollapsibleSection(
                    title = "Routes & Waypoints",
                    isCollapsed = routesCollapsed,
                    onToggleCollapse = { routesCollapsed = !routesCollapsed },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    RoutesTree(
                        rideId = ride.id!!,
                        routes = routes,
                        serverClient = serverClient,
                        selectedLegId = selectedLegId,
                        selectedBonusPointId = selectedBonusPointId,
                        selectedWaypointId = selectedWaypointId,
                        waypointReloadTrigger = waypointReloadTrigger,
                        onLegSelected = onLegSelected,
                        onWaypointSelected = onWaypointSelected,
                        onRoutesChanged = onRoutesChanged,
                        onReloadRoutes = onReloadRoutes,
                        onWaypointChanged = onWaypointAdded,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Combos & Bonus Points section (only if rally-associated)
                if (rally != null) {
                    HorizontalDivider()

                    CollapsibleSection(
                        title = "Combos & Bonus Points",
                        isCollapsed = bonusPointsCollapsed,
                        onToggleCollapse = { bonusPointsCollapsed = !bonusPointsCollapsed },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        RidePlanningComboTree(
                            rallyId = rally.id!!,
                            ride = ride!!,
                            routes = routes,
                            activeRouteId = activeRouteId,
                            serverClient = serverClient,
                            selectedLegId = selectedLegId,
                            selectedBonusPointId = selectedBonusPointId,
                            selectedCombinationId = selectedCombinationId,
                            waypointReloadTrigger = waypointReloadTrigger,
                            onBonusPointSelected = onBonusPointSelected,
                            onCombinationSelected = onCombinationSelected,
                            onBonusPointsAdded = { count ->
                                logger.info("Added {} bonus points as waypoints", count)
                                onWaypointAdded()
                            },
                            onNoLegSelected = onNoLegSelected,
                            onRideUpdated = onRideUpdated,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Resizable divider
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

/**
 * Compact Ride Info display
 */
@Composable
fun CompactRideInfo(
    ride: UiRide,
    rally: UiRally?,
    routeResult: RouteResult?,
    isCalculatingRoute: Boolean,
    onEditClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val routingService = remember { RoutingService() }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ride.name ?: "Unnamed Ride",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onEditClicked,
                modifier = Modifier.size(24.dp)
            ) {
                Text("✏️", style = MaterialTheme.typography.labelSmall)
            }
        }

        if (rally != null) {
            Text(
                text = "Rally: ${rally.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (ride.expectedStart != null) {
            val start = ride.expectedStart
            val end = ride.expectedEnd
            val dateText = if (end != null && start.toLocalDate() == end.toLocalDate()) {
                val dayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                "${start.format(dayFormatter)} ${start.format(timeFormatter)} to ${end.format(timeFormatter)}"
            } else if (end != null) {
                val fullFormatter = DateTimeFormatter.ofPattern("MMM d HH:mm")
                "${start.format(fullFormatter)} to ${end.format(fullFormatter)}"
            } else {
                val fullFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")
                start.format(fullFormatter)
            }
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
        
        if (isCalculatingRoute) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                Text(
                    text = "Calculating...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (routeResult != null) {
            val stopDuration = ride.stopDuration ?: 0
            val stopCount = routeResult.segments.size
            val totalStopSeconds = stopDuration * stopCount
            val totalStopMinutes = totalStopSeconds / 60
            
            val distanceStr = routingService.formatDistance(routeResult.totalDistanceMeters, useMiles = true)
            val ridingStr = routeResult.formattedDuration()
            val routeSummary = if (totalStopMinutes > 0) {
                "$distanceStr $ridingStr riding + ${totalStopMinutes}m stopped"
            } else {
                "$distanceStr $ridingStr"
            }
            
            Text(
                text = routeSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (ride.expectedStart != null) {
                val totalSeconds = routeResult.totalDurationSeconds.toLong() + totalStopSeconds
                val estimatedEnd = ride.expectedStart.plusSeconds(totalSeconds)
                val endFormatter = DateTimeFormatter.ofPattern("MMM d HH:mm")
                Text(
                    text = "Est. finish: ${estimatedEnd.format(endFormatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Text(
                text = "Add waypoints to see route",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RideDetailsEditDialog(
    ride: UiRide,
    serverClient: RallyServerClient,
    onDismiss: () -> Unit,
    onRideUpdated: (UiRide) -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(ride.name ?: "") }
    var description by remember { mutableStateOf(ride.description ?: "") }
    var expectedStartDate by remember { mutableStateOf(ride.expectedStart?.toLocalDate()?.toString() ?: "") }
    var expectedStartTime by remember { mutableStateOf(ride.expectedStart?.toLocalTime()?.toString()?.substring(0, 5) ?: "") }
    var expectedEndDate by remember { mutableStateOf(ride.expectedEnd?.toLocalDate()?.toString() ?: "") }
    var expectedEndTime by remember { mutableStateOf(ride.expectedEnd?.toLocalTime()?.toString()?.substring(0, 5) ?: "") }
    var stopDurationMinutes by remember { 
        mutableStateOf(((ride.stopDuration ?: 0) / 60).toString()) 
    }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DialogWindow(
        onCloseRequest = onDismiss,
        title = "Edit Ride Details"
    ) {
        window.minimumSize = Dimension(450, 500)
        window.size = Dimension(450, 550)

        Card(
            modifier = Modifier.fillMaxSize(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Ride Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving,
                        minLines = 2,
                        maxLines = 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = expectedStartDate,
                            onValueChange = { expectedStartDate = it },
                            label = { Text("Start Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = expectedStartTime,
                            onValueChange = { expectedStartTime = it },
                            label = { Text("Time") },
                            placeholder = { Text("HH:MM") },
                            modifier = Modifier.width(100.dp),
                            enabled = !isSaving,
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = expectedEndDate,
                            onValueChange = { expectedEndDate = it },
                            label = { Text("End Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = expectedEndTime,
                            onValueChange = { expectedEndTime = it },
                            label = { Text("Time") },
                            placeholder = { Text("HH:MM") },
                            modifier = Modifier.width(100.dp),
                            enabled = !isSaving,
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = stopDurationMinutes,
                        onValueChange = { stopDurationMinutes = it.filter { c -> c.isDigit() } },
                        label = { Text("Default Stop Duration (minutes)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving,
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onDismiss, enabled = !isSaving) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                errorMessage = null
                                
                                try {
                                    val startDateTime = if (expectedStartDate.isNotBlank()) {
                                        val date = java.time.LocalDate.parse(expectedStartDate)
                                        val time = if (expectedStartTime.isNotBlank()) {
                                            java.time.LocalTime.parse(expectedStartTime)
                                        } else {
                                            java.time.LocalTime.of(0, 0)
                                        }
                                        java.time.LocalDateTime.of(date, time)
                                    } else null
                                    
                                    val endDateTime = if (expectedEndDate.isNotBlank()) {
                                        val date = java.time.LocalDate.parse(expectedEndDate)
                                        val time = if (expectedEndTime.isNotBlank()) {
                                            java.time.LocalTime.parse(expectedEndTime)
                                        } else {
                                            java.time.LocalTime.of(23, 59)
                                        }
                                        java.time.LocalDateTime.of(date, time)
                                    } else null
                                    
                                    val stopSeconds = stopDurationMinutes.toIntOrNull()?.let { it * 60 }
                                    
                                    val request = UpdateRideRequest.builder()
                                        .name(name.trim().ifBlank { null })
                                        .description(description.trim().ifBlank { null })
                                        .expectedStart(startDateTime)
                                        .expectedEnd(endDateTime)
                                        .stopDuration(stopSeconds)
                                        .build()
                                    
                                    serverClient.updateRide(ride.id!!, request).fold(
                                        onSuccess = { updatedRide ->
                                            logger.info("Ride updated: {}", updatedRide.name)
                                            onRideUpdated(updatedRide)
                                        },
                                        onFailure = { error ->
                                            logger.error("Failed to update ride", error)
                                            errorMessage = "Failed to save: ${error.message}"
                                            isSaving = false
                                        }
                                    )
                                } catch (e: Exception) {
                                    logger.error("Invalid date/time format", e)
                                    errorMessage = "Invalid date/time format. Use YYYY-MM-DD and HH:MM"
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving && name.isNotBlank()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoutesTree(
    rideId: Int,
    routes: List<UiRoute>,
    serverClient: RallyServerClient,
    selectedLegId: Int?,
    selectedBonusPointId: Int?,
    selectedWaypointId: Int?,
    waypointReloadTrigger: Int,
    onLegSelected: (Int?) -> Unit,
    onWaypointSelected: (Int?) -> Unit,
    onRoutesChanged: (List<UiRoute>) -> Unit,
    onReloadRoutes: () -> Unit,
    onWaypointChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isCreating by remember { mutableStateOf(false) }
    
    val sortedRoutes = remember(routes) {
        routes.sortedWith(compareByDescending<UiRoute> { it.isPrimary == true }.thenBy { it.name ?: "" })
    }
    
    suspend fun createRouteWithLeg(isPrimary: Boolean) {
        isCreating = true
        val routeRequest = CreateRouteRequest.builder()
            .name("Route ${routes.size + 1}")
            .isPrimary(isPrimary)
            .build()
        
        serverClient.createRoute(rideId, routeRequest).fold(
            onSuccess = { newRoute ->
                logger.info("Route created: {}", newRoute.name)
                
                val legRequest = CreateRideLegRequest.builder()
                    .name("Leg 1")
                    .sequenceOrder(1)
                    .build()
                
                serverClient.createRideLeg(newRoute.id!!, legRequest).fold(
                    onSuccess = { newLeg ->
                        logger.info("Initial leg created: {}", newLeg.name)
                    },
                    onFailure = { error ->
                        logger.error("Failed to create initial leg", error)
                    }
                )
                
                onRoutesChanged(routes + newRoute)
                isCreating = false
            },
            onFailure = { error ->
                logger.error("Failed to create route", error)
                isCreating = false
            }
        )
    }
    
    Column(
        modifier = modifier.padding(8.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (routes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No routes yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TooltipArea(
                        tooltip = {
                            Surface(
                                color = MaterialTheme.colorScheme.inverseSurface,
                                shape = MaterialTheme.shapes.small,
                                shadowElevation = 4.dp
                            ) {
                                Text(
                                    text = "A route is one possible plan. You can create different routes while deciding what you want to do, then pick the one you intend to ride.",
                                    modifier = Modifier.padding(8.dp).widthIn(max = 300.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.inverseOnSurface
                                )
                            }
                        },
                        delayMillis = 500,
                        tooltipPlacement = TooltipPlacement.CursorPoint()
                    ) {
                        Button(
                            onClick = { scope.launch { createRouteWithLeg(isPrimary = true) } },
                            enabled = !isCreating
                        ) {
                            if (isCreating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Create Route")
                            }
                        }
                    }
                }
            }
        } else {
            sortedRoutes.forEach { route ->
                RouteItem(
                    route = route,
                    serverClient = serverClient,
                    selectedLegId = selectedLegId,
                    selectedBonusPointId = selectedBonusPointId,
                    selectedWaypointId = selectedWaypointId,
                    waypointReloadTrigger = waypointReloadTrigger,
                    onLegSelected = onLegSelected,
                    onWaypointSelected = onWaypointSelected,
                    onRouteChanged = { updatedRoute ->
                        onRoutesChanged(routes.map { if (it.id == updatedRoute.id) updatedRoute else it })
                    },
                    onRouteDeleted = {
                        val remainingRoutes = routes.filter { it.id != route.id }
                        if (route.isPrimary == true && remainingRoutes.isNotEmpty()) {
                            val newPrimaryRoute = remainingRoutes
                                .sortedBy { it.name ?: "" }
                                .first()
                            scope.launch {
                                val updateRequest = UpdateRouteRequest.builder()
                                    .isPrimary(true)
                                    .build()
                                serverClient.updateRoute(newPrimaryRoute.id!!, updateRequest).fold(
                                    onSuccess = { promoted ->
                                        logger.info("Promoted route {} to primary", promoted.name)
                                        onReloadRoutes()
                                    },
                                    onFailure = { error ->
                                        logger.error("Failed to promote route to primary", error)
                                        onRoutesChanged(remainingRoutes)
                                    }
                                )
                            }
                        } else {
                            onRoutesChanged(remainingRoutes)
                        }
                    },
                    onReloadRoutes = onReloadRoutes,
                    onWaypointChanged = onWaypointChanged
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TooltipArea(
                tooltip = {
                    Surface(
                        color = MaterialTheme.colorScheme.inverseSurface,
                        shape = MaterialTheme.shapes.small,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = "A route is one possible plan. You can create different routes while deciding what you want to do, then pick the one you intend to ride.",
                            modifier = Modifier.padding(8.dp).widthIn(max = 300.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface
                        )
                    }
                },
                delayMillis = 500,
                tooltipPlacement = TooltipPlacement.CursorPoint()
            ) {
                OutlinedButton(
                    onClick = { scope.launch { createRouteWithLeg(isPrimary = false) } },
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("+ Add Route")
                    }
                }
            }
        }
    }
}

/**
 * Single route item showing legs and waypoints
 */
@Composable
fun RouteItem(
    route: UiRoute,
    serverClient: RallyServerClient,
    selectedLegId: Int?,
    selectedBonusPointId: Int?,
    selectedWaypointId: Int?,
    waypointReloadTrigger: Int,
    onLegSelected: (Int?) -> Unit,
    onWaypointSelected: (Int?) -> Unit,
    onRouteChanged: (UiRoute) -> Unit,
    onRouteDeleted: () -> Unit,
    onReloadRoutes: () -> Unit,
    onWaypointChanged: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    var legs by remember { mutableStateOf(emptyList<UiRideLeg>()) }
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember(route.name) { mutableStateOf(route.name ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isCreatingLeg by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(route.id) {
        if (route.id != null) {
            serverClient.listRideLegs(route.id).fold(
                onSuccess = { loadedLegs ->
                    logger.info("Loaded {} legs for route {}", loadedLegs.size, route.id)
                    legs = loadedLegs.sortedBy { it.sequenceOrder ?: Int.MAX_VALUE }
                },
                onFailure = { error ->
                    logger.error("Failed to load legs for route {}", route.id, error)
                }
            )
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isExpanded) "▼" else "▶",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { isExpanded = !isExpanded }
            )
            
            if (isEditing) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                IconButton(
                    onClick = {
                        scope.launch {
                            val updateRequest = UpdateRouteRequest.builder()
                                .name(editedName.trim())
                                .build()
                            serverClient.updateRoute(route.id!!, updateRequest).fold(
                                onSuccess = { updated ->
                                    logger.info("Route renamed to: {}", updated.name)
                                    onRouteChanged(updated)
                                    isEditing = false
                                },
                                onFailure = { error ->
                                    logger.error("Failed to rename route", error)
                                }
                            )
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("✓", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(
                    onClick = {
                        editedName = route.name ?: ""
                        isEditing = false
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("✕", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text(
                    text = route.name ?: "Route",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (route.isPrimary == true) "★" else "☆",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(enabled = route.isPrimary != true) {
                        scope.launch {
                            val updateRequest = UpdateRouteRequest.builder()
                                .isPrimary(true)
                                .build()
                            serverClient.updateRoute(route.id!!, updateRequest).fold(
                                onSuccess = { updated ->
                                    logger.info("Route set as primary: {}", updated.name)
                                    onReloadRoutes()
                                },
                                onFailure = { error ->
                                    logger.error("Failed to set route as primary", error)
                                }
                            )
                        }
                    }
                )
                IconButton(
                    onClick = { isEditing = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("✏️", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("🗑", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (isExpanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp)
            ) {
                legs.forEach { leg ->
                    RideLegItem(
                        leg = leg,
                        serverClient = serverClient,
                        isSelected = selectedLegId == leg.id,
                        selectedBonusPointId = selectedBonusPointId,
                        selectedWaypointId = selectedWaypointId,
                        waypointReloadTrigger = waypointReloadTrigger,
                        onSelect = { onLegSelected(leg.id) },
                        onWaypointSelected = onWaypointSelected,
                        onLegChanged = { updatedLeg ->
                            legs = legs.map { if (it.id == updatedLeg.id) updatedLeg else it }
                        },
                        onLegDeleted = {
                            legs = legs.filter { it.id != leg.id }
                            if (selectedLegId == leg.id) {
                                onLegSelected(null)
                            }
                            onWaypointChanged()
                        },
                        onWaypointChanged = onWaypointChanged
                    )
                }

                if (legs.isEmpty()) {
                    Text(
                        text = "No legs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                
                TextButton(
                    onClick = {
                        scope.launch {
                            isCreatingLeg = true
                            val legRequest = CreateRideLegRequest.builder()
                                .name("Leg ${legs.size + 1}")
                                .sequenceOrder(legs.size + 1)
                                .build()
                            
                            serverClient.createRideLeg(route.id!!, legRequest).fold(
                                onSuccess = { newLeg ->
                                    logger.info("Leg created: {}", newLeg.name)
                                    legs = (legs + newLeg).sortedBy { it.sequenceOrder ?: Int.MAX_VALUE }
                                    isCreatingLeg = false
                                },
                                onFailure = { error ->
                                    logger.error("Failed to create leg", error)
                                    isCreatingLeg = false
                                }
                            )
                        }
                    },
                    enabled = !isCreatingLeg,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    if (isCreatingLeg) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("+ Add Leg", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Route?") },
            text = { 
                Text("Are you sure you want to delete \"${route.name}\"? This will also delete all legs and waypoints.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            serverClient.deleteRoute(route.id!!).fold(
                                onSuccess = {
                                    logger.info("Route deleted: {}", route.name)
                                    showDeleteConfirm = false
                                    onRouteDeleted()
                                },
                                onFailure = { error ->
                                    logger.error("Failed to delete route", error)
                                    showDeleteConfirm = false
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Single leg item showing waypoints
 */
@Composable
fun RideLegItem(
    leg: UiRideLeg,
    serverClient: RallyServerClient,
    isSelected: Boolean,
    selectedBonusPointId: Int?,
    selectedWaypointId: Int?,
    waypointReloadTrigger: Int,
    onSelect: () -> Unit,
    onWaypointSelected: (Int?) -> Unit,
    onLegChanged: (UiRideLeg) -> Unit,
    onLegDeleted: () -> Unit,
    onWaypointChanged: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    var waypoints by remember { mutableStateOf(emptyList<UiWaypoint>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var editingWaypoint by remember { mutableStateOf<UiWaypoint?>(null) }
    val scope = rememberCoroutineScope()

    // Reload waypoints when the trigger changes (from parent when waypoint is added)
    LaunchedEffect(leg.id, waypointReloadTrigger) {
        if (leg.id != null) {
            serverClient.listWaypoints(leg.id).fold(
                onSuccess = { loadedWaypoints ->
                    logger.info("Loaded {} waypoints for leg {}", loadedWaypoints.size, leg.id)
                    waypoints = loadedWaypoints.sortedBy { it.sequenceOrder }
                },
                onFailure = { error ->
                    logger.error("Failed to load waypoints for leg {}", leg.id, error)
                }
            )
        }
    }

    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .clickable { onSelect() }
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isExpanded) "▼" else "▶",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { isExpanded = !isExpanded }
            )
            Text(
                text = leg.name ?: "Leg",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Text(
                    text = "●",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (waypoints.isNotEmpty()) {
                IconButton(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.size(20.dp)
                ) {
                    Text("✕", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.size(20.dp)
            ) {
                Text("🗑", style = MaterialTheme.typography.labelSmall)
            }
        }

        if (isExpanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp)
            ) {
                waypoints.forEachIndexed { index, waypoint ->
                    val isWpSelected = selectedWaypointId == waypoint.id
                    val isLinkedToSelectedBp = waypoint.bonusPointId != null && 
                                               waypoint.bonusPointId == selectedBonusPointId
                    
                    WaypointItem(
                        waypoint = waypoint,
                        isSelected = isWpSelected,
                        isHighlighted = isLinkedToSelectedBp && !isWpSelected,
                        onSelect = { 
                            onWaypointSelected(if (selectedWaypointId == waypoint.id) null else waypoint.id) 
                        },
                        onDelete = {
                            scope.launch {
                                WaypointSequencer.deleteAndRenumber(waypoints, waypoint, serverClient).fold(
                                    onSuccess = { updated ->
                                        waypoints = updated
                                        onWaypointSelected(null)
                                        onWaypointChanged()
                                    },
                                    onFailure = { error ->
                                        logger.error("Failed to delete waypoint", error)
                                    }
                                )
                            }
                        },
                        onEdit = { editingWaypoint = waypoint },
                        onMoveUp = if (index > 0) {
                            {
                                scope.launch {
                                    WaypointSequencer.moveUp(waypoints, waypoint, serverClient).fold(
                                        onSuccess = { updated ->
                                            waypoints = updated
                                            onWaypointChanged()
                                        },
                                        onFailure = { error -> logger.error("Failed to move waypoint up", error) }
                                    )
                                }
                            }
                        } else null,
                        onMoveDown = if (index < waypoints.size - 1) {
                            {
                                scope.launch {
                                    WaypointSequencer.moveDown(waypoints, waypoint, serverClient).fold(
                                        onSuccess = { updated ->
                                            waypoints = updated
                                            onWaypointChanged()
                                        },
                                        onFailure = { error -> logger.error("Failed to move waypoint down", error) }
                                    )
                                }
                            }
                        } else null
                    )
                }

                if (waypoints.isEmpty()) {
                    Text(
                        text = if (isSelected) "Double-click a bonus point to add it" else "No waypoints",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Leg?") },
            text = { 
                Text("Are you sure you want to delete \"${leg.name}\"? This will also delete all waypoints in this leg.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            serverClient.deleteRideLeg(leg.id!!).fold(
                                onSuccess = {
                                    logger.info("Deleted leg: {}", leg.name)
                                    showDeleteConfirm = false
                                    onLegDeleted()
                                },
                                onFailure = { error ->
                                    logger.error("Failed to delete leg", error)
                                    showDeleteConfirm = false
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Waypoints?") },
            text = { 
                Text("Remove all ${waypoints.size} waypoints from \"${leg.name}\"?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            var allDeleted = true
                            for (wp in waypoints) {
                                serverClient.deleteWaypoint(wp.id!!).fold(
                                    onSuccess = {
                                        logger.info("Deleted waypoint: {}", wp.name)
                                    },
                                    onFailure = { error ->
                                        logger.error("Failed to delete waypoint: {}", wp.name, error)
                                        allDeleted = false
                                    }
                                )
                            }
                            if (allDeleted) {
                                waypoints = emptyList()
                            }
                            showClearConfirm = false
                            onWaypointSelected(null)
                            onWaypointChanged()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (editingWaypoint != null) {
        WaypointEditDialog(
            waypoint = editingWaypoint!!,
            serverClient = serverClient,
            onDismiss = { editingWaypoint = null },
            onWaypointUpdated = { updated ->
                waypoints = waypoints.map { if (it.id == updated.id) updated else it }
                editingWaypoint = null
                onWaypointChanged()
            }
        )
    }
}

/**
 * Single waypoint item
 */
@Composable
fun WaypointItem(
    waypoint: UiWaypoint,
    isSelected: Boolean,
    isHighlighted: Boolean = false,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onSelect() }
            .padding(vertical = 2.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${waypoint.sequenceOrder ?: "?"}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = waypoint.name ?: "Waypoint",
                style = MaterialTheme.typography.bodySmall
            )
            if (waypoint.stopDuration != null) {
                Text(
                    text = "${waypoint.stopDuration / 60}m stop",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        if (isSelected) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(20.dp)
            ) {
                Text("✏️", style = MaterialTheme.typography.labelSmall)
            }
            if (onMoveUp != null) {
                IconButton(
                    onClick = onMoveUp,
                    modifier = Modifier.size(20.dp)
                ) {
                    Text("↑", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (onMoveDown != null) {
                IconButton(
                    onClick = onMoveDown,
                    modifier = Modifier.size(20.dp)
                ) {
                    Text("↓", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(20.dp)
            ) {
                Text("🗑", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun WaypointEditDialog(
    waypoint: UiWaypoint,
    serverClient: RallyServerClient,
    onDismiss: () -> Unit,
    onWaypointUpdated: (UiWaypoint) -> Unit
) {
    val scope = rememberCoroutineScope()
    var stopDurationMinutes by remember { 
        mutableStateOf(waypoint.stopDuration?.let { (it / 60).toString() } ?: "") 
    }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Waypoint") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = waypoint.name ?: "Waypoint",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = stopDurationMinutes,
                    onValueChange = { stopDurationMinutes = it.filter { c -> c.isDigit() } },
                    label = { Text("Stop Duration (minutes)") },
                    placeholder = { Text("Leave empty for ride default") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    singleLine = true
                )
                
                Text(
                    text = "Leave empty to use the ride's default stop duration",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        
                        val stopSeconds = stopDurationMinutes.toIntOrNull()?.let { it * 60 }
                        
                        val request = UpdateWaypointRequest.builder()
                            .stopDuration(stopSeconds)
                            .build()
                        
                        serverClient.updateWaypoint(waypoint.id!!, request).fold(
                            onSuccess = { updatedWaypoint ->
                                logger.info("Waypoint updated: {}", updatedWaypoint.name)
                                onWaypointUpdated(updatedWaypoint)
                            },
                            onFailure = { error ->
                                logger.error("Failed to update waypoint", error)
                                errorMessage = "Failed to save: ${error.message}"
                                isSaving = false
                            }
                        )
                    }
                },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for creating a new ride
 */
@Composable
fun CreateRideDialog(
    serverClient: RallyServerClient,
    onDismiss: () -> Unit,
    onRideCreated: (Int) -> Unit
) {
    var rideName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Ride") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = rideName,
                    onValueChange = { rideName = it },
                    label = { Text("Ride Name") },
                    placeholder = { Text("e.g., Saddlesore 1000") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating,
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Ride details...") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating,
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rideName.isBlank()) {
                        errorMessage = "Ride name is required"
                        return@Button
                    }

                    scope.launch {
                        isCreating = true
                        errorMessage = null

                        val request = CreateRideRequest.builder()
                            .name(rideName.trim())
                            .description(if (description.isBlank()) null else description.trim())
                            .build()

                        serverClient.createRide(request).fold(
                            onSuccess = { ride ->
                                logger.info("Ride created: {} (ID: {})", ride.name, ride.id)
                                
                                val routeRequest = CreateRouteRequest.builder()
                                    .name("Primary Route")
                                    .isPrimary(true)
                                    .build()
                                
                                serverClient.createRoute(ride.id!!, routeRequest).fold(
                                    onSuccess = { route ->
                                        logger.info("Initial route created: {} (ID: {})", route.name, route.id)
                                        
                                        val legRequest = CreateRideLegRequest.builder()
                                            .name("Leg 1")
                                            .sequenceOrder(1)
                                            .build()
                                        
                                        serverClient.createRideLeg(route.id!!, legRequest).fold(
                                            onSuccess = { leg ->
                                                logger.info("Initial leg created: {}", leg.name)
                                            },
                                            onFailure = { error ->
                                                logger.error("Failed to create initial leg", error)
                                            }
                                        )
                                        
                                        onRideCreated(ride.id)
                                    },
                                    onFailure = { error ->
                                        logger.error("Failed to create initial route", error)
                                        onRideCreated(ride.id)
                                    }
                                )
                            },
                            onFailure = { error ->
                                logger.error("Failed to create ride", error)
                                errorMessage = "Failed to create ride: ${error.message}"
                                isCreating = false
                            }
                        )
                    }
                },
                enabled = !isCreating && rideName.isNotBlank()
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("Cancel")
            }
        }
    )
}

enum class ComboInclusionStatus {
    FULL,
    PARTIAL,
    NONE
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RidePlanningComboTree(
    rallyId: Int,
    ride: UiRide,
    routes: List<UiRoute>,
    activeRouteId: Int?,
    serverClient: RallyServerClient,
    selectedLegId: Int?,
    selectedBonusPointId: Int?,
    selectedCombinationId: Int?,
    waypointReloadTrigger: Int,
    onBonusPointSelected: (Int?) -> Unit,
    onCombinationSelected: (Int?) -> Unit,
    onBonusPointsAdded: (Int) -> Unit,
    onNoLegSelected: () -> Unit,
    onRideUpdated: (UiRide) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    var combinations by remember { mutableStateOf<List<UiCombination>>(emptyList()) }
    var bonusPoints by remember { mutableStateOf<List<UiBonusPoint>>(emptyList()) }
    var includedBonusPointIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var expandedCombos by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var unassociatedExpanded by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var contextMenuCombo by remember { mutableStateOf<UiCombination?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuBp by remember { mutableStateOf<UiBonusPoint?>(null) }
    var showBpContextMenu by remember { mutableStateOf(false) }
    
    LaunchedEffect(rallyId) {
        isLoading = true
        errorMessage = null
        
        serverClient.listCombinations(rallyId).fold(
            onSuccess = { combos ->
                logger.info("Loaded {} combinations", combos.size)
                combinations = combos
            },
            onFailure = { error ->
                logger.error("Failed to load combinations", error)
                errorMessage = "Failed to load combinations: ${error.message}"
            }
        )
        
        serverClient.listBonusPoints(rallyId).fold(
            onSuccess = { points ->
                logger.info("Loaded {} bonus points", points.size)
                bonusPoints = points
            },
            onFailure = { error ->
                logger.error("Failed to load bonus points", error)
            }
        )
        
        isLoading = false
    }
    
    LaunchedEffect(routes, waypointReloadTrigger, activeRouteId) {
        val allBonusPointIds = mutableSetOf<Int>()
        val routesToCheck = if (activeRouteId != null) {
            routes.filter { it.id == activeRouteId }
        } else {
            routes.take(1)
        }
        
        for (route in routesToCheck) {
            if (route.id != null) {
                serverClient.listRideLegs(route.id).getOrNull()?.forEach { leg ->
                    if (leg.id != null) {
                        serverClient.listWaypoints(leg.id).getOrNull()?.forEach { waypoint ->
                            waypoint.bonusPointId?.let { allBonusPointIds.add(it) }
                        }
                    }
                }
            }
        }
        includedBonusPointIds = allBonusPointIds
        logger.info("Found {} bonus points included in active route {}", allBonusPointIds.size, activeRouteId)
    }
    
    fun getComboInclusionStatus(combo: UiCombination): ComboInclusionStatus {
        val comboBpIds = combo.combinationPoints?.mapNotNull { it.bonusPointId }?.toSet() ?: emptySet()
        if (comboBpIds.isEmpty()) return ComboInclusionStatus.NONE
        
        val includedCount = comboBpIds.count { it in includedBonusPointIds }
        return when {
            includedCount == comboBpIds.size -> ComboInclusionStatus.FULL
            includedCount > 0 -> ComboInclusionStatus.PARTIAL
            else -> ComboInclusionStatus.NONE
        }
    }
    
    val sortedCombinations = remember(combinations, includedBonusPointIds) {
        combinations.sortedWith(
            compareBy<UiCombination> { combo ->
                when (getComboInclusionStatus(combo)) {
                    ComboInclusionStatus.FULL -> 0
                    ComboInclusionStatus.PARTIAL -> 1
                    ComboInclusionStatus.NONE -> 2
                }
            }.thenBy { it.code ?: "" }
        )
    }
    
    val bonusPointMap = remember(bonusPoints) { bonusPoints.associateBy { it.id } }
    
    val associatedBpIds = remember(combinations) {
        combinations.flatMap { combo ->
            combo.combinationPoints?.mapNotNull { it.bonusPointId } ?: emptyList()
        }.toSet()
    }
    
    val unassociatedBonusPoints = remember(bonusPoints, associatedBpIds, includedBonusPointIds) {
        bonusPoints
            .filter { it.id !in associatedBpIds }
            .sortedWith(
                compareBy<UiBonusPoint> { bp ->
                    if (bp.id in includedBonusPointIds) 0 else 1
                }.thenBy { it.code ?: "" }
            )
    }
    
    suspend fun addBonusPointAsWaypoint(bp: UiBonusPoint, legId: Int): Boolean {
        val existingWaypoints = serverClient.listWaypoints(legId).getOrElse { emptyList() }
        
        if (existingWaypoints.any { it.bonusPointId == bp.id }) {
            logger.info("Bonus point {} already in leg {}", bp.code, legId)
            return false
        }
        
        val newLat = bp.latitude?.toDouble()
        val newLon = bp.longitude?.toDouble()
        
        val insertSequence = if (newLat != null && newLon != null) {
            val startBp = ride.startingBonusPointId?.let { id -> bonusPointMap[id] }
            val endBp = ride.endingBonusPointId?.let { id -> bonusPointMap[id] }
            
            WaypointSequencer.findBestInsertionSequence(
                newLat = newLat,
                newLon = newLon,
                existingWaypoints = existingWaypoints,
                startBonusPoint = startBp,
                endBonusPoint = endBp
            )
        } else {
            WaypointSequencer.nextSequence(existingWaypoints)
        }
        
        if (existingWaypoints.any { (it.sequenceOrder ?: 0) >= insertSequence }) {
            WaypointSequencer.makeRoomForInsertion(existingWaypoints, insertSequence, serverClient)
                .onFailure { error ->
                    logger.error("Failed to make room for insertion", error)
                    return false
                }
        }
        
        val waypointRequest = CreateWaypointRequest.builder()
            .name(bp.code ?: bp.name ?: "Waypoint")
            .bonusPointId(bp.id)
            .latitude(bp.latitude?.toFloat())
            .longitude(bp.longitude?.toFloat())
            .sequenceOrder(insertSequence)
            .build()
        
        return serverClient.createWaypoint(legId, waypointRequest).fold(
            onSuccess = {
                logger.info("Added bonus point {} as waypoint to leg {} at sequence {}", bp.code, legId, insertSequence)
                true
            },
            onFailure = { error ->
                logger.error("Failed to add bonus point {} as waypoint", bp.code, error)
                false
            }
        )
    }
    
    suspend fun removeComboFromRoute(combo: UiCombination): Int {
        val comboBpIds = combo.combinationPoints?.mapNotNull { it.bonusPointId }?.toSet() ?: emptySet()
        var removedCount = 0
        
        for (route in routes) {
            if (route.id != null) {
                serverClient.listRideLegs(route.id).getOrNull()?.forEach { leg ->
                    if (leg.id != null) {
                        serverClient.listWaypoints(leg.id).getOrNull()?.forEach { waypoint ->
                            if (waypoint.bonusPointId in comboBpIds) {
                                serverClient.deleteWaypoint(waypoint.id!!).fold(
                                    onSuccess = {
                                        logger.info("Removed waypoint {} (BP {}) from leg {}", waypoint.name, waypoint.bonusPointId, leg.id)
                                        removedCount++
                                    },
                                    onFailure = { error ->
                                        logger.error("Failed to remove waypoint {}", waypoint.name, error)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        return removedCount
    }
    
    suspend fun setRallyStart(bp: UiBonusPoint) {
        val updateRequest = UpdateRideRequest.builder()
            .startingBonusPointId(bp.id)
            .build()
        serverClient.updateRide(ride.id!!, updateRequest).fold(
            onSuccess = { updatedRide ->
                logger.info("Set rally start to {}", bp.code)
                onRideUpdated(updatedRide)
            },
            onFailure = { error ->
                logger.error("Failed to set rally start", error)
            }
        )
    }
    
    suspend fun setRallyFinish(bp: UiBonusPoint) {
        val updateRequest = UpdateRideRequest.builder()
            .endingBonusPointId(bp.id)
            .build()
        serverClient.updateRide(ride.id!!, updateRequest).fold(
            onSuccess = { updatedRide ->
                logger.info("Set rally finish to {}", bp.code)
                onRideUpdated(updatedRide)
            },
            onFailure = { error ->
                logger.error("Failed to set rally finish", error)
            }
        )
    }
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${combinations.size} combos, ${unassociatedBonusPoints.size} other",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            combinations.isEmpty() && unassociatedBonusPoints.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No bonus points defined",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                val listState = rememberLazyListState()
                
                LaunchedEffect(selectedBonusPointId, sortedCombinations, unassociatedBonusPoints) {
                    if (selectedBonusPointId == null) return@LaunchedEffect
                    
                    var targetComboIndex: Int? = null
                    
                    for ((comboIndex, combo) in sortedCombinations.withIndex()) {
                        val cpIndex = combo.combinationPoints?.indexOfFirst { it.bonusPointId == selectedBonusPointId } ?: -1
                        if (cpIndex >= 0) {
                            targetComboIndex = comboIndex
                            
                            if (combo.id != null && !expandedCombos.contains(combo.id)) {
                                expandedCombos = expandedCombos + combo.id
                            }
                            break
                        }
                    }
                    
                    if (targetComboIndex != null) {
                        listState.animateScrollToItem(targetComboIndex)
                    } else {
                        val unassocIndex = unassociatedBonusPoints.indexOfFirst { it.id == selectedBonusPointId }
                        if (unassocIndex >= 0) {
                            if (!unassociatedExpanded) {
                                unassociatedExpanded = true
                            }
                            val listIndex = sortedCombinations.size + 1 + unassocIndex
                            listState.animateScrollToItem(listIndex)
                        }
                    }
                }
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    items(
                        count = sortedCombinations.size,
                        key = { index -> sortedCombinations[index].id ?: index }
                    ) { index ->
                        val combo = sortedCombinations[index]
                        val isExpanded = expandedCombos.contains(combo.id)
                        val inclusionStatus = getComboInclusionStatus(combo)
                        val isComboSelected = combo.id == selectedCombinationId
                        
                        Column {
                            RidePlanningComboItem(
                                combination = combo,
                                inclusionStatus = inclusionStatus,
                                isExpanded = isExpanded,
                                isSelected = isComboSelected,
                                onToggleExpand = {
                                    expandedCombos = if (isExpanded) {
                                        expandedCombos - combo.id!!
                                    } else {
                                        expandedCombos + combo.id!!
                                    }
                                },
                                onClick = {
                                    onCombinationSelected(if (isComboSelected) null else combo.id)
                                },
                                onDoubleClick = {
                                    val legId = selectedLegId
                                    if (legId != null) {
                                        scope.launch {
                                            val bpIds = combo.combinationPoints?.mapNotNull { it.bonusPointId } ?: emptyList()
                                            var addedCount = 0
                                            for (bpId in bpIds) {
                                                val bp = bonusPointMap[bpId]
                                                if (bp != null && addBonusPointAsWaypoint(bp, legId)) {
                                                    addedCount++
                                                }
                                            }
                                            if (addedCount > 0) {
                                                onBonusPointsAdded(addedCount)
                                            }
                                        }
                                    } else {
                                        onNoLegSelected()
                                    }
                                },
                                onRightClick = {
                                    contextMenuCombo = combo
                                    showContextMenu = true
                                }
                            )
                            
                            if (isExpanded && combo.combinationPoints?.isNotEmpty() == true) {
                                Column(modifier = Modifier.padding(start = 24.dp)) {
                                    combo.combinationPoints.forEach { cp ->
                                        val bp = bonusPointMap[cp.bonusPointId]
                                        val isIncluded = cp.bonusPointId in includedBonusPointIds
                                        val isBpSelected = cp.bonusPointId == selectedBonusPointId
                                        val isInSelectedCombo = isComboSelected
                                        
                                        RidePlanningBonusPointItem(
                                            bonusPoint = bp,
                                            bonusPointId = cp.bonusPointId,
                                            isIncluded = isIncluded,
                                            isSelected = isBpSelected,
                                            isHighlighted = isInSelectedCombo && !isBpSelected,
                                            isStart = cp.bonusPointId == ride.startingBonusPointId,
                                            isFinish = cp.bonusPointId == ride.endingBonusPointId,
                                            onClick = {
                                                onBonusPointSelected(if (isBpSelected) null else cp.bonusPointId)
                                            },
                                            onDoubleClick = {
                                                val legId = selectedLegId
                                                if (legId != null && bp != null) {
                                                    scope.launch {
                                                        if (addBonusPointAsWaypoint(bp, legId)) {
                                                            onBonusPointsAdded(1)
                                                        }
                                                    }
                                                } else if (legId == null) {
                                                    onNoLegSelected()
                                                }
                                            },
                                            onRightClick = if (bp != null) {
                                                {
                                                    contextMenuBp = bp
                                                    showBpContextMenu = true
                                                }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    if (unassociatedBonusPoints.isNotEmpty()) {
                        item(key = "unassociated_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { unassociatedExpanded = !unassociatedExpanded }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (unassociatedExpanded) "▼" else "▶",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Other Bonus Points",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "(${unassociatedBonusPoints.size})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        if (unassociatedExpanded) {
                            items(
                                count = unassociatedBonusPoints.size,
                                key = { index -> "unassoc_${unassociatedBonusPoints[index].id}" }
                            ) { index ->
                                val bp = unassociatedBonusPoints[index]
                                val isIncluded = bp.id in includedBonusPointIds
                                val isBpSelected = bp.id == selectedBonusPointId
                                
                                RidePlanningBonusPointItem(
                                    bonusPoint = bp,
                                    bonusPointId = bp.id!!,
                                    isIncluded = isIncluded,
                                    isSelected = isBpSelected,
                                    isHighlighted = false,
                                    isStart = bp.id == ride.startingBonusPointId,
                                    isFinish = bp.id == ride.endingBonusPointId,
                                    onClick = {
                                        onBonusPointSelected(if (isBpSelected) null else bp.id)
                                    },
                                    onDoubleClick = {
                                        val legId = selectedLegId
                                        if (legId != null) {
                                            scope.launch {
                                                if (addBonusPointAsWaypoint(bp, legId)) {
                                                    onBonusPointsAdded(1)
                                                }
                                            }
                                        } else {
                                            onNoLegSelected()
                                        }
                                    },
                                    onRightClick = {
                                        contextMenuBp = bp
                                        showBpContextMenu = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showContextMenu && contextMenuCombo != null) {
        val comboStatus = getComboInclusionStatus(contextMenuCombo!!)
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { 
                showContextMenu = false
                contextMenuCombo = null
            }
        ) {
            if (comboStatus != ComboInclusionStatus.NONE) {
                DropdownMenuItem(
                    text = { Text("Remove from route") },
                    onClick = {
                        val combo = contextMenuCombo!!
                        scope.launch {
                            val removedCount = removeComboFromRoute(combo)
                            if (removedCount > 0) {
                                logger.info("Removed {} waypoints for combo {}", removedCount, combo.code)
                                onBonusPointsAdded(0)
                            }
                        }
                        showContextMenu = false
                        contextMenuCombo = null
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Add all to route") },
                onClick = {
                    val combo = contextMenuCombo!!
                    val legId = selectedLegId
                    if (legId != null) {
                        scope.launch {
                            val bpIds = combo.combinationPoints?.mapNotNull { it.bonusPointId } ?: emptyList()
                            var addedCount = 0
                            for (bpId in bpIds) {
                                val bp = bonusPointMap[bpId]
                                if (bp != null && addBonusPointAsWaypoint(bp, legId)) {
                                    addedCount++
                                }
                            }
                            if (addedCount > 0) {
                                onBonusPointsAdded(addedCount)
                            }
                        }
                    } else {
                        onNoLegSelected()
                    }
                    showContextMenu = false
                    contextMenuCombo = null
                }
            )
        }
    }
    
    if (showBpContextMenu && contextMenuBp != null) {
        val bp = contextMenuBp!!
        val isIncluded = bp.id in includedBonusPointIds
        DropdownMenu(
            expanded = showBpContextMenu,
            onDismissRequest = { 
                showBpContextMenu = false
                contextMenuBp = null
            }
        ) {
            DropdownMenuItem(
                text = { Text("Set as Rally Start") },
                onClick = {
                    scope.launch { setRallyStart(bp) }
                    showBpContextMenu = false
                    contextMenuBp = null
                }
            )
            DropdownMenuItem(
                text = { Text("Set as Rally Finish") },
                onClick = {
                    scope.launch { setRallyFinish(bp) }
                    showBpContextMenu = false
                    contextMenuBp = null
                }
            )
            HorizontalDivider()
            if (!isIncluded) {
                DropdownMenuItem(
                    text = { Text("Add to route") },
                    onClick = {
                        val legId = selectedLegId
                        if (legId != null) {
                            scope.launch {
                                if (addBonusPointAsWaypoint(bp, legId)) {
                                    onBonusPointsAdded(1)
                                }
                            }
                        } else {
                            onNoLegSelected()
                        }
                        showBpContextMenu = false
                        contextMenuBp = null
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RidePlanningComboItem(
    combination: UiCombination,
    inclusionStatus: ComboInclusionStatus,
    isExpanded: Boolean,
    isSelected: Boolean = false,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onRightClick: () -> Unit
) {
    val baseBackgroundColor = when (inclusionStatus) {
        ComboInclusionStatus.FULL -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ComboInclusionStatus.PARTIAL -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ComboInclusionStatus.NONE -> Color.Transparent
    }
    
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    } else {
        baseBackgroundColor
    }
    
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
        inclusionStatus == ComboInclusionStatus.FULL -> MaterialTheme.colorScheme.primary
        inclusionStatus == ComboInclusionStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    val statusIcon = when (inclusionStatus) {
        ComboInclusionStatus.FULL -> "✓"
        ComboInclusionStatus.PARTIAL -> "◐"
        ComboInclusionStatus.NONE -> ""
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick,
                onLongClick = onRightClick
            )
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isExpanded) "▼" else "▶",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.clickable(onClick = onToggleExpand)
        )
        
        if (statusIcon.isNotEmpty()) {
            Text(
                text = statusIcon,
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
        }
        
        Text(
            text = combination.code ?: "?",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (inclusionStatus == ComboInclusionStatus.FULL || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            modifier = Modifier.width(60.dp)
        )
        
        Text(
            text = combination.name ?: "",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (inclusionStatus == ComboInclusionStatus.FULL || isSelected) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (inclusionStatus == ComboInclusionStatus.PARTIAL) FontStyle.Italic else FontStyle.Normal,
            color = textColor,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        
        if (combination.points != null) {
            Text(
                text = "${combination.points}",
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RidePlanningBonusPointItem(
    bonusPoint: UiBonusPoint?,
    bonusPointId: Int,
    isIncluded: Boolean,
    isSelected: Boolean = false,
    isHighlighted: Boolean = false,
    isStart: Boolean = false,
    isFinish: Boolean = false,
    onClick: () -> Unit = {},
    onDoubleClick: () -> Unit,
    onRightClick: (() -> Unit)? = null
) {
    val code = bonusPoint?.code ?: "BP$bonusPointId"
    
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        isIncluded -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else -> Color.Transparent
    }
    
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        isIncluded -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
    } else {
        Modifier
    }
    
    val statusIndicator = when {
        isStart && isFinish -> "🏁"
        isStart -> "🚩"
        isFinish -> "🏁"
        isIncluded -> "✓"
        else -> "•"
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick,
                onLongClick = onRightClick
            )
            .background(backgroundColor)
            .padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = statusIndicator,
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
        
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isIncluded || isStart || isFinish || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
        
        if (bonusPoint?.points != null) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${bonusPoint.points}",
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}
