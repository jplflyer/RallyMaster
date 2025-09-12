package com.rallymaster.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rallymaster.data.DesktopFileManager
import com.rallymaster.data.RallyRepository
import com.rallymaster.model.Rally
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Create Rally screen for Rally Master Desktop application.
 *
 * Allows Rally Masters to create new rallies with all basic information
 * including dates, location, organizer details, and configuration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRallyScreen(
    onRallyCreated: () -> Unit,
    onBackPressed: () -> Unit,
) {
    val repository = remember { RallyRepository(DesktopFileManager()) }
    val coroutineScope = rememberCoroutineScope()

    // Form state
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var organizerName by remember { mutableStateOf("") }
    var organizerEmail by remember { mutableStateOf("") }
    var organizerPhone by remember { mutableStateOf("") }
    var registrationFee by remember { mutableStateOf("0.00") }
    var maxParticipants by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf("") }

    // Date/Time state (simplified - no time picker for now)
    var startDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var endDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Form validation
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Focus management for keyboard navigation
    val focusRequesters = remember {
        List(10) { FocusRequester() }
    }

    // Helper function to handle Tab navigation
    fun handleKeyNavigation(currentIndex: Int, keyEvent: androidx.compose.ui.input.key.KeyEvent): Boolean {
        if (keyEvent.type != KeyEventType.KeyDown) return false

        return when (keyEvent.key) {
            Key.Tab -> {
                val nextIndex = if (keyEvent.isShiftPressed) {
                    // Shift+Tab: go backwards
                    (currentIndex - 1 + focusRequesters.size) % focusRequesters.size
                } else {
                    // Tab: go forwards
                    (currentIndex + 1) % focusRequesters.size
                }
                focusRequesters[nextIndex].requestFocus()
                true
            }
            else -> false
        }
    }

    val isFormValid by remember {
        derivedStateOf {
            name.isNotBlank() &&
                location.isNotBlank() &&
                organizerName.isNotBlank() &&
                organizerEmail.isNotBlank() &&
                startDate != null &&
                endDate != null &&
                startDate!! < endDate!! &&
                isValidEmail(organizerEmail)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create New Rally",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBackPressed) {
                        Text("Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Error message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Basic Information Section
            Text(
                text = "Basic Information",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Rally Name *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequesters[0])
                    .onPreviewKeyEvent { keyEvent ->
                        handleKeyNavigation(0, keyEvent)
                    },
                isError = name.isBlank(),
                supportingText = if (name.isBlank()) {
                    { Text("Rally name is required") }
                } else {
                    null
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequesters[1])
                    .onPreviewKeyEvent { keyEvent ->
                        handleKeyNavigation(1, keyEvent)
                    },
                minLines = 3,
                maxLines = 5,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location *") },
                placeholder = { Text("e.g., Brainerd, Minnesota") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequesters[2])
                    .onPreviewKeyEvent { keyEvent ->
                        handleKeyNavigation(2, keyEvent)
                    },
                isError = location.isBlank(),
                supportingText = if (location.isBlank()) {
                    { Text("Location is required") }
                } else {
                    null
                },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Date/Time Section
            Text(
                text = "Date & Time",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Start Date/Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = startDate?.date?.toString() ?: "Start Date *",
                        color = if (startDate == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }

                OutlinedTextField(
                    value = startDate?.time?.toString()?.substring(0, 5) ?: "",
                    onValueChange = { timeStr ->
                        if (startDate != null && timeStr.matches(Regex("\\d{2}:\\d{2}"))) {
                            val parts = timeStr.split(":")
                            val hour = parts[0].toIntOrNull()
                            val minute = parts[1].toIntOrNull()
                            if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                                startDate = LocalDateTime(
                                    startDate!!.date,
                                    kotlinx.datetime.LocalTime(hour, minute),
                                )
                            }
                        }
                    },
                    label = { Text("Start Time") },
                    placeholder = { Text("09:00") },
                    modifier = Modifier.weight(1f),
                    enabled = startDate != null,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // End Date/Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = endDate?.date?.toString() ?: "End Date *",
                        color = if (endDate == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }

                OutlinedTextField(
                    value = endDate?.time?.toString()?.substring(0, 5) ?: "",
                    onValueChange = { timeStr ->
                        if (endDate != null && timeStr.matches(Regex("\\d{2}:\\d{2}"))) {
                            val parts = timeStr.split(":")
                            val hour = parts[0].toIntOrNull()
                            val minute = parts[1].toIntOrNull()
                            if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                                endDate = LocalDateTime(
                                    endDate!!.date,
                                    kotlinx.datetime.LocalTime(hour, minute),
                                )
                            }
                        }
                    },
                    label = { Text("End Time") },
                    placeholder = { Text("17:00") },
                    modifier = Modifier.weight(1f),
                    enabled = endDate != null,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Organizer Information Section
            Text(
                text = "Organizer Information",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            OutlinedTextField(
                value = organizerName,
                onValueChange = { organizerName = it },
                label = { Text("Organizer Name *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequesters[3])
                    .onPreviewKeyEvent { keyEvent ->
                        handleKeyNavigation(3, keyEvent)
                    },
                isError = organizerName.isBlank(),
                supportingText = if (organizerName.isBlank()) {
                    { Text("Organizer name is required") }
                } else {
                    null
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = organizerEmail,
                onValueChange = { organizerEmail = it },
                label = { Text("Organizer Email *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequesters[4])
                    .onPreviewKeyEvent { keyEvent ->
                        handleKeyNavigation(4, keyEvent)
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = organizerEmail.isBlank() || !isValidEmail(organizerEmail),
                supportingText = when {
                    organizerEmail.isBlank() -> { { Text("Email is required") } }
                    !isValidEmail(organizerEmail) -> { { Text("Please enter a valid email") } }
                    else -> null
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = organizerPhone,
                onValueChange = { organizerPhone = it },
                label = { Text("Organizer Phone") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequesters[5])
                    .onPreviewKeyEvent { keyEvent ->
                        handleKeyNavigation(5, keyEvent)
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Rally Configuration Section
            Text(
                text = "Rally Configuration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = registrationFee,
                    onValueChange = { registrationFee = it },
                    label = { Text("Registration Fee") },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequesters[6])
                        .onPreviewKeyEvent { keyEvent ->
                            handleKeyNavigation(6, keyEvent)
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("$") },
                )

                OutlinedTextField(
                    value = maxParticipants,
                    onValueChange = { maxParticipants = it },
                    label = { Text("Max Participants") },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequesters[7])
                        .onPreviewKeyEvent { keyEvent ->
                            handleKeyNavigation(7, keyEvent)
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("Unlimited") },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = rules,
                onValueChange = { rules = it },
                label = { Text("Rules & Instructions") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequesters[8])
                    .onPreviewKeyEvent { keyEvent ->
                        handleKeyNavigation(8, keyEvent)
                    },
                minLines = 4,
                maxLines = 8,
                placeholder = { Text("Enter rally rules, safety requirements, and instructions...") },
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedButton(
                    onClick = onBackPressed,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (isFormValid) {
                            isSaving = true
                            errorMessage = null
                            coroutineScope.launch {
                                try {
                                    val rally = Rally.create(
                                        name = name.trim(),
                                        description = description.trim(),
                                        startDateTime = startDate!!,
                                        endDateTime = endDate!!,
                                        location = location.trim(),
                                        organizerName = organizerName.trim(),
                                        organizerEmail = organizerEmail.trim(),
                                        organizerPhone = organizerPhone.trim(),
                                    ).copy(
                                        registrationFee = registrationFee.toDoubleOrNull() ?: 0.0,
                                        maxParticipants = maxParticipants.toIntOrNull(),
                                        rules = rules.trim(),
                                    )

                                    val result = repository.saveRally(rally)
                                    if (result.isSuccess) {
                                        onRallyCreated()
                                    } else {
                                        errorMessage = "Failed to create rally: ${result.exceptionOrNull()?.message}"
                                        isSaving = false
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Failed to create rally: ${e.message}"
                                    isSaving = false
                                }
                            }
                        }
                    },
                    enabled = isFormValid && !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequesters[9])
                        .onPreviewKeyEvent { keyEvent ->
                            handleKeyNavigation(9, keyEvent)
                        },
                ) {
                    Text(if (isSaving) "Creating..." else "Create Rally")
                }
            }
        }
    }

    // Date/Time Pickers
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val instant = Instant.fromEpochMilliseconds(millis)
                            val localDateTime = instant.toLocalDateTime(TimeZone.UTC)
                            startDate = startDate?.let { current ->
                                LocalDateTime(localDateTime.date, current.time)
                            } ?: LocalDateTime(localDateTime.date, kotlinx.datetime.LocalTime(9, 0))
                        }
                        showStartDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val instant = Instant.fromEpochMilliseconds(millis)
                            val localDateTime = instant.toLocalDateTime(TimeZone.UTC)
                            endDate = endDate?.let { current ->
                                LocalDateTime(localDateTime.date, current.time)
                            } ?: LocalDateTime(localDateTime.date, kotlinx.datetime.LocalTime(17, 0))
                        }
                        showEndDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Basic email validation
 */
private fun isValidEmail(email: String): Boolean {
    return email.contains("@") &&
        email.split("@").size == 2 &&
        email.split("@")[1].contains(".")
}
