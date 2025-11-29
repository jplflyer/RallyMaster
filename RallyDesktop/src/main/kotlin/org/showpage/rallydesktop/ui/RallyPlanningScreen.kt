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
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )

                        MapPanel(
                            rally = rally!!,
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            BonusPointsList(
                rallyId = rallyId,
                serverClient = serverClient
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CombinationsList(
                rallyId = rallyId,
                serverClient = serverClient
            )
        }
    }
}

/**
 * Bottom-right panel: Map (placeholder for Phase 5)
 */
@Composable
fun MapPanel(
    rally: UiRally,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Map",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Interactive Map\n(Phase 5)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
