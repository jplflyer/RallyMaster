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
import androidx.compose.ui.window.DialogWindow
import java.awt.Dimension
import kotlinx.coroutines.launch
import org.showpage.rallydesktop.service.RallyServerClient
import org.showpage.rallyserver.ui.CreateCombinationPointRequest
import org.showpage.rallyserver.ui.CreateCombinationRequest
import org.showpage.rallyserver.ui.UiCombination
import org.showpage.rallyserver.ui.UpdateCombinationRequest
import org.slf4j.LoggerFactory
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private val logger = LoggerFactory.getLogger("CombinationComponents")

/**
 * Dialog for creating or editing a combination.
 */
@Composable
fun CombinationDialog(
    combination: UiCombination? = null,  // null for create, populated for edit
    onDismiss: () -> Unit,
    onSave: (
        code: String,
        name: String,
        description: String?,
        points: Int?,
        requiresAll: Boolean,
        numRequired: Int?
    ) -> Unit
) {
    val isEdit = combination != null

    var code by remember { mutableStateOf(combination?.code ?: "") }
    var name by remember { mutableStateOf(combination?.name ?: "") }
    var description by remember { mutableStateOf(combination?.description ?: "") }
    var pointsText by remember { mutableStateOf(combination?.points?.toString() ?: "") }
    var requiresAll by remember { mutableStateOf(combination?.requiresAll ?: true) }
    var numRequiredText by remember { mutableStateOf(combination?.numRequired?.toString() ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    DialogWindow(
        onCloseRequest = onDismiss,
        title = if (isEdit) "Edit Combination" else "Add Combination"
    ) {
        window.minimumSize = Dimension(600, 600)
        window.size = Dimension(600, 850)

        Card(
            modifier = Modifier.fillMaxSize(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
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
                        placeholder = { Text("COMBO1") }
                    )

                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Great Lakes Tour") }
                    )

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        placeholder = { Text("Visit all Great Lakes states") }
                    )

                    // Points
                    OutlinedTextField(
                        value = pointsText,
                        onValueChange = { pointsText = it },
                        label = { Text("Points") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("5000") }
                    )

                    // Requires All checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = requiresAll,
                            onCheckedChange = { requiresAll = it }
                        )
                        Text("Requires all bonus points")
                    }

                    // Number Required (only if not requiresAll)
                    if (!requiresAll) {
                        OutlinedTextField(
                            value = numRequiredText,
                            onValueChange = { numRequiredText = it },
                            label = { Text("Number Required") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("3") }
                        )
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
                            val points = try {
                                if (pointsText.isNotBlank()) pointsText.toInt() else null
                            } catch (e: NumberFormatException) {
                                errorMessage = "Invalid points value"
                                return@Button
                            }

                            val numRequired = if (!requiresAll) {
                                try {
                                    if (numRequiredText.isNotBlank()) numRequiredText.toInt() else null
                                } catch (e: NumberFormatException) {
                                    errorMessage = "Invalid number required value"
                                    return@Button
                                }
                            } else null

                            onSave(
                                code,
                                name,
                                description.takeIf { it.isNotBlank() },
                                points,
                                requiresAll,
                                numRequired
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
 * List of combinations with add/edit/delete functionality.
 */
@Composable
fun CombinationsList(
    rallyId: Int,
    serverClient: RallyServerClient,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var combinations by remember { mutableStateOf<List<UiCombination>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var editingCombination by remember { mutableStateOf<UiCombination?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deletingCombination by remember { mutableStateOf<UiCombination?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    // Load combinations on first composition
    LaunchedEffect(rallyId) {
        isLoading = true
        errorMessage = null

        logger.info("Loading combinations for rally: {}", rallyId)

        serverClient.listCombinations(rallyId).fold(
            onSuccess = { combos ->
                logger.info("Loaded {} combinations", combos.size)
                combinations = combos
                isLoading = false
            },
            onFailure = { error ->
                logger.error("Failed to load combinations", error)
                errorMessage = "Failed to load combinations: ${error.message}"
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
                text = "Combinations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        editingCombination = null
                        showDialog = true
                    },
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Add Combo")
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
                                serverClient.listCombinations(rallyId).fold(
                                    onSuccess = { combos ->
                                        combinations = combos
                                        isLoading = false
                                    },
                                    onFailure = { error ->
                                        errorMessage = "Failed to load combinations: ${error.message}"
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
            combinations.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No combinations yet.\nClick 'Add Combo' to create one.",
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
                    items(combinations) { combo ->
                        CombinationListItem(
                            combination = combo,
                            onClick = {
                                editingCombination = combo
                                showDialog = true
                            },
                            onDelete = {
                                deletingCombination = combo
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
        CombinationDialog(
            combination = editingCombination,
            onDismiss = {
                showDialog = false
                editingCombination = null
            },
            onSave = { code, name, description, points, requiresAll, numRequired ->
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    showDialog = false

                    if (editingCombination == null) {
                        // Create new combination
                        val request = CreateCombinationRequest.builder()
                            .code(code)
                            .name(name)
                            .description(description)
                            .points(points)
                            .requiresAll(requiresAll)
                            .numRequired(numRequired)
                            .combinationPoints(emptyList())  // Empty for now, can add later
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
                        val request = UpdateCombinationRequest.builder()
                            .code(code)
                            .name(name)
                            .description(description)
                            .points(points)
                            .requiresAll(requiresAll)
                            .numRequired(numRequired)
                            .build()

                        serverClient.updateCombination(editingCombination!!.id!!, request).fold(
                            onSuccess = { updatedCombo ->
                                logger.info("Combination updated: {}", updatedCombo.code)
                                combinations = combinations.map { if (it.id == updatedCombo.id) updatedCombo else it }
                                isLoading = false
                                editingCombination = null
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
    if (showDeleteConfirm && deletingCombination != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
                deletingCombination = null
            },
            title = { Text("Delete Combination") },
            text = { Text("Are you sure you want to delete '${deletingCombination!!.code} - ${deletingCombination!!.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val comboToDelete = deletingCombination!!
                            showDeleteConfirm = false
                            isLoading = true
                            errorMessage = null

                            serverClient.deleteCombination(comboToDelete.id!!).fold(
                                onSuccess = {
                                    logger.info("Combination deleted: {}", comboToDelete.code)
                                    combinations = combinations.filter { it.id != comboToDelete.id }
                                    deletingCombination = null
                                    isLoading = false
                                },
                                onFailure = { error ->
                                    logger.error("Failed to delete combination", error)
                                    errorMessage = "Failed to delete combination: ${error.message}"
                                    deletingCombination = null
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
                        deletingCombination = null
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
                // Reload the full list from server to ensure consistency
                scope.launch {
                    isLoading = true
                    serverClient.listCombinations(rallyId).fold(
                        onSuccess = { combos ->
                            combinations = combos
                            isLoading = false
                            showImportDialog = false
                        },
                        onFailure = { error ->
                            errorMessage = "Failed to reload combinations: ${error.message}"
                            isLoading = false
                            showImportDialog = false
                        }
                    )
                }
            }
        )
    }
}

@Composable
fun CombinationListItem(
    combination: UiCombination,
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
                        text = combination.code ?: "???",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = combination.name ?: "Unnamed",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (combination.description?.isNotBlank() == true) {
                    Text(
                        text = combination.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (combination.points != null) {
                        Text(
                            text = "${combination.points} pts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    val requirementText = if (combination.requiresAll == true) {
                        "All required"
                    } else {
                        "${combination.numRequired ?: 0} required"
                    }
                    Text(
                        text = requirementText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    if (combination.combinationPoints != null) {
                        Text(
                            text = "${combination.combinationPoints.size} bonus points",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
 * Data class representing a parsed combination from CSV
 */
data class CsvCombination(
    val code: String,
    val name: String,
    val points: Int?,
    val description: String?,
    val bonusPointCodes: List<String>
)

/**
 * Result of importing a combination with warnings about missing bonus points
 */
data class CombinationImportResult(
    val combination: CsvCombination,
    val missingBonusPoints: List<String>
)

/**
 * Dialog for importing combinations from CSV file
 */
@Composable
fun CombinationCsvImportDialog(
    rallyId: Int,
    serverClient: RallyServerClient,
    onDismiss: () -> Unit,
    onImportComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var selectedFile by remember { mutableStateOf<File?>(null) }
    var parsedCombinations by remember { mutableStateOf<List<CsvCombination>>(emptyList()) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf(0) }
    var importTotal by remember { mutableStateOf(0) }
    var importResults by remember { mutableStateOf<List<CombinationImportResult>>(emptyList()) }

    DialogWindow(
        onCloseRequest = onDismiss,
        title = "Import Combinations from CSV"
    ) {
        window.minimumSize = Dimension(800, 600)
        window.size = Dimension(800, 700)

        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    text = "Import Combinations from CSV",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // File selection
                if (selectedFile == null && !isImporting && importResults.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Select a CSV file with columns: CODE, NAME, POINTS, DESCRIPTION, [BONUS POINT CODES...]",
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
                                        val combos = parseCombinationsCsv(file)
                                        parsedCombinations = combos
                                        parseError = null
                                        logger.info("Parsed {} combinations from CSV", combos.size)
                                    } catch (e: Exception) {
                                        logger.error("Failed to parse CSV file", e)
                                        parseError = "Failed to parse CSV: ${e.message}"
                                        parsedCombinations = emptyList()
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

                // Preview parsed combinations
                if (parsedCombinations.isNotEmpty() && !isImporting && importResults.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Found ${parsedCombinations.size} combinations:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(parsedCombinations) { combo ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = combo.code,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.width(100.dp)
                                            )
                                            Text(
                                                text = combo.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (combo.points != null) {
                                                Text(
                                                    text = "${combo.points} pts",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                        if (combo.bonusPointCodes.isNotEmpty()) {
                                            Text(
                                                text = "${combo.bonusPointCodes.size} bonus points: ${combo.bonusPointCodes.joinToString(", ")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
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
                            text = "Importing combinations...",
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
                    }
                }

                // Import results
                if (importResults.isNotEmpty() && !isImporting) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Import Complete",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val hasWarnings = importResults.any { it.missingBonusPoints.isNotEmpty() }
                        if (hasWarnings) {
                            Text(
                                text = "Some combinations have missing bonus points:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(importResults.filter { it.missingBonusPoints.isNotEmpty() }) { result ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            Text(
                                                text = "${result.combination.code} - ${result.combination.name}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = "Missing: ${result.missingBonusPoints.joinToString(", ")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "All ${importResults.size} combinations imported successfully!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
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
                        onClick = {
                            if (importResults.isNotEmpty()) {
                                onImportComplete()
                            } else {
                                onDismiss()
                            }
                        },
                        enabled = !isImporting
                    ) {
                        Text(if (importResults.isNotEmpty()) "Done" else "Cancel")
                    }

                    if (parsedCombinations.isNotEmpty() && !isImporting && importResults.isEmpty()) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isImporting = true
                                    importTotal = parsedCombinations.size
                                    importProgress = 0

                                    // First, load existing combinations and bonus points
                                    val existingCombosResult = serverClient.listCombinations(rallyId)
                                    val existingCombos = existingCombosResult.getOrNull() ?: emptyList()
                                    val existingCombosByCode = existingCombos.associateBy { it.code }

                                    val bonusPointsResult = serverClient.listBonusPoints(rallyId)
                                    val bonusPoints = bonusPointsResult.getOrNull() ?: emptyList()
                                    val bonusPointsByCode = bonusPoints.associateBy { it.code }

                                    logger.info("Found {} existing combinations and {} bonus points", existingCombos.size, bonusPoints.size)

                                    val results = mutableListOf<CombinationImportResult>()

                                    for (csvCombo in parsedCombinations) {
                                        // Check which bonus points exist
                                        val missingBonusPoints = mutableListOf<String>()
                                        val validBonusPointIds = mutableListOf<Int>()

                                        for (bpCode in csvCombo.bonusPointCodes) {
                                            val bonusPoint = bonusPointsByCode[bpCode]
                                            if (bonusPoint != null && bonusPoint.id != null) {
                                                validBonusPointIds.add(bonusPoint.id)
                                            } else {
                                                missingBonusPoints.add(bpCode)
                                            }
                                        }

                                        results.add(CombinationImportResult(csvCombo, missingBonusPoints))

                                        // Create combination points list
                                        val combinationPoints = validBonusPointIds.map { bpId ->
                                            CreateCombinationPointRequest.builder()
                                                .bonusPointId(bpId)
                                                .required(true)
                                                .build()
                                        }

                                        val existingCombo = existingCombosByCode[csvCombo.code]

                                        if (existingCombo != null) {
                                            // Update existing combination
                                            val updateRequest = UpdateCombinationRequest.builder()
                                                .code(csvCombo.code)
                                                .name(csvCombo.name)
                                                .description(csvCombo.description)
                                                .points(csvCombo.points)
                                                .requiresAll(true)
                                                .numRequired(null)
                                                .build()

                                            serverClient.updateCombination(existingCombo.id!!, updateRequest).fold(
                                                onSuccess = { updatedCombo ->
                                                    logger.info("Updated combination: {}", csvCombo.code)
                                                    importProgress++
                                                },
                                                onFailure = { error ->
                                                    logger.error("Failed to update combination: {}", csvCombo.code, error)
                                                    importProgress++
                                                }
                                            )
                                        } else {
                                            // Create new combination
                                            val createRequest = CreateCombinationRequest.builder()
                                                .code(csvCombo.code)
                                                .name(csvCombo.name)
                                                .description(csvCombo.description)
                                                .points(csvCombo.points)
                                                .requiresAll(true)
                                                .numRequired(null)
                                                .combinationPoints(combinationPoints)
                                                .build()

                                            serverClient.createCombination(rallyId, createRequest).fold(
                                                onSuccess = { newCombo ->
                                                    logger.info("Created combination: {}", csvCombo.code)
                                                    importProgress++
                                                },
                                                onFailure = { error ->
                                                    logger.error("Failed to create combination: {}", csvCombo.code, error)
                                                    importProgress++
                                                }
                                            )
                                        }
                                    }

                                    importResults = results
                                    isImporting = false
                                    logger.info("Import complete: {} combinations processed", results.size)
                                }
                            }
                        ) {
                            Text("Import ${parsedCombinations.size} Combinations")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Parse a CSV file and extract combinations.
 * Expected format: CODE, NAME, POINTS, DESCRIPTION, [BONUS POINT CODES...]
 */
private fun parseCombinationsCsv(file: File): List<CsvCombination> {
    val combinations = mutableListOf<CsvCombination>()

    file.bufferedReader().use { reader ->
        val lines = reader.readLines()

        if (lines.isEmpty()) {
            throw IllegalArgumentException("CSV file is empty")
        }

        // Skip header row and parse data rows
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            val columns = line.split(',').map { it.trim() }

            if (columns.size < 2) {
                logger.warn("Skipping invalid row {}: {}", i, line)
                continue
            }

            try {
                val code = columns[0]
                val name = columns.getOrNull(1) ?: code
                val points = columns.getOrNull(2)?.toIntOrNull()
                val description = columns.getOrNull(3)?.takeIf { it.isNotBlank() }

                // All columns after index 3 are bonus point codes
                val bonusPointCodes = if (columns.size > 4) {
                    columns.subList(4, columns.size).filter { it.isNotBlank() }
                } else {
                    emptyList()
                }

                combinations.add(CsvCombination(
                    code = code,
                    name = name,
                    points = points,
                    description = description,
                    bonusPointCodes = bonusPointCodes
                ))
            } catch (e: Exception) {
                logger.warn("Skipping row {} due to error: {}", i, e.message)
            }
        }
    }

    return combinations
}
