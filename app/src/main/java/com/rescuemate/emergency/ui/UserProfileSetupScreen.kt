package com.rescuemate.emergency.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rescuemate.emergency.data.MedicalInfo
import com.rescuemate.emergency.data.database.EmergencyDatabaseHelper

/**
 * Complete User Profile Setup Screen
 * Handles all user information including photo upload
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileSetupScreen(
    userId: String,
    onNavigateBack: () -> Unit = {},
    onProfileComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val dbHelper = remember { EmergencyDatabaseHelper(context) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }
    var profilePhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            profilePhotoUri = uri
        }
    }

    // Load existing data
    val existingData = remember { dbHelper.getMedicalInfo(userId).getOrNull() }

    // Form State
    var fullName by remember { mutableStateOf(existingData?.let { userId } ?: "") }
    var dateOfBirth by remember { mutableStateOf(existingData?.dateOfBirth ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedSex by remember { mutableStateOf(existingData?.let { "Male" } ?: "Select") }
    var selectedBloodGroup by remember { mutableStateOf(existingData?.bloodType ?: "Select") }
    var emergencyNotes by remember { mutableStateOf(existingData?.emergencyNotes ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.Done, "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Photo Section
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { photoPickerLauncher.launch("image/*") }
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (profilePhotoUri != null) {
                    AsyncImage(
                        model = profilePhotoUri,
                        contentDescription = "Profile Photo",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Add Photo",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Add Photo",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Text(
                "Tap to add profile photo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider()

            // Basic Information
            Text(
                "Basic Information",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            // Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name *") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, "Name") },
                isError = fullName.isBlank(),
                supportingText = {
                    if (fullName.isBlank()) {
                        Text("Name is required", color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            // Date of Birth
            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = {},
                label = { Text("Date of Birth (DD/MM/YYYY) *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                leadingIcon = { Icon(Icons.Default.DateRange, "DOB") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, "Pick Date")
                    }
                },
                readOnly = true,
                isError = dateOfBirth.isBlank(),
                supportingText = {
                    if (dateOfBirth.isBlank()) {
                        Text("Date of birth is required", color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            // Sex Dropdown
            DropdownMenuField(
                label = "Sex *",
                selectedValue = selectedSex,
                options = listOf("Male", "Female", "Other", "Prefer not to say"),
                onValueChanged = { selectedSex = it },
                isError = selectedSex == "Select"
            )

            // Blood Group Dropdown
            DropdownMenuField(
                label = "Blood Group *",
                selectedValue = selectedBloodGroup,
                options = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-", "Unknown"),
                onValueChanged = { selectedBloodGroup = it },
                isError = selectedBloodGroup == "Select"
            )

            Divider()

            // Emergency Notes
            Text(
                "Additional Information",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = emergencyNotes,
                onValueChange = { emergencyNotes = it },
                label = { Text("Emergency Notes (allergies, conditions, etc.)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                leadingIcon = { Icon(Icons.Default.Notes, "Notes") }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Date Picker
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                dateOfBirth = date
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Save Confirmation Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Profile?") },
            text = { Text("Do you want to save your profile changes?") },
            confirmButton = {
                Button(
                    onClick = {
                        // Validate
                        if (fullName.isBlank()) {
                            errorMessage = "Name is required"
                            showErrorDialog = true
                            showSaveDialog = false
                            return@Button
                        }
                        if (dateOfBirth.isBlank()) {
                            errorMessage = "Date of birth is required"
                            showErrorDialog = true
                            showSaveDialog = false
                            return@Button
                        }
                        if (selectedSex == "Select") {
                            errorMessage = "Please select a sex"
                            showErrorDialog = true
                            showSaveDialog = false
                            return@Button
                        }
                        if (selectedBloodGroup == "Select") {
                            errorMessage = "Please select a blood group"
                            showErrorDialog = true
                            showSaveDialog = false
                            return@Button
                        }

                        // Save profile
                        val medicalInfo = MedicalInfo(
                            userId = userId,
                            dateOfBirth = dateOfBirth,
                            bloodType = selectedBloodGroup,
                            emergencyNotes = emergencyNotes,
                            baselineHeartRate = existingData?.baselineHeartRate ?: 70
                        )

                        dbHelper.insertOrUpdateMedicalInfo(medicalInfo)
                        showSaveDialog = false
                        showSuccessDialog = true
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Success!") },
            text = { Text("Changes are saved") },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onProfileComplete()
                        onNavigateBack()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Error Dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Validation Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}


/**
 * Simple Date Picker Dialog
 */
@Composable
fun DatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDay by remember { mutableStateOf("01") }
    var selectedMonth by remember { mutableStateOf("01") }
    var selectedYear by remember { mutableStateOf("1990") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date of Birth") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Day
                OutlinedTextField(
                    value = selectedDay,
                    onValueChange = { newValue ->
                        if (newValue.length <= 2) {
                            val day = newValue.toIntOrNull()
                            if (day != null && day in 1..31) {
                                selectedDay = newValue.padStart(2, '0')
                            } else if (newValue.isEmpty()) {
                                selectedDay = newValue
                            }
                        }
                    },
                    label = { Text("DD") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // Month
                OutlinedTextField(
                    value = selectedMonth,
                    onValueChange = { newValue ->
                        if (newValue.length <= 2) {
                            val month = newValue.toIntOrNull()
                            if (month != null && month in 1..12) {
                                selectedMonth = newValue.padStart(2, '0')
                            } else if (newValue.isEmpty()) {
                                selectedMonth = newValue
                            }
                        }
                    },
                    label = { Text("MM") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // Year
                OutlinedTextField(
                    value = selectedYear,
                    onValueChange = { newValue ->
                        if (newValue.length <= 4) {
                            val year = newValue.toIntOrNull()
                            if (year != null || newValue.isEmpty()) {
                                selectedYear = newValue
                            }
                        }
                    },
                    label = { Text("YYYY") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dateString = "$selectedDay/$selectedMonth/$selectedYear"
                    if (isValidDate(selectedDay, selectedMonth, selectedYear)) {
                        onDateSelected(dateString)
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Validate date format
 */
private fun isValidDate(day: String, month: String, year: String): Boolean {
    val d = day.toIntOrNull() ?: return false
    val m = month.toIntOrNull() ?: return false
    val y = year.toIntOrNull() ?: return false

    if (d < 1 || d > 31) return false
    if (m < 1 || m > 12) return false
    if (y < 1900 || y > 2025) return false

    return true
}
