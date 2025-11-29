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
import org.showpage.rallyserver.ui.CreateCombinationRequest
import org.showpage.rallyserver.ui.UiCombination
import org.showpage.rallyserver.ui.UpdateCombinationRequest
import org.slf4j.LoggerFactory

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
                    text = if (isEdit) "Edit Combination" else "Add Combination",
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

            Button(
                onClick = {
                    editingCombination = null
                    showDialog = true
                },
                modifier = Modifier.height(32.dp)
            ) {
                Text("Add Combo")
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
