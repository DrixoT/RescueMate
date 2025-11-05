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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rescuemate.emergency.data.MedicalInfo
import com.rescuemate.emergency.data.database.EmergencyDatabaseHelper
import java.io.File

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
    val existingData = remember { dbHelper.getMedicalInfo(userId) }

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

