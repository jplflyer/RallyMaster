package org.showpage.rallydesktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.showpage.rallydesktop.service.RallyServerClient
import org.showpage.rallyserver.ui.UiRally
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
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var rally by remember { mutableStateOf<UiRally?>(null) }
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
                isLoading = false
            },
            onFailure = { error ->
                logger.error("Failed to load rally", error)
                errorMessage = "Failed to load rally: ${error.message}"
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

                // 4-panel layout
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Top row: Rally Info + Bonus Points
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RallyInfoPanel(
                            rally = rally!!,
                            onEditRally = { onEditRally(rallyId) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )

                        BonusPointsPanel(
                            rallyId = rallyId,
                            serverClient = serverClient,
                            selectedBonusPointId = selectedBonusPointId,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }

                    // Bottom row: Combinations + Map
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CombinationsPanel(
                            rallyId = rallyId,
                            serverClient = serverClient,
                            selectedCombinationId = selectedCombinationId,
                            onCombinationSelected = { comboId ->
                                // Clear bonus point selection when combo is selected
                                selectedBonusPointId = null
                                selectedCombinationId = comboId
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )

                        MapPanel(
                            rallyId = rallyId,
                            rally = rally!!,
                            serverClient = serverClient,
                            selectedBonusPointId = selectedBonusPointId,
                            selectedCombinationId = selectedCombinationId,
                            onBonusPointSelected = { bonusPointId ->
                                selectedBonusPointId = bonusPointId
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
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
