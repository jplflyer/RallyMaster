package org.showpage.rallydesktop.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.showpage.rallydesktop.service.RallyServerClient
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
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var ride by remember { mutableStateOf<UiRide?>(null) }
    var rally by remember { mutableStateOf<UiRally?>(null) }
    var routes by remember { mutableStateOf(emptyList<UiRoute>()) }
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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
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
                            onCollapse = { sidebarCollapsed = true },
                            width = sidebarWidth,
                            onWidthChange = { sidebarWidth = it },
                            onRoutesChanged = { updatedRoutes ->
                                routes = updatedRoutes
                            },
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

/**
 * Sidebar containing ride info, routes/legs/waypoints tree, and bonus points
 */
@Composable
fun RidePlanSidebar(
    ride: UiRide,
    rally: UiRally?,
    routes: List<UiRoute>,
    serverClient: RallyServerClient,
    onCollapse: () -> Unit,
    width: Dp,
    onWidthChange: (Dp) -> Unit,
    onRoutesChanged: (List<UiRoute>) -> Unit,
    modifier: Modifier = Modifier
) {
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
                        onRoutesChanged = onRoutesChanged,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Bonus Points section (only if rally-associated)
                if (rally != null) {
                    HorizontalDivider()

                    CollapsibleSection(
                        title = "Bonus Points",
                        isCollapsed = bonusPointsCollapsed,
                        onToggleCollapse = { bonusPointsCollapsed = !bonusPointsCollapsed },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        CompactBonusPointsList(
                            rallyId = rally.id!!,
                            serverClient = serverClient,
                            selectedBonusPointId = null,
                            onBonusPointSelected = {},
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
@Composable
fun RoutesTree(
    rideId: Int,
    routes: List<UiRoute>,
    serverClient: RallyServerClient,
    onRoutesChanged: (List<UiRoute>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(8.dp),
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
                    Button(onClick = {
                        // TODO: Add route creation dialog
                    }) {
                        Text("Create Route")
                    }
                }
            }
        } else {
            routes.forEach { route ->
                RouteItem(
                    route = route,
                    serverClient = serverClient,
                    onRouteChanged = { updatedRoute ->
                        onRoutesChanged(routes.map { if (it.id == updatedRoute.id) updatedRoute else it })
                    }
                )
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
    onRouteChanged: (UiRoute) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    var legs by remember { mutableStateOf(emptyList<UiRideLeg>()) }
    val scope = rememberCoroutineScope()

    // Load legs for this route
    LaunchedEffect(route.id) {
        if (route.id != null) {
            serverClient.listRideLegs(route.id).fold(
                onSuccess = { loadedLegs ->
                    logger.info("Loaded {} legs for route {}", loadedLegs.size, route.id)
                    legs = loadedLegs
                },
                onFailure = { error ->
                    logger.error("Failed to load legs for route {}", route.id, error)
                }
            )
        }
    }

    Column {
        // Route header
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
            Text(
                text = route.name ?: "Route",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            if (route.isPrimary == true) {
                Text(
                    text = "★",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Legs (if expanded)
        if (isExpanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp)
            ) {
                legs.forEach { leg ->
                    RideLegItem(
                        leg = leg,
                        serverClient = serverClient,
                        onLegChanged = { updatedLeg ->
                            legs = legs.map { if (it.id == updatedLeg.id) updatedLeg else it }
                        }
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
            }
        }
    }
}

/**
 * Single leg item showing waypoints
 */
@Composable
fun RideLegItem(
    leg: UiRideLeg,
    serverClient: RallyServerClient,
    onLegChanged: (UiRideLeg) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    var waypoints by remember { mutableStateOf(emptyList<UiWaypoint>()) }

    // Load waypoints for this leg
    LaunchedEffect(leg.id) {
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

    Column {
        // Leg header
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
            Text(
                text = leg.name ?: "Leg",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Waypoints (if expanded)
        if (isExpanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp)
            ) {
                waypoints.forEach { waypoint ->
                    WaypointItem(
                        waypoint = waypoint,
                        onDelete = {
                            // TODO: Implement deletion
                        }
                    )
                }

                if (waypoints.isEmpty()) {
                    Text(
                        text = "No waypoints",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Single waypoint item
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WaypointItem(
    waypoint: UiWaypoint,
    onDelete: () -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showContextMenu = true }
                )
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
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Context menu
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    onDelete()
                    showContextMenu = false
                }
            )
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
                                onRideCreated(ride.id!!)
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
