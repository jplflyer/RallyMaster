package org.showpage.rallydesktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.showpage.rallydesktop.service.RallyServerClient
import org.showpage.rallyserver.ui.CreateRallyRequest
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val logger = LoggerFactory.getLogger("RallyFormScreen")

/**
 * Screen for creating a new rally.
 * Shows a form with basic rally information fields.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RallyFormScreen(
    serverClient: RallyServerClient,
    onRallyCreated: (Int) -> Unit,  // Rally ID
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // Form state
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDateText by remember { mutableStateOf("") }
    var endDateText by remember { mutableStateOf("") }
    var locationCity by remember { mutableStateOf("") }
    var locationState by remember { mutableStateOf("") }
    var locationCountry by remember { mutableStateOf("US") }
    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }

    // Visibility toggles
    var isPublic by remember { mutableStateOf(true) }
    var pointsPublic by remember { mutableStateOf(false) }
    var ridersPublic by remember { mutableStateOf(true) }
    var organizersPublic by remember { mutableStateOf(true) }

    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Create Rally",
                style = MaterialTheme.typography.headlineMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null

                            // Parse dates
                            val startDate = try {
                                if (startDateText.isNotBlank()) LocalDate.parse(startDateText, dateFormatter) else null
                            } catch (e: DateTimeParseException) {
                                errorMessage = "Invalid start date format. Use YYYY-MM-DD"
                                isLoading = false
                                return@launch
                            }

                            val endDate = try {
                                if (endDateText.isNotBlank()) LocalDate.parse(endDateText, dateFormatter) else null
                            } catch (e: DateTimeParseException) {
                                errorMessage = "Invalid end date format. Use YYYY-MM-DD"
                                isLoading = false
                                return@launch
                            }

                            // Parse coordinates
                            val latitude = try {
                                if (latitudeText.isNotBlank()) latitudeText.toFloat() else null
                            } catch (e: NumberFormatException) {
                                errorMessage = "Invalid latitude"
                                isLoading = false
                                return@launch
                            }

                            val longitude = try {
                                if (longitudeText.isNotBlank()) longitudeText.toFloat() else null
                            } catch (e: NumberFormatException) {
                                errorMessage = "Invalid longitude"
                                isLoading = false
                                return@launch
                            }

                            // Validate required fields
                            if (name.isBlank()) {
                                errorMessage = "Rally name is required"
                                isLoading = false
                                return@launch
                            }

                            if (description.isBlank()) {
                                errorMessage = "Description is required"
                                isLoading = false
                                return@launch
                            }

                            if (locationCity.isBlank()) {
                                errorMessage = "Location city is required"
                                isLoading = false
                                return@launch
                            }

                            if (locationState.isBlank()) {
                                errorMessage = "Location state/province is required"
                                isLoading = false
                                return@launch
                            }

                            // Create request
                            val request = CreateRallyRequest.builder()
                                .name(name)
                                .description(description)
                                .startDate(startDate)
                                .endDate(endDate)
                                .locationCity(locationCity)
                                .locationState(locationState)
                                .locationCountry(locationCountry.takeIf { it.isNotBlank() })
                                .latitude(latitude)
                                .longitude(longitude)
                                .isPublic(isPublic)
                                .pointsPublic(pointsPublic)
                                .ridersPublic(ridersPublic)
                                .organizersPublic(organizersPublic)
                                .build()

                            logger.info("Creating rally: {}", name)

                            serverClient.createRally(request).fold(
                                onSuccess = { rally ->
                                    logger.info("Rally created successfully: ID={}", rally.id)
                                    isLoading = false
                                    onRallyCreated(rally.id!!)
                                },
                                onFailure = { error ->
                                    logger.error("Failed to create rally", error)
                                    errorMessage = "Failed to create rally: ${error.message}"
                                    isLoading = false
                                }
                            )
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Create Rally")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage ?: "",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Form fields (scrollable)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Basic Information
            Text(
                text = "Basic Information",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Rally Name *") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description *") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                minLines = 3,
                maxLines = 5
            )

            // Dates
            Text(
                text = "Dates",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = startDateText,
                    onValueChange = { startDateText = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    singleLine = true,
                    placeholder = { Text("2025-06-01") }
                )

                OutlinedTextField(
                    value = endDateText,
                    onValueChange = { endDateText = it },
                    label = { Text("End Date (YYYY-MM-DD)") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    singleLine = true,
                    placeholder = { Text("2025-06-03") }
                )
            }

            // Location
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = locationCity,
                onValueChange = { locationCity = it },
                label = { Text("City *") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = locationState,
                    onValueChange = { locationState = it },
                    label = { Text("State/Province *") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    singleLine = true
                )

                OutlinedTextField(
                    value = locationCountry,
                    onValueChange = { locationCountry = it },
                    label = { Text("Country Code") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    singleLine = true,
                    placeholder = { Text("US") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = latitudeText,
                    onValueChange = { latitudeText = it },
                    label = { Text("Latitude") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    singleLine = true,
                    placeholder = { Text("42.8864") }
                )

                OutlinedTextField(
                    value = longitudeText,
                    onValueChange = { longitudeText = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    singleLine = true,
                    placeholder = { Text("-78.8784") }
                )
            }

            // Visibility Settings
            Text(
                text = "Visibility Settings",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isPublic,
                    onCheckedChange = { isPublic = it },
                    enabled = !isLoading
                )
                Text("Rally is publicly visible")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = pointsPublic,
                    onCheckedChange = { pointsPublic = it },
                    enabled = !isLoading
                )
                Text("Bonus points are publicly visible")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = ridersPublic,
                    onCheckedChange = { ridersPublic = it },
                    enabled = !isLoading
                )
                Text("Rider list is publicly visible")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = organizersPublic,
                    onCheckedChange = { organizersPublic = it },
                    enabled = !isLoading
                )
                Text("Organizer list is publicly visible")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
