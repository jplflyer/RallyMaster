package org.showpage.rallydesktop.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch
import org.showpage.rallydesktop.service.PreferencesService
import org.showpage.rallydesktop.service.RallyServerClient
import org.showpage.rallydesktop.service.WaypointSequencer
import org.showpage.rallyserver.ui.*
import org.slf4j.LoggerFactory
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
    var selectedLegId by remember { mutableStateOf<Int?>(null) }
    var waypointReloadTrigger by remember { mutableStateOf(0) }
    var showNoLegSelectedMessage by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

                // If rally-associated, load rally details
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
                            serverClient = serverClient,
                            selectedLegId = selectedLegId,
                            waypointReloadTrigger = waypointReloadTrigger,
                            onLegSelected = { legId -> selectedLegId = legId },
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

                    // Map panel (placeholder for now)
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Map View\n(Coming Soon)",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
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
    serverClient: RallyServerClient,
    selectedLegId: Int?,
    waypointReloadTrigger: Int,
    onLegSelected: (Int?) -> Unit,
    onCollapse: () -> Unit,
    width: Dp,
    onWidthChange: (Dp) -> Unit,
    onRoutesChanged: (List<UiRoute>) -> Unit,
    onReloadRoutes: () -> Unit,
    onWaypointAdded: () -> Unit,
    onNoLegSelected: () -> Unit,
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
                        waypointReloadTrigger = waypointReloadTrigger,
                        onLegSelected = onLegSelected,
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
                            routes = routes,
                            serverClient = serverClient,
                            selectedLegId = selectedLegId,
                            waypointReloadTrigger = waypointReloadTrigger,
                            onBonusPointsAdded = { count ->
                                logger.info("Added {} bonus points as waypoints", count)
                                onWaypointAdded()
                            },
                            onNoLegSelected = onNoLegSelected,
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
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = ride.name ?: "Unnamed Ride",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        // Rally association
        if (rally != null) {
            Text(
                text = "Rally: ${rally.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = "Standalone Ride",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Dates
        if (ride.expectedStart != null) {
            Text(
                text = "Start: ${ride.expectedStart.format(dateFormatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (ride.expectedEnd != null) {
            Text(
                text = "End: ${ride.expectedEnd.format(dateFormatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Description
        if (!ride.description.isNullOrBlank()) {
            Text(
                text = ride.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Routes tree showing routes -> legs -> waypoints
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoutesTree(
    rideId: Int,
    routes: List<UiRoute>,
    serverClient: RallyServerClient,
    selectedLegId: Int?,
    waypointReloadTrigger: Int,
    onLegSelected: (Int?) -> Unit,
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
                    waypointReloadTrigger = waypointReloadTrigger,
                    onLegSelected = onLegSelected,
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
    waypointReloadTrigger: Int,
    onLegSelected: (Int?) -> Unit,
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
                        waypointReloadTrigger = waypointReloadTrigger,
                        onSelect = { onLegSelected(leg.id) },
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
    waypointReloadTrigger: Int,
    onSelect: () -> Unit,
    onLegChanged: (UiRideLeg) -> Unit,
    onLegDeleted: () -> Unit,
    onWaypointChanged: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    var waypoints by remember { mutableStateOf(emptyList<UiWaypoint>()) }
    var selectedWaypointId by remember { mutableStateOf<Int?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
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
                    WaypointItem(
                        waypoint = waypoint,
                        isSelected = selectedWaypointId == waypoint.id,
                        onSelect = { selectedWaypointId = if (selectedWaypointId == waypoint.id) null else waypoint.id },
                        onDelete = {
                            scope.launch {
                                WaypointSequencer.deleteAndRenumber(waypoints, waypoint, serverClient).fold(
                                    onSuccess = { updated ->
                                        waypoints = updated
                                        selectedWaypointId = null
                                        onWaypointChanged()
                                    },
                                    onFailure = { error ->
                                        logger.error("Failed to delete waypoint", error)
                                    }
                                )
                            }
                        },
                        onMoveUp = if (index > 0) {
                            {
                                scope.launch {
                                    WaypointSequencer.moveUp(waypoints, waypoint, serverClient).fold(
                                        onSuccess = { updated -> waypoints = updated },
                                        onFailure = { error -> logger.error("Failed to move waypoint up", error) }
                                    )
                                }
                            }
                        } else null,
                        onMoveDown = if (index < waypoints.size - 1) {
                            {
                                scope.launch {
                                    WaypointSequencer.moveDown(waypoints, waypoint, serverClient).fold(
                                        onSuccess = { updated -> waypoints = updated },
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
}

/**
 * Single waypoint item
 */
@Composable
fun WaypointItem(
    waypoint: UiWaypoint,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    } else {
        Color.Transparent
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
        Text(
            text = waypoint.name ?: "Waypoint",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        
        if (isSelected) {
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
    routes: List<UiRoute>,
    serverClient: RallyServerClient,
    selectedLegId: Int?,
    waypointReloadTrigger: Int,
    onBonusPointsAdded: (Int) -> Unit,
    onNoLegSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    var combinations by remember { mutableStateOf<List<UiCombination>>(emptyList()) }
    var bonusPoints by remember { mutableStateOf<List<UiBonusPoint>>(emptyList()) }
    var includedBonusPointIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var expandedCombos by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
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
    
    LaunchedEffect(routes, waypointReloadTrigger) {
        val allBonusPointIds = mutableSetOf<Int>()
        for (route in routes) {
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
        logger.info("Found {} bonus points included in current route", allBonusPointIds.size)
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
    
    suspend fun addBonusPointAsWaypoint(bp: UiBonusPoint, legId: Int): Boolean {
        val existingWaypoints = serverClient.listWaypoints(legId).getOrElse { emptyList() }
        
        if (existingWaypoints.any { it.bonusPointId == bp.id }) {
            logger.info("Bonus point {} already in leg {}", bp.code, legId)
            return false
        }
        
        val nextSeq = WaypointSequencer.nextSequence(existingWaypoints)
        val waypointRequest = CreateWaypointRequest.builder()
            .name(bp.code ?: bp.name ?: "Waypoint")
            .bonusPointId(bp.id)
            .latitude(bp.latitude?.toFloat())
            .longitude(bp.longitude?.toFloat())
            .sequenceOrder(nextSeq)
            .build()
        
        return serverClient.createWaypoint(legId, waypointRequest).fold(
            onSuccess = {
                logger.info("Added bonus point {} as waypoint to leg {}", bp.code, legId)
                true
            },
            onFailure = { error ->
                logger.error("Failed to add bonus point {} as waypoint", bp.code, error)
                false
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
                text = "${combinations.size} combos",
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
            combinations.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No combinations defined",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                val listState = rememberLazyListState()
                
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
                        
                        Column {
                            RidePlanningComboItem(
                                combination = combo,
                                inclusionStatus = inclusionStatus,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    expandedCombos = if (isExpanded) {
                                        expandedCombos - combo.id!!
                                    } else {
                                        expandedCombos + combo.id!!
                                    }
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
                                }
                            )
                            
                            if (isExpanded && combo.combinationPoints?.isNotEmpty() == true) {
                                Column(modifier = Modifier.padding(start = 24.dp)) {
                                    combo.combinationPoints.forEach { cp ->
                                        val bp = bonusPointMap[cp.bonusPointId]
                                        val isIncluded = cp.bonusPointId in includedBonusPointIds
                                        
                                        RidePlanningBonusPointItem(
                                            bonusPoint = bp,
                                            bonusPointId = cp.bonusPointId,
                                            isIncluded = isIncluded,
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
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
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
    onToggleExpand: () -> Unit,
    onDoubleClick: () -> Unit
) {
    val backgroundColor = when (inclusionStatus) {
        ComboInclusionStatus.FULL -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ComboInclusionStatus.PARTIAL -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ComboInclusionStatus.NONE -> Color.Transparent
    }
    
    val textColor = when (inclusionStatus) {
        ComboInclusionStatus.FULL -> MaterialTheme.colorScheme.primary
        ComboInclusionStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary
        ComboInclusionStatus.NONE -> MaterialTheme.colorScheme.onSurface
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
                onClick = onToggleExpand,
                onDoubleClick = onDoubleClick
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
            fontWeight = if (inclusionStatus == ComboInclusionStatus.FULL) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            modifier = Modifier.width(60.dp)
        )
        
        Text(
            text = combination.name ?: "",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (inclusionStatus == ComboInclusionStatus.FULL) FontWeight.Bold else FontWeight.Normal,
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
    onDoubleClick: () -> Unit
) {
    val code = bonusPoint?.code ?: "BP$bonusPointId"
    
    val backgroundColor = if (isIncluded) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    } else {
        Color.Transparent
    }
    
    val textColor = if (isIncluded) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onDoubleClick = onDoubleClick
            )
            .background(backgroundColor)
            .padding(start = 8.dp, top = 2.dp, bottom = 2.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isIncluded) "✓" else "•",
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
        
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isIncluded) FontWeight.Bold else FontWeight.Normal,
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
