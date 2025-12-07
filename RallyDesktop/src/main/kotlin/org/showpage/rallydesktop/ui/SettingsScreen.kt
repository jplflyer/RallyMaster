package org.showpage.rallydesktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Settings screen for configuring application preferences.
 */
@Composable
fun SettingsScreen(
    currentServerUrl: String,
    onServerUrlChange: (String) -> Unit,
    onBack: () -> Unit
) {
    var serverUrl by remember { mutableStateOf(currentServerUrl) }
    var showSaveConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )
            Button(onClick = onBack) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Server URL setting
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Server Configuration",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Configure the RallyServer URL for API communication",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("http://localhost:8080") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            onServerUrlChange(serverUrl)
                            showSaveConfirmation = true
                        },
                        enabled = serverUrl != currentServerUrl && serverUrl.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }

                if (showSaveConfirmation) {
                    Text(
                        text = "✓ Server URL saved successfully",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(3000)
                        showSaveConfirmation = false
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Additional settings can be added here
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "RallyMaster Desktop v1.0.0",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "A desktop application for motorcycle rally planning, ride planning, and scoring.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
