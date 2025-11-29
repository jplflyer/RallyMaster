package org.showpage.rallydesktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import org.showpage.rallydesktop.service.RallyServerClient
import org.showpage.rallyserver.ui.CreateBonusPointRequest
import org.showpage.rallyserver.ui.UiBonusPoint
import org.showpage.rallyserver.ui.UpdateBonusPointRequest
import org.slf4j.LoggerFactory
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private val logger = LoggerFactory.getLogger("BonusPointComponents")

/**
 * Dialog for creating or editing a bonus point.
 */
@Composable
fun BonusPointDialog(
    bonusPoint: UiBonusPoint? = null,  // null for create, populated for edit
    onDismiss: () -> Unit,
    onSave: (code: String, name: String, description: String?, latitude: Double?, longitude: Double?, address: String?, points: Int?, required: Boolean, repeatable: Boolean) -> Unit
) {
    val isEdit = bonusPoint != null

    var code by remember { mutableStateOf(bonusPoint?.code ?: "") }
    var name by remember { mutableStateOf(bonusPoint?.name ?: "") }
    var description by remember { mutableStateOf(bonusPoint?.description ?: "") }
    var latitudeText by remember { mutableStateOf(bonusPoint?.latitude?.toString() ?: "") }
    var longitudeText by remember { mutableStateOf(bonusPoint?.longitude?.toString() ?: "") }
    var address by remember { mutableStateOf(bonusPoint?.address ?: "") }
    var pointsText by remember { mutableStateOf(bonusPoint?.points?.toString() ?: "") }
    var required by remember { mutableStateOf(bonusPoint?.required ?: false) }
    var repeatable by remember { mutableStateOf(bonusPoint?.repeatable ?: false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(600.dp)
                .heightIn(max = 700.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    text = if (isEdit) "Edit Bonus Point" else "Add Bonus Point",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

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
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Form fields (scrollable)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Code
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Code *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("BP001") }
                    )

                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Statue of Liberty") }
                    )

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        placeholder = { Text("Photo with Liberty Island in background") }
                    )

                    // Points
                    OutlinedTextField(
                        value = pointsText,
                        onValueChange = { pointsText = it },
                        label = { Text("Points") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("100") }
                    )

                    // Coordinates
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = latitudeText,
                            onValueChange = { latitudeText = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("40.6892") }
                        )

                        OutlinedTextField(
                            value = longitudeText,
                            onValueChange = { longitudeText = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("-74.0445") }
                        )
                    }

                    // Address
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Liberty Island, New York, NY 10004") }
                    )

                    // Checkboxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = required,
                            onCheckedChange = { required = it }
                        )
                        Text("Required")

                        Spacer(modifier = Modifier.width(24.dp))

                        Checkbox(
                            checked = repeatable,
                            onCheckedChange = { repeatable = it }
                        )
                        Text("Repeatable")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            // Validation
                            if (code.isBlank()) {
                                errorMessage = "Code is required"
                                return@Button
                            }
                            if (name.isBlank()) {
                                errorMessage = "Name is required"
                                return@Button
                            }

                            // Parse numeric fields
                            val latitude = try {
                                if (latitudeText.isNotBlank()) latitudeText.toDouble() else null
                            } catch (e: NumberFormatException) {
                                errorMessage = "Invalid latitude"
                                return@Button
                            }

                            val longitude = try {
                                if (longitudeText.isNotBlank()) longitudeText.toDouble() else null
                            } catch (e: NumberFormatException) {
                                errorMessage = "Invalid longitude"
                                return@Button
                            }

                            val points = try {
                                if (pointsText.isNotBlank()) pointsText.toInt() else null
                            } catch (e: NumberFormatException) {
                                errorMessage = "Invalid points value"
                                return@Button
                            }

                            onSave(
                                code,
                                name,
                                description.takeIf { it.isNotBlank() },
                                latitude,
                                longitude,
                                address.takeIf { it.isNotBlank() },
                                points,
                                required,
                                repeatable
                            )
                        }
                    ) {
                        Text(if (isEdit) "Save" else "Create")
                    }
                }
            }
        }
    }
}

/**
 * List of bonus points with add/edit/delete functionality.
 */
@Composable
fun BonusPointsList(
    rallyId: Int,
    serverClient: RallyServerClient,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var bonusPoints by remember { mutableStateOf<List<UiBonusPoint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var editingPoint by remember { mutableStateOf<UiBonusPoint?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deletingPoint by remember { mutableStateOf<UiBonusPoint?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    // Load bonus points on first composition
    LaunchedEffect(rallyId) {
        isLoading = true
        errorMessage = null

        logger.info("Loading bonus points for rally: {}", rallyId)

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
        modifier = modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bonus Points",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        editingPoint = null
                        showDialog = true
                    },
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Add Point")
                }

                OutlinedButton(
                    onClick = {
                        showImportDialog = true
                    },
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Import CSV")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Loading/error/content states
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
                                serverClient.listBonusPoints(rallyId).fold(
                                    onSuccess = { points ->
                                        bonusPoints = points
                                        isLoading = false
                                    },
                                    onFailure = { error ->
                                        errorMessage = "Failed to load bonus points: ${error.message}"
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
            bonusPoints.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No bonus points yet.\nClick 'Add Point' to create one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(bonusPoints) { point ->
                        BonusPointListItem(
                            bonusPoint = point,
                            onClick = {
                                editingPoint = point
                                showDialog = true
                            },
                            onDelete = {
                                deletingPoint = point
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
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
                        val request = CreateBonusPointRequest.builder()
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
                        val request = UpdateBonusPointRequest.builder()
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
            text = { Text("Are you sure you want to delete '${deletingPoint!!.code} - ${deletingPoint!!.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val pointToDelete = deletingPoint!!
                            showDeleteConfirm = false
                            isLoading = true
                            errorMessage = null

                            serverClient.deleteBonusPoint(pointToDelete.id!!).fold(
                                onSuccess = {
                                    logger.info("Bonus point deleted: {}", pointToDelete.code)
                                    bonusPoints = bonusPoints.filter { it.id != pointToDelete.id }
                                    deletingPoint = null
                                    isLoading = false
                                },
                                onFailure = { error ->
                                    logger.error("Failed to delete bonus point", error)
                                    errorMessage = "Failed to delete bonus point: ${error.message}"
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
                // Refresh the list
                bonusPoints = bonusPoints + importedPoints
                showImportDialog = false
            }
        )
    }
}

@Composable
fun BonusPointListItem(
    bonusPoint: UiBonusPoint,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bonusPoint.code ?: "???",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = bonusPoint.name ?: "Unnamed",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (bonusPoint.description?.isNotBlank() == true) {
                    Text(
                        text = bonusPoint.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (bonusPoint.points != null) {
                        Text(
                            text = "${bonusPoint.points} pts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (bonusPoint.required == true) {
                        Text(
                            text = "Required",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (bonusPoint.repeatable == true) {
                        Text(
                            text = "Repeatable",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            IconButton(
                onClick = { onDelete() }
            ) {
                Text("🗑️")  // Delete icon (using emoji for simplicity)
            }
        }
    }
}

/**
 * Data class representing a parsed bonus point from CSV
 */
data class CsvBonusPoint(
    val code: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Dialog for importing bonus points from CSV file
 */
@Composable
fun CsvImportDialog(
    rallyId: Int,
    serverClient: RallyServerClient,
    onDismiss: () -> Unit,
    onImportComplete: (List<UiBonusPoint>) -> Unit
) {
    val scope = rememberCoroutineScope()

    var selectedFile by remember { mutableStateOf<File?>(null) }
    var parsedPoints by remember { mutableStateOf<List<CsvBonusPoint>>(emptyList()) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf(0) }
    var importTotal by remember { mutableStateOf(0) }
    var importError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(800.dp)
                .heightIn(max = 700.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    text = "Import Bonus Points from CSV",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // File selection
                if (selectedFile == null && !isImporting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Select a CSV file with columns: NAME, LATITUDE, LONGITUDE",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Button(
                            onClick = {
                                val fileChooser = JFileChooser().apply {
                                    fileFilter = FileNameExtensionFilter("CSV Files", "csv")
                                    dialogTitle = "Select CSV File"
                                }

                                val result = fileChooser.showOpenDialog(null)
                                if (result == JFileChooser.APPROVE_OPTION) {
                                    val file = fileChooser.selectedFile
                                    selectedFile = file

                                    // Parse the CSV file
                                    try {
                                        val points = parseCsvFile(file)
                                        parsedPoints = points
                                        parseError = null
                                        logger.info("Parsed {} bonus points from CSV", points.size)
                                    } catch (e: Exception) {
                                        logger.error("Failed to parse CSV file", e)
                                        parseError = "Failed to parse CSV: ${e.message}"
                                        parsedPoints = emptyList()
                                    }
                                }
                            }
                        ) {
                            Text("Choose CSV File")
                        }

                        if (parseError != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = parseError ?: "",
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Preview parsed points
                if (parsedPoints.isNotEmpty() && !isImporting) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Found ${parsedPoints.size} bonus points:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(parsedPoints) { point ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = point.code,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.width(60.dp)
                                        )
                                        Text(
                                            text = point.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${point.latitude}, ${point.longitude}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Import progress
                if (isImporting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Importing bonus points...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Progress: $importProgress / $importTotal",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        LinearProgressIndicator(
                            progress = { if (importTotal > 0) importProgress.toFloat() / importTotal else 0f },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (importError != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = importError ?: "",
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isImporting
                    ) {
                        Text("Cancel")
                    }

                    if (parsedPoints.isNotEmpty() && !isImporting) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isImporting = true
                                    importTotal = parsedPoints.size
                                    importProgress = 0
                                    importError = null

                                    val importedPoints = mutableListOf<UiBonusPoint>()

                                    for (csvPoint in parsedPoints) {
                                        val request = CreateBonusPointRequest.builder()
                                            .code(csvPoint.code)
                                            .name(csvPoint.name)
                                            .latitude(csvPoint.latitude)
                                            .longitude(csvPoint.longitude)
                                            .required(false)
                                            .repeatable(false)
                                            .build()

                                        serverClient.createBonusPoint(rallyId, request).fold(
                                            onSuccess = { newPoint ->
                                                importedPoints.add(newPoint)
                                                importProgress++
                                                logger.info("Imported bonus point: {}", csvPoint.code)
                                            },
                                            onFailure = { error ->
                                                logger.error("Failed to import bonus point: {}", csvPoint.code, error)
                                                importError = "Failed to import ${csvPoint.code}: ${error.message}"
                                            }
                                        )
                                    }

                                    if (importError == null) {
                                        onImportComplete(importedPoints)
                                    } else {
                                        isImporting = false
                                    }
                                }
                            }
                        ) {
                            Text("Import ${parsedPoints.size} Points")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Parse a CSV file and extract bonus points.
 * Expected format: NAME,LATITUDE,LONGITUDE (with header row)
 */
private fun parseCsvFile(file: File): List<CsvBonusPoint> {
    val points = mutableListOf<CsvBonusPoint>()

    file.bufferedReader().use { reader ->
        val lines = reader.readLines()

        if (lines.isEmpty()) {
            throw IllegalArgumentException("CSV file is empty")
        }

        // Parse header to find column indices
        val header = lines.first().split(',').map { it.trim().uppercase() }
        val nameIndex = header.indexOf("NAME")
        val latIndex = header.indexOf("LATITUDE")
        val lonIndex = header.indexOf("LONGITUDE")

        if (nameIndex == -1 || latIndex == -1 || lonIndex == -1) {
            throw IllegalArgumentException("CSV must have NAME, LATITUDE, and LONGITUDE columns")
        }

        // Parse data rows
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            val columns = line.split(',').map { it.trim() }

            if (columns.size <= maxOf(nameIndex, latIndex, lonIndex)) {
                logger.warn("Skipping invalid row {}: {}", i, line)
                continue
            }

            try {
                val code = columns[nameIndex]
                val latitude = columns[latIndex].toDouble()
                val longitude = columns[lonIndex].toDouble()

                points.add(CsvBonusPoint(
                    code = code,
                    name = code,  // Use code as name since we don't have a separate name field
                    latitude = latitude,
                    longitude = longitude
                ))
            } catch (e: NumberFormatException) {
                logger.warn("Skipping row {} due to invalid number format: {}", i, line)
            }
        }
    }

    return points
}
