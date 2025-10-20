package org.showpage.rallydesktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.showpage.rallyserver.ui.UiMember

@OptIn(ExperimentalMaterial3Api::class)

/**
 * Home screen - main application screen after login.
 * Will contain navigation to Rally Planning, Ride Planning, and Scoring.
 */
@Composable
fun HomeScreen(
    user: UiMember,
    onLogout: () -> Unit,
    onNavigateToRallyPlanning: () -> Unit,
    onNavigateToRidePlanning: () -> Unit,
    onNavigateToScoring: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Top bar with user info and logout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Welcome, ${user.email}",
                style = MaterialTheme.typography.headlineSmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onNavigateToSettings) {
                    Text("Settings")
                }
                Button(onClick = onLogout) {
                    Text("Logout")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Main content - navigation cards
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Rally Planning Card
            Card(
                onClick = onNavigateToRallyPlanning,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = "Rally Planning",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Create and manage rallies, bonus points, and combinations",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Ride Planning Card
            Card(
                onClick = onNavigateToRidePlanning,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = "Ride Planning",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Plan your route and optimize bonus point collection",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Scoring Card
            Card(
                onClick = onNavigateToScoring,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = "Scoring",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Score rider submissions and manage rally results",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
