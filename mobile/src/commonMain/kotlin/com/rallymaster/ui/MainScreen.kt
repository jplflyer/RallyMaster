package com.rallymaster.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rallymaster.data.DesktopFileManager
import com.rallymaster.data.RallyRepository
import com.rallymaster.model.Rally
import com.rallymaster.model.RallyStatus
import kotlinx.coroutines.launch

/**
 * Main screen for Rally Master Desktop application.
 *
 * Shows list of existing rallies with options to create new rallies,
 * edit existing ones, and manage rally data using local JSON storage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    // Initialize repository and state
    val repository = remember { RallyRepository(DesktopFileManager()) }
    val rallies by repository.rallies.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCreateScreen by remember { mutableStateOf(false) }

    // Initialize repository on screen load
    LaunchedEffect(Unit) {
        try {
            repository.initialize()
            isLoading = false
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }
    }

    // Show appropriate screen
    if (showCreateScreen) {
        CreateRallyScreen(
            onRallyCreated = {
                showCreateScreen = false
                // Refresh the rally list
                kotlinx.coroutines.MainScope().launch {
                    try {
                        repository.initialize()
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            },
            onBackPressed = {
                showCreateScreen = false
            },
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Rally Master Desktop",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        showCreateScreen = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Text("+", style = MaterialTheme.typography.headlineMedium)
                }
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            ) {
                when {
                    isLoading -> {
                        LoadingContent()
                    }
                    error != null -> {
                        ErrorContent(error = error!!) {
                            // Retry loading
                            isLoading = true
                            error = null
                            kotlinx.coroutines.MainScope().launch {
                                try {
                                    repository.initialize()
                                    isLoading = false
                                } catch (e: Exception) {
                                    error = e.message
                                    isLoading = false
                                }
                            }
                        }
                    }
                    rallies.isEmpty() -> {
                        EmptyStateContent(
                            onCreateRally = { showCreateScreen = true },
                        )
                    }
                    else -> {
                        RallyListContent(
                            rallies = rallies,
                            onRallyClick = { rally ->
                                // TODO: Navigate to EditRallyScreen in T022
                                println("Rally clicked: ${rally.name}")
                            },
                            onEditClick = { rally ->
                                // TODO: Navigate to EditRallyScreen in T022
                                println("Edit rally: ${rally.name}")
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Loading rallies...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorContent(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Error loading rallies",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyStateContent(
    onCreateRally: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Welcome to Rally Master",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Desktop-First Motorcycle Rally Management",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "No Rallies Yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Create your first rally to get started.\nAll data is stored locally on your computer.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onCreateRally,
                ) {
                    Text("Create Your First Rally")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Feature highlights
        Card(
            modifier = Modifier.padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
            ) {
                Text(
                    text = "Features",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                Spacer(modifier = Modifier.height(12.dp))

                val features = listOf(
                    "Create and manage motorcycle rallies",
                    "Add bonus points with GPS coordinates",
                    "Design scoring combinations",
                    "Local JSON storage (offline-first)",
                    "Export rally data for sharing",
                )

                features.forEach { feature ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RallyListContent(
    rallies: List<Rally>,
    onRallyClick: (Rally) -> Unit,
    onEditClick: (Rally) -> Unit,
) {
    Column {
        Text(
            text = "Your Rallies (${rallies.size})",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(rallies) { rally ->
                RallyCard(
                    rally = rally,
                    onClick = { onRallyClick(rally) },
                    onEditClick = { onEditClick(rally) },
                )
            }
        }
    }
}

@Composable
private fun RallyCard(
    rally: Rally,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = rally.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                    )

                    if (rally.location.isNotEmpty()) {
                        Text(
                            text = rally.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Status badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (rally.status) {
                            RallyStatus.DRAFT -> MaterialTheme.colorScheme.surfaceVariant
                            RallyStatus.PUBLISHED -> MaterialTheme.colorScheme.primaryContainer
                            RallyStatus.ACTIVE -> MaterialTheme.colorScheme.errorContainer
                            RallyStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                            RallyStatus.CANCELLED -> MaterialTheme.colorScheme.outline
                        },
                    ),
                ) {
                    Text(
                        text = rally.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = when (rally.status) {
                            RallyStatus.DRAFT -> MaterialTheme.colorScheme.onSurfaceVariant
                            RallyStatus.PUBLISHED -> MaterialTheme.colorScheme.onPrimaryContainer
                            RallyStatus.ACTIVE -> MaterialTheme.colorScheme.onErrorContainer
                            RallyStatus.COMPLETED -> MaterialTheme.colorScheme.onTertiaryContainer
                            RallyStatus.CANCELLED -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (rally.description.isNotEmpty()) {
                Text(
                    text = rally.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Rally details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    if (rally.bonusPointIds.isNotEmpty()) {
                        Text(
                            text = "${rally.bonusPointIds.size} Bonus Points",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    if (rally.combinationIds.isNotEmpty()) {
                        Text(
                            text = "${rally.combinationIds.size} Combinations",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }

                OutlinedButton(
                    onClick = onEditClick,
                ) {
                    Text("Edit")
                }
            }
        }
    }
}
