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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.showpage.rallydesktop.service.RallyServerClient
import org.showpage.rallyserver.ui.CreateRideRequest
import org.showpage.rallyserver.ui.UiRally
import org.showpage.rallyserver.ui.UiRide
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter

private val logger = LoggerFactory.getLogger("RallyPlanningScreen")

/**
 * Rally Planning workspace with 4-panel layout:
 * - Top-left: Rally info (read-only summary)
 * - Top-right: Bonus Points list
 * - Bottom-left: Combinations list
 * - Bottom-right: Map
 */
@Composable
fun RallyPlanningScreen(
    rallyId: Int,
    serverClient: RallyServerClient,
    onEditRally: (Int) -> Unit,
    onNavigateToRidePlanning: (Int) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var rally by remember { mutableStateOf<UiRally?>(null) }
    var rideForRally by remember { mutableStateOf<UiRide?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load rally data on first composition
    LaunchedEffect(rallyId) {
        isLoading = true
        errorMessage = null

        logger.info("Loading rally with ID: {}", rallyId)

        serverClient.getRally(rallyId).fold(
            onSuccess = { loadedRally ->
                logger.info("Rally loaded: {}", loadedRally.name)
                rally = loadedRally
            },
            onFailure = { error ->
                logger.error("Failed to load rally", error)
                errorMessage = "Failed to load rally: ${error.message}"
            }
        )

        // Check if user has an existing ride for this rally
        serverClient.getRideForRally(rallyId).fold(
            onSuccess = { existingRide ->
                rideForRally = existingRide
                if (existingRide != null) {
                    logger.info("Found existing ride for rally: {}", existingRide.name)
                }
            },
            onFailure = { error ->
                logger.error("Failed to check for existing ride", error)
            }
        )

        isLoading = false
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
                text = "Rally Planning",
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
                                serverClient.getRally(rallyId).fold(
                                    onSuccess = { loadedRally ->
                                        rally = loadedRally
                                        isLoading = false
                                    },
                                    onFailure = { error ->
                                        errorMessage = "Failed to load rally: ${error.message}"
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
            rally != null -> {
                // Shared state for selected bonus point (for map-to-list communication)
                var selectedBonusPointId by remember { mutableStateOf<Int?>(null) }
                var selectedCombinationId by remember { mutableStateOf<Int?>(null) }

                // Load combinations to determine which combo the selected BP belongs to
                var combinations by remember { mutableStateOf(emptyList<org.showpage.rallyserver.ui.UiCombination>()) }

                LaunchedEffect(rallyId) {
                    serverClient.listCombinations(rallyId).fold(
                        onSuccess = { combos ->
                            combinations = combos
                        },
                        onFailure = { error ->
                            logger.error("Failed to load combinations for combo selection", error)
                        }
                    )
                }

                // When a bonus point is selected, find the first combo it belongs to
                LaunchedEffect(selectedBonusPointId) {
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

                // New layout: Collapsible sidebar + Map
                var sidebarCollapsed by remember { mutableStateOf(false) }
                var sidebarWidth by remember { mutableStateOf(300.dp) }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Left sidebar
                    if (!sidebarCollapsed) {
                        CollapsibleSidebar(
                            rally = rally!!,
                            rallyId = rallyId,
                            serverClient = serverClient,
                            selectedBonusPointId = selectedBonusPointId,
                            selectedCombinationId = selectedCombinationId,
                            rideForRally = rideForRally,
                            onBonusPointSelected = { bonusPointId ->
                                selectedBonusPointId = bonusPointId
                            },
                            onCombinationSelected = { comboId ->
                                // Clear bonus point selection when combo is selected
                                selectedBonusPointId = null
                                selectedCombinationId = comboId
                            },
                            onEditRally = { onEditRally(rallyId) },
                            onPlanRide = {
                                scope.launch {
                                    val request = CreateRideRequest.builder()
                                        .name("${rally!!.name} Ride")
                                        .rallyId(rallyId)
                                        .build()
                                    serverClient.createRide(request).fold(
                                        onSuccess = { newRide ->
                                            logger.info("Created ride for rally: {}", newRide.name)
                                            rideForRally = newRide
                                            onNavigateToRidePlanning(newRide.id!!)
                                        },
                                        onFailure = { error ->
                                            logger.error("Failed to create ride for rally", error)
                                        }
                                    )
                                }
                            },
                            onGoToRide = { rideId ->
                                onNavigateToRidePlanning(rideId)
                            },
                            onCollapse = { sidebarCollapsed = true },
                            width = sidebarWidth,
                            onWidthChange = { sidebarWidth = it },
                            modifier = Modifier.fillMaxHeight()
                        )
                    } else {
                        // Collapsed sidebar - just show expand button
                        CollapsedSidebar(
                            onExpand = { sidebarCollapsed = false },
                            modifier = Modifier.fillMaxHeight()
                        )
                    }

                    // Map takes remaining space
                    MapPanel(
                        rallyId = rallyId,
                        rally = rally!!,
                        serverClient = serverClient,
                        selectedBonusPointId = selectedBonusPointId,
                        selectedCombinationId = selectedCombinationId,
                        onBonusPointSelected = { bonusPointId ->
                            selectedBonusPointId = bonusPointId
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Top-left panel: Rally basic info display (read-only summary)
 */
@Composable
fun RallyInfoPanel(
    rally: UiRally,
    onEditRally: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rally Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = onEditRally,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Edit")
                }
            }

            HorizontalDivider()

            // Rally name
            InfoRow(label = "Name", value = rally.name ?: "Unnamed Rally")

            // Dates
            val dateText = buildString {
                if (rally.startDate != null) {
                    append(rally.startDate.format(dateFormatter))
                    if (rally.endDate != null && rally.endDate != rally.startDate) {
                        append(" - ")
                        append(rally.endDate.format(dateFormatter))
                    }
                }
            }
            if (dateText.isNotEmpty()) {
                InfoRow(label = "Dates", value = dateText)
            }

            // Location
            val location = buildString {
                if (!rally.locationCity.isNullOrBlank()) {
                    append(rally.locationCity)
                }
                if (!rally.locationState.isNullOrBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(rally.locationState)
                }
                if (!rally.locationCountry.isNullOrBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(rally.locationCountry)
                }
            }
            if (location.isNotEmpty()) {
                InfoRow(label = "Location", value = location)
            }

            // Coordinates
            if (rally.latitude != null && rally.longitude != null) {
                InfoRow(
                    label = "Coordinates",
                    value = "${rally.latitude}, ${rally.longitude}"
                )
            }

            // Description
            if (!rally.description.isNullOrBlank()) {
                Column {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rally.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            HorizontalDivider()

            // Visibility settings
            Text(
                text = "Visibility",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            VisibilityRow("Rally", rally.isPublic)
            VisibilityRow("Bonus Points", rally.pointsPublic)
            VisibilityRow("Riders", rally.ridersPublic)
            VisibilityRow("Organizers", rally.organizersPublic)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun VisibilityRow(label: String, isPublic: Boolean?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = if (isPublic == true) "Public" else "Private",
            style = MaterialTheme.typography.bodySmall,
            color = if (isPublic == true)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Top-right panel: Bonus Points list
 */
@Composable
fun BonusPointsPanel(
    rallyId: Int,
    serverClient: RallyServerClient,
    selectedBonusPointId: Int?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            BonusPointsList(
                rallyId = rallyId,
                serverClient = serverClient,
                selectedBonusPointId = selectedBonusPointId
            )
        }
    }
}

/**
 * Bottom-left panel: Combinations list
 */
@Composable
fun CombinationsPanel(
    rallyId: Int,
    serverClient: RallyServerClient,
    selectedCombinationId: Int?,
    onCombinationSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CombinationsList(
                rallyId = rallyId,
                serverClient = serverClient,
                selectedCombinationId = selectedCombinationId,
                onCombinationSelected = onCombinationSelected
            )
        }
    }
}

/**
 * Bottom-right panel: Map showing bonus points
 */
@Composable
fun MapPanel(
    rallyId: Int,
    rally: UiRally,
    serverClient: RallyServerClient,
    selectedBonusPointId: Int?,
    selectedCombinationId: Int?,
    onBonusPointSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var bonusPoints by remember { mutableStateOf(emptyList<org.showpage.rallyserver.ui.UiBonusPoint>()) }
    var combinations by remember { mutableStateOf(emptyList<org.showpage.rallyserver.ui.UiCombination>()) }
    var isLoading by remember { mutableStateOf(false) }

    // Load bonus points and combinations for the map
    LaunchedEffect(rallyId) {
        isLoading = true

        // Load bonus points
        serverClient.listBonusPoints(rallyId).fold(
            onSuccess = { points ->
                bonusPoints = points
                logger.info("Loaded {} bonus points for map", points.size)
                if (points.isNotEmpty()) {
                    points.take(3).forEach { bp ->
                        logger.info("  BP: id={}, code={}, name={}, lat={}, lon={}, color={}",
                            bp.id, bp.code, bp.name, bp.latitude, bp.longitude, bp.markerColor)
                    }
                }
            },
            onFailure = { error ->
                logger.error("Failed to load bonus points for map", error)
            }
        )

        // Load combinations
        serverClient.listCombinations(rallyId).fold(
            onSuccess = { combos ->
                combinations = combos
                logger.info("Loaded {} combinations for map", combos.size)
            },
            onFailure = { error ->
                logger.error("Failed to load combinations for map", error)
            }
        )

        isLoading = false
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Map",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            // Map viewer
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                MapViewer(
                    bonusPoints = bonusPoints,
                    combinations = combinations,
                    centerLatitude = rally.latitude?.toDouble(),
                    centerLongitude = rally.longitude?.toDouble(),
                    selectedBonusPointId = selectedBonusPointId,
                    selectedCombinationId = selectedCombinationId,
                    onBonusPointClicked = onBonusPointSelected,
                    onBonusPointDragged = { bonusPointId, newLat, newLon ->
                        // Update the bonus point's coordinates on the server
                        scope.launch {
                            logger.info("Updating bonus point {} to new coordinates: ({}, {})",
                                bonusPointId, newLat, newLon)

                            // Find the bonus point to update
                            val bonusPoint = bonusPoints.find { it.id == bonusPointId }
                            if (bonusPoint != null) {
                                val updateRequest = org.showpage.rallyserver.ui.UpdateBonusPointRequest.builder()
                                    .code(bonusPoint.code)
                                    .name(bonusPoint.name)
                                    .description(bonusPoint.description)
                                    .latitude(newLat)
                                    .longitude(newLon)
                                    .address(bonusPoint.address)
                                    .points(bonusPoint.points)
                                    .required(bonusPoint.required)
                                    .repeatable(bonusPoint.repeatable)
                                    .build()

                                serverClient.updateBonusPoint(bonusPointId, updateRequest).fold(
                                    onSuccess = { updatedPoint ->
                                        logger.info("Successfully updated bonus point {}", bonusPoint.code)
                                        // Update local state to reflect the change
                                        bonusPoints = bonusPoints.map {
                                            if (it.id == bonusPointId) updatedPoint else it
                                        }
                                    },
                                    onFailure = { error ->
                                        logger.error("Failed to update bonus point {}", bonusPoint.code, error)
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Collapsible sidebar containing rally info, bonus points, and combinations
 */
@Composable
fun CollapsibleSidebar(
    rally: UiRally,
    rallyId: Int,
    serverClient: RallyServerClient,
    selectedBonusPointId: Int?,
    selectedCombinationId: Int?,
    rideForRally: UiRide?,
    onBonusPointSelected: (Int?) -> Unit,
    onCombinationSelected: (Int?) -> Unit,
    onEditRally: () -> Unit,
    onPlanRide: () -> Unit,
    onGoToRide: (Int) -> Unit,
    onCollapse: () -> Unit,
    width: Dp,
    onWidthChange: (Dp) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        // Main sidebar content
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
                        text = "Rally Planning",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onCollapse) {
                        Text("◀")  // Left arrow to collapse
                    }
                }

                HorizontalDivider()

                // Three sections: Rally Info, Bonus Points, Combinations
                var rallyInfoCollapsed by remember { mutableStateOf(false) }
                var bonusPointsCollapsed by remember { mutableStateOf(false) }
                var combinationsCollapsed by remember { mutableStateOf(false) }

                // Rally Info section
                CollapsibleSection(
                    title = "Rally Info",
                    isCollapsed = rallyInfoCollapsed,
                    onToggleCollapse = { rallyInfoCollapsed = !rallyInfoCollapsed },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CompactRallyInfo(
                        rally = rally,
                        rideForRally = rideForRally,
                        onEditRally = onEditRally,
                        onPlanRide = onPlanRide,
                        onGoToRide = onGoToRide,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }

                HorizontalDivider()

                // Bonus Points section
                CollapsibleSection(
                    title = "Bonus Points",
                    isCollapsed = bonusPointsCollapsed,
                    onToggleCollapse = { bonusPointsCollapsed = !bonusPointsCollapsed },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    CompactBonusPointsList(
                        rallyId = rallyId,
                        serverClient = serverClient,
                        selectedBonusPointId = selectedBonusPointId,
                        onBonusPointSelected = onBonusPointSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                HorizontalDivider()

                // Combinations section
                CollapsibleSection(
                    title = "Combinations",
                    isCollapsed = combinationsCollapsed,
                    onToggleCollapse = { combinationsCollapsed = !combinationsCollapsed },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    CompactCombinationsList(
                        rallyId = rallyId,
                        serverClient = serverClient,
                        selectedCombinationId = selectedCombinationId,
                        onCombinationSelected = onCombinationSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Resizable divider (simplified for now - just visual)
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

/**
 * Collapsed sidebar showing just an expand button
 */
@Composable
fun CollapsedSidebar(
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(40.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            IconButton(onClick = onExpand) {
                Text("▶", style = MaterialTheme.typography.titleLarge)  // Right arrow to expand
            }
        }
    }
}

/**
 * Generic collapsible section with title and collapse button
 */
@Composable
fun CollapsibleSection(
    title: String,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleCollapse)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isCollapsed) "▼" else "▲",
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Section content
        if (!isCollapsed) {
            content()
        }
    }
}

/**
 * Compact Rally Info display
 */
@Composable
fun CompactRallyInfo(
    rally: UiRally,
    rideForRally: UiRide?,
    onEditRally: () -> Unit,
    onPlanRide: () -> Unit,
    onGoToRide: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rally.name ?: "Unnamed Rally",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onEditRally,
                modifier = Modifier.size(24.dp)
            ) {
                Text("✏️", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Dates
        if (rally.startDate != null) {
            Text(
                text = "${rally.startDate.format(dateFormatter)}" +
                      if (rally.endDate != null && rally.endDate != rally.startDate)
                          " - ${rally.endDate.format(dateFormatter)}"
                      else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Location
        val location = buildString {
            if (!rally.locationCity.isNullOrBlank()) append(rally.locationCity)
            if (!rally.locationState.isNullOrBlank()) {
                if (isNotEmpty()) append(", ")
                append(rally.locationState)
            }
        }
        if (location.isNotEmpty()) {
            Text(
                text = location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Ride button - either "Plan My Ride" or "Go to My Ride"
        if (rideForRally != null) {
            Button(
                onClick = { onGoToRide(rideForRally.id!!) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Go to My Ride", style = MaterialTheme.typography.labelMedium)
            }
        } else {
            OutlinedButton(
                onClick = onPlanRide,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Plan My Ride", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Compact Bonus Points list with +/- buttons
 */
@Composable
fun CompactBonusPointsList(
    rallyId: Int,
    serverClient: RallyServerClient,
    selectedBonusPointId: Int?,
    onBonusPointSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var bonusPoints by remember { mutableStateOf<List<org.showpage.rallyserver.ui.UiBonusPoint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var editingPoint by remember { mutableStateOf<org.showpage.rallyserver.ui.UiBonusPoint?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deletingPoint by remember { mutableStateOf<org.showpage.rallyserver.ui.UiBonusPoint?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPoint by remember { mutableStateOf<org.showpage.rallyserver.ui.UiBonusPoint?>(null) }

    // Load bonus points on first composition
    LaunchedEffect(rallyId) {
        isLoading = true
        errorMessage = null

        serverClient.listBonusPoints(rallyId).fold(
            onSuccess = { points ->
                logger.info("Loaded {} bonus points", points.size)
                bonusPoints = points
                isLoading = false
            },
            onFailure = { error ->
                logger.error("Failed to load bonus points", error)
                errorMessage = "Failed to load bonus points: ${error.message}"
                isLoading = false
            }
        )
    }

    Column(
        modifier = modifier
    ) {
        // Header with +/- buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add button
                IconButton(
                    onClick = {
                        editingPoint = null
                        showDialog = true
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("+", style = MaterialTheme.typography.labelLarge)
                }

                // Delete button (disabled if nothing selected)
                IconButton(
                    onClick = {
                        val selected = bonusPoints.find { it.id == selectedBonusPointId }
                        if (selected != null) {
                            deletingPoint = selected
                            showDeleteConfirm = true
                        }
                    },
                    modifier = Modifier.size(24.dp),
                    enabled = selectedBonusPointId != null
                ) {
                    Text("-", style = MaterialTheme.typography.labelLarge)
                }

                // Menu button for import
                IconButton(
                    onClick = { showContextMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("⋮", style = MaterialTheme.typography.labelLarge)
                }
            }

            Text(
                text = "${bonusPoints.size} points",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Content
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
            bonusPoints.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No bonus points",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()

                // Scroll to selected item only if not visible
                LaunchedEffect(selectedBonusPointId) {
                    if (selectedBonusPointId != null) {
                        val index = bonusPoints.indexOfFirst { it.id == selectedBonusPointId }
                        if (index >= 0) {
                            val layoutInfo = listState.layoutInfo
                            val visibleItems = layoutInfo.visibleItemsInfo
                            val isVisible = visibleItems.any { it.index == index }

                            if (!isVisible) {
                                listState.animateScrollToItem(index)
                            }
                        }
                    }
                }

                androidx.compose.foundation.lazy.LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    items(
                        count = bonusPoints.size,
                        key = { index -> bonusPoints[index].id ?: index }
                    ) { index ->
                        val point = bonusPoints[index]
                        CompactBonusPointItem(
                            bonusPoint = point,
                            isSelected = point.id == selectedBonusPointId,
                            onClick = {
                                onBonusPointSelected(point.id)
                            },
                            onDoubleClick = {
                                editingPoint = point
                                showDialog = true
                            },
                            onRightClick = {
                                contextMenuPoint = point
                                showContextMenu = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Context menu dropdown
    if (showContextMenu) {
        androidx.compose.material3.DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Add Bonus Point") },
                onClick = {
                    editingPoint = null
                    showDialog = true
                    showContextMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Import CSV") },
                onClick = {
                    showImportDialog = true
                    showContextMenu = false
                }
            )
            if (contextMenuPoint != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Edit ${contextMenuPoint!!.code}") },
                    onClick = {
                        editingPoint = contextMenuPoint
                        showDialog = true
                        showContextMenu = false
                        contextMenuPoint = null
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete ${contextMenuPoint!!.code}") },
                    onClick = {
                        deletingPoint = contextMenuPoint
                        showDeleteConfirm = true
                        showContextMenu = false
                        contextMenuPoint = null
                    }
                )
            }
        }
    }

    // Show dialog for add/edit
    if (showDialog) {
        BonusPointDialog(
            bonusPoint = editingPoint,
            onDismiss = {
                showDialog = false
                editingPoint = null
            },
            onSave = { code, name, description, latitude, longitude, address, points, required, repeatable ->
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    showDialog = false

                    if (editingPoint == null) {
                        // Create new bonus point
                        val request = org.showpage.rallyserver.ui.CreateBonusPointRequest.builder()
                            .code(code)
                            .name(name)
                            .description(description)
                            .latitude(latitude)
                            .longitude(longitude)
                            .address(address)
                            .points(points)
                            .required(required)
                            .repeatable(repeatable)
                            .build()

                        serverClient.createBonusPoint(rallyId, request).fold(
                            onSuccess = { newPoint ->
                                logger.info("Bonus point created: {}", newPoint.code)
                                bonusPoints = bonusPoints + newPoint
                                isLoading = false
                            },
                            onFailure = { error ->
                                logger.error("Failed to create bonus point", error)
                                errorMessage = "Failed to create bonus point: ${error.message}"
                                isLoading = false
                            }
                        )
                    } else {
                        // Update existing bonus point
                        val request = org.showpage.rallyserver.ui.UpdateBonusPointRequest.builder()
                            .code(code)
                            .name(name)
                            .description(description)
                            .latitude(latitude)
                            .longitude(longitude)
                            .address(address)
                            .points(points)
                            .required(required)
                            .repeatable(repeatable)
                            .build()

                        serverClient.updateBonusPoint(editingPoint!!.id!!, request).fold(
                            onSuccess = { updatedPoint ->
                                logger.info("Bonus point updated: {}", updatedPoint.code)
                                bonusPoints = bonusPoints.map { if (it.id == updatedPoint.id) updatedPoint else it }
                                isLoading = false
                                editingPoint = null
                            },
                            onFailure = { error ->
                                logger.error("Failed to update bonus point", error)
                                errorMessage = "Failed to update bonus point: ${error.message}"
                                isLoading = false
                            }
                        )
                    }
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirm && deletingPoint != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
                deletingPoint = null
            },
            title = { Text("Delete Bonus Point") },
            text = { Text("Delete '${deletingPoint!!.code}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val pointToDelete = deletingPoint!!
                            showDeleteConfirm = false
                            isLoading = true

                            serverClient.deleteBonusPoint(pointToDelete.id!!).fold(
                                onSuccess = {
                                    logger.info("Bonus point deleted: {}", pointToDelete.code)
                                    bonusPoints = bonusPoints.filter { it.id != pointToDelete.id }
                                    deletingPoint = null
                                    isLoading = false
                                },
                                onFailure = { error ->
                                    logger.error("Failed to delete bonus point", error)
                                    errorMessage = "Failed to delete: ${error.message}"
                                    deletingPoint = null
                                    isLoading = false
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
                OutlinedButton(
                    onClick = {
                        showDeleteConfirm = false
                        deletingPoint = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // CSV Import dialog
    if (showImportDialog) {
        CsvImportDialog(
            rallyId = rallyId,
            serverClient = serverClient,
            onDismiss = { showImportDialog = false },
            onImportComplete = { importedPoints ->
                scope.launch {
                    isLoading = true
                    serverClient.listBonusPoints(rallyId).fold(
                        onSuccess = { points ->
                            bonusPoints = points
                            isLoading = false
                            showImportDialog = false
                        },
                        onFailure = { error ->
                            errorMessage = "Failed to reload: ${error.message}"
                            isLoading = false
                            showImportDialog = false
                        }
                    )
                }
            }
        )
    }
}

/**
 * Compact item for bonus point in the list
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactBonusPointItem(
    bonusPoint: org.showpage.rallyserver.ui.UiBonusPoint,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onRightClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick,
                onLongClick = onRightClick
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = bonusPoint.code ?: "?",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(60.dp)
        )

        Text(
            text = bonusPoint.name ?: "",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        if (bonusPoint.points != null) {
            Text(
                text = "${bonusPoint.points}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Compact Combinations tree view with +/- buttons
 */
@Composable
fun CompactCombinationsList(
    rallyId: Int,
    serverClient: RallyServerClient,
    selectedCombinationId: Int?,
    onCombinationSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var combinations by remember { mutableStateOf<List<org.showpage.rallyserver.ui.UiCombination>>(emptyList()) }
    var bonusPoints by remember { mutableStateOf<List<org.showpage.rallyserver.ui.UiBonusPoint>>(emptyList()) }
    var expandedCombos by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var editingCombo by remember { mutableStateOf<org.showpage.rallyserver.ui.UiCombination?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deletingCombo by remember { mutableStateOf<org.showpage.rallyserver.ui.UiCombination?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuCombo by remember { mutableStateOf<org.showpage.rallyserver.ui.UiCombination?>(null) }

    // Load combinations and bonus points on first composition
    LaunchedEffect(rallyId) {
        isLoading = true
        errorMessage = null

        // Load combinations
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

        // Load bonus points to get codes
        serverClient.listBonusPoints(rallyId).fold(
            onSuccess = { points ->
                logger.info("Loaded {} bonus points for combo display", points.size)
                bonusPoints = points
            },
            onFailure = { error ->
                logger.error("Failed to load bonus points for combo display", error)
            }
        )

        isLoading = false
    }

    Column(
        modifier = modifier
    ) {
        // Header with +/- buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add button
                IconButton(
                    onClick = {
                        editingCombo = null
                        showDialog = true
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("+", style = MaterialTheme.typography.labelLarge)
                }

                // Delete button (disabled if nothing selected)
                IconButton(
                    onClick = {
                        val selected = combinations.find { it.id == selectedCombinationId }
                        if (selected != null) {
                            deletingCombo = selected
                            showDeleteConfirm = true
                        }
                    },
                    modifier = Modifier.size(24.dp),
                    enabled = selectedCombinationId != null
                ) {
                    Text("-", style = MaterialTheme.typography.labelLarge)
                }

                // Menu button for import
                IconButton(
                    onClick = { showContextMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("⋮", style = MaterialTheme.typography.labelLarge)
                }
            }

            Text(
                text = "${combinations.size} combos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Content
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
                        text = "No combinations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()

                // Scroll to selected item only if not visible
                LaunchedEffect(selectedCombinationId) {
                    if (selectedCombinationId != null) {
                        val index = combinations.indexOfFirst { it.id == selectedCombinationId }
                        if (index >= 0) {
                            val layoutInfo = listState.layoutInfo
                            val visibleItems = layoutInfo.visibleItemsInfo
                            val isVisible = visibleItems.any { it.index == index }

                            if (!isVisible) {
                                listState.animateScrollToItem(index)
                            }
                        }
                    }
                }

                androidx.compose.foundation.lazy.LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    items(
                        count = combinations.size,
                        key = { index -> combinations[index].id ?: index }
                    ) { index ->
                        val combo = combinations[index]
                        val isExpanded = expandedCombos.contains(combo.id)

                        // Create a map of bonus point IDs to codes for quick lookup
                        val bonusPointMap = bonusPoints.associateBy { it.id }

                        Column {
                            CompactCombinationItem(
                                combination = combo,
                                isSelected = combo.id == selectedCombinationId,
                                isExpanded = isExpanded,
                                markerColor = combo.markerColor,
                                onClick = {
                                    onCombinationSelected(combo.id)
                                },
                                onToggleExpand = {
                                    expandedCombos = if (isExpanded) {
                                        expandedCombos - combo.id!!
                                    } else {
                                        expandedCombos + combo.id!!
                                    }
                                },
                                onDoubleClick = {
                                    editingCombo = combo
                                    showDialog = true
                                },
                                onRightClick = {
                                    contextMenuCombo = combo
                                    showContextMenu = true
                                }
                            )

                            // Show bonus points if expanded
                            if (isExpanded && combo.combinationPoints?.isNotEmpty() == true) {
                                Column(
                                    modifier = Modifier.padding(start = 16.dp)
                                ) {
                                    combo.combinationPoints.forEach { cp ->
                                        val bonusPoint = bonusPointMap[cp.bonusPointId]
                                        val code = bonusPoint?.code ?: "BP${cp.bonusPointId}"
                                        Text(
                                            text = "• $code",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
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

    // Context menu dropdown
    if (showContextMenu) {
        androidx.compose.material3.DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Add Combination") },
                onClick = {
                    editingCombo = null
                    showDialog = true
                    showContextMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Import CSV") },
                onClick = {
                    showImportDialog = true
                    showContextMenu = false
                }
            )
            if (contextMenuCombo != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Edit ${contextMenuCombo!!.code}") },
                    onClick = {
                        editingCombo = contextMenuCombo
                        showDialog = true
                        showContextMenu = false
                        contextMenuCombo = null
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete ${contextMenuCombo!!.code}") },
                    onClick = {
                        deletingCombo = contextMenuCombo
                        showDeleteConfirm = true
                        showContextMenu = false
                        contextMenuCombo = null
                    }
                )
            }
        }
    }

    // Show dialog for add/edit
    if (showDialog) {
        CombinationDialog(
            combination = editingCombo,
            onDismiss = {
                showDialog = false
                editingCombo = null
            },
            onSave = { code, name, description, points, requiresAll, numRequired ->
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    showDialog = false

                    if (editingCombo == null) {
                        // Create new combination
                        val request = org.showpage.rallyserver.ui.CreateCombinationRequest.builder()
                            .code(code)
                            .name(name)
                            .description(description)
                            .points(points)
                            .requiresAll(requiresAll)
                            .numRequired(numRequired)
                            .combinationPoints(emptyList())
                            .build()

                        serverClient.createCombination(rallyId, request).fold(
                            onSuccess = { newCombo ->
                                logger.info("Combination created: {}", newCombo.code)
                                combinations = combinations + newCombo
                                isLoading = false
                            },
                            onFailure = { error ->
                                logger.error("Failed to create combination", error)
                                errorMessage = "Failed to create combination: ${error.message}"
                                isLoading = false
                            }
                        )
                    } else {
                        // Update existing combination
                        val request = org.showpage.rallyserver.ui.UpdateCombinationRequest.builder()
                            .code(code)
                            .name(name)
                            .description(description)
                            .points(points)
                            .requiresAll(requiresAll)
                            .numRequired(numRequired)
                            .build()

                        serverClient.updateCombination(editingCombo!!.id!!, request).fold(
                            onSuccess = { updatedCombo ->
                                logger.info("Combination updated: {}", updatedCombo.code)
                                combinations = combinations.map { if (it.id == updatedCombo.id) updatedCombo else it }
                                isLoading = false
                                editingCombo = null
                            },
                            onFailure = { error ->
                                logger.error("Failed to update combination", error)
                                errorMessage = "Failed to update combination: ${error.message}"
                                isLoading = false
                            }
                        )
                    }
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirm && deletingCombo != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
                deletingCombo = null
            },
            title = { Text("Delete Combination") },
            text = { Text("Delete '${deletingCombo!!.code}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val comboToDelete = deletingCombo!!
                            showDeleteConfirm = false
                            isLoading = true

                            serverClient.deleteCombination(comboToDelete.id!!).fold(
                                onSuccess = {
                                    logger.info("Combination deleted: {}", comboToDelete.code)
                                    combinations = combinations.filter { it.id != comboToDelete.id }
                                    deletingCombo = null
                                    isLoading = false
                                },
                                onFailure = { error ->
                                    logger.error("Failed to delete combination", error)
                                    errorMessage = "Failed to delete: ${error.message}"
                                    deletingCombo = null
                                    isLoading = false
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
                OutlinedButton(
                    onClick = {
                        showDeleteConfirm = false
                        deletingCombo = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // CSV Import dialog
    if (showImportDialog) {
        CombinationCsvImportDialog(
            rallyId = rallyId,
            serverClient = serverClient,
            onDismiss = { showImportDialog = false },
            onImportComplete = {
                scope.launch {
                    isLoading = true
                    serverClient.listCombinations(rallyId).fold(
                        onSuccess = { combos ->
                            combinations = combos
                            isLoading = false
                            showImportDialog = false
                        },
                        onFailure = { error ->
                            errorMessage = "Failed to reload: ${error.message}"
                            isLoading = false
                            showImportDialog = false
                        }
                    )
                }
            }
        )
    }
}

/**
 * Compact item for combination in the tree
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactCombinationItem(
    combination: org.showpage.rallyserver.ui.UiCombination,
    isSelected: Boolean,
    isExpanded: Boolean,
    markerColor: String?,
    onClick: () -> Unit,
    onToggleExpand: () -> Unit,
    onDoubleClick: () -> Unit,
    onRightClick: () -> Unit
) {
    // Parse the marker color from hex string
    val parsedColor = remember(markerColor) {
        try {
            if (markerColor != null) {
                val hex = if (markerColor.startsWith("#")) markerColor.substring(1) else markerColor
                Color(
                    red = Integer.parseInt(hex.substring(0, 2), 16) / 255f,
                    green = Integer.parseInt(hex.substring(2, 4), 16) / 255f,
                    blue = Integer.parseInt(hex.substring(4, 6), 16) / 255f
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick,
                onLongClick = onRightClick
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expand/collapse indicator
        Text(
            text = if (isExpanded) "▼" else "▶",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .clickable(onClick = onToggleExpand)
                .padding(4.dp)
        )

        Text(
            text = combination.code ?: "?",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = parsedColor ?: MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(60.dp)
        )

        Text(
            text = combination.name ?: "",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = parsedColor ?: Color.Unspecified,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        if (combination.points != null) {
            Text(
                text = "${combination.points}",
                style = MaterialTheme.typography.bodySmall,
                color = parsedColor ?: MaterialTheme.colorScheme.secondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
