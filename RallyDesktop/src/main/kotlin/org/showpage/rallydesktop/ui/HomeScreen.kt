package org.showpage.rallydesktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.showpage.rallydesktop.service.PreferencesService
import org.showpage.rallydesktop.service.RallyServerClient
import org.showpage.rallyserver.ui.UiMember
import org.showpage.rallyserver.ui.UiRally
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val logger = LoggerFactory.getLogger("HomeScreen")

/**
 * Home screen - displays "My Rallies" list and action buttons.
 */
@Composable
fun HomeScreen(
    user: UiMember,
    serverClient: RallyServerClient,
    preferencesService: PreferencesService,
    onNavigateToCreateRally: () -> Unit,
    onNavigateToRallyPlanning: (Int) -> Unit,
    onNavigateToRidePlanning: (Int) -> Unit,
    onNavigateToScoring: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var rallies by remember { mutableStateOf<List<UiRally>>(emptyList()) }
    var rides by remember { mutableStateOf<List<org.showpage.rallyserver.ui.UiRide>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAllPastRallies by remember { mutableStateOf(preferencesService.getShowAllPastRallies()) }
    var showCreateRideDialog by remember { mutableStateOf(false) }

    // Load rallies and rides on first composition
    LaunchedEffect(showAllPastRallies) {
        // Check if authenticated before making API call
        if (!serverClient.isAuthenticated()) {
            logger.warn("HomeScreen loaded but client not authenticated yet")
            errorMessage = "Not authenticated"
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null

        logger.info("Loading rallies, showAllPastRallies={}, isAuthenticated={}", showAllPastRallies, serverClient.isAuthenticated())

        // For "My Rallies", filter by date unless showAllPastRallies is checked
        val from = if (!showAllPastRallies) {
            // Show rallies ending today or later
            LocalDate.now().toString()
        } else null

        serverClient.searchRallies(from = from, size = 100).fold(
            onSuccess = { page ->
                logger.info("Loaded {} rallies", page.content().size)
                rallies = page.content()
            },
            onFailure = { error ->
                logger.error("Failed to load rallies", error)
                errorMessage = "Failed to load rallies: ${error.message}"
            }
        )

        // Load rides
        serverClient.listRides().fold(
            onSuccess = { loadedRides ->
                logger.info("Loaded {} rides", loadedRides.size)
                rides = loadedRides
            },
            onFailure = { error ->
                logger.error("Failed to load rides", error)
                // Don't overwrite rally error message if it exists
                if (errorMessage == null) {
                    errorMessage = "Failed to load rides: ${error.message}"
                }
            }
        )

        isLoading = false
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Header with user info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Welcome, ${user.email}",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { /* TODO: Browse Rallies */ }) {
                Text("Browse Rallies")
            }
            Button(onClick = onNavigateToCreateRally) {
                Text("Create Rally")
            }
            Button(onClick = { showCreateRideDialog = true }) {
                Text("Create Ride")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                                serverClient.searchRallies(size = 100).fold(
                                    onSuccess = { page ->
                                        rallies = page.content()
                                        isLoading = false
                                    },
                                    onFailure = { error ->
                                        errorMessage = "Failed to load rallies: ${error.message}"
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
            else -> {
                // Show both rallies and rides in a scrollable column
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // My Rides section
                    item {
                        Text(
                            text = "My Rides",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (rides.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No rides yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(rides) { ride ->
                            RideListItem(
                                ride = ride,
                                onClick = {
                                    logger.info("Navigating to ride planning: {}", ride.name)
                                    ride.id?.let { onNavigateToRidePlanning(it) }
                                }
                            )
                        }
                    }

                    // Spacer between sections
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // My Rallies section header with checkbox
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "My Rallies",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = showAllPastRallies,
                                    onCheckedChange = { checked ->
                                        showAllPastRallies = checked
                                        preferencesService.setShowAllPastRallies(checked)
                                    }
                                )
                                Text("All Past Rallies", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    if (rallies.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No rallies found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(rallies) { rally ->
                            RallyListItem(
                                rally = rally,
                                onClick = {
                                    logger.info("Navigating to rally planning: {}", rally.name)
                                    rally.id?.let { onNavigateToRallyPlanning(it) }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Create Ride Dialog
        if (showCreateRideDialog) {
            CreateRideDialog(
                serverClient = serverClient,
                onDismiss = { showCreateRideDialog = false },
                onRideCreated = { rideId ->
                    showCreateRideDialog = false
                    onNavigateToRidePlanning(rideId)
                }
            )
        }
    }
}

@Composable
fun RallyListItem(
    rally: UiRally,
    onClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Rally name
            Text(
                text = rally.name ?: "Unnamed Rally",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

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
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Description (if present)
            if (!rally.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = rally.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun RideListItem(
    ride: org.showpage.rallyserver.ui.UiRide,
    onClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            // Ride name
            Text(
                text = ride.name ?: "Unnamed Ride",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Rally association (if any)
            if (ride.rallyId != null) {
                Text(
                    text = "Rally-associated ride",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "Standalone ride",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expected dates
            if (ride.expectedStart != null || ride.expectedEnd != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val dateText = buildString {
                    if (ride.expectedStart != null) {
                        append(ride.expectedStart.format(dateFormatter))
                        if (ride.expectedEnd != null) {
                            append(" - ")
                            append(ride.expectedEnd.format(dateFormatter))
                        }
                    } else if (ride.expectedEnd != null) {
                        append("Ends: ")
                        append(ride.expectedEnd.format(dateFormatter))
                    }
                }
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Description (if present)
            if (!ride.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ride.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }
        }
    }
}
