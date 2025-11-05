package com.rescuemate.emergency.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rescuemate.emergency.data.MedicalInfo
import com.rescuemate.emergency.data.database.EmergencyDatabaseHelper
import java.util.*

/**
 * User Medical Profile Screen
 * Manages user medical information with proper validation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMedicalProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit = {},
    onProfileSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val dbHelper = remember { EmergencyDatabaseHelper(context) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }

    // Load existing data
    val existingData = remember { dbHelper.getMedicalInfo(userId) }

    // Form State Variables
    var fullName by remember { mutableStateOf(existingData?.let { userId } ?: "") }
    var dateOfBirth by remember { mutableStateOf(existingData?.dateOfBirth ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedSex by remember { mutableStateOf(existingData?.let { "Male" } ?: "Select") }
    var selectedBloodGroup by remember { mutableStateOf(existingData?.bloodType ?: "Select") }
    var emergencyNotes by remember { mutableStateOf(existingData?.emergencyNotes ?: "") }
    var preferredHospital by remember { mutableStateOf(existingData?.preferredHospital ?: "") }
    var doctorName by remember { mutableStateOf(existingData?.doctorName ?: "") }
    var doctorPhone by remember { mutableStateOf(existingData?.doctorPhone ?: "") }

    // Medications list
    var medications: List<String> by remember {
        mutableStateOf(listOf())
    }
    var newMedication by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medical Profile") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

            // Emergency Notes
            OutlinedTextField(
                value = emergencyNotes,
                onValueChange = { emergencyNotes = it },
                label = { Text("Emergency Notes (e.g., allergies, conditions)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                leadingIcon = { Icon(Icons.Default.Notes, "Notes") }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Medical Contacts & Information",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            // Preferred Hospital
            OutlinedTextField(
                value = preferredHospital,
                onValueChange = { preferredHospital = it },
                label = { Text("Preferred Hospital") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.LocalHospital, "Hospital") }
            )

            // Doctor Name
            OutlinedTextField(
                value = doctorName,
                onValueChange = { doctorName = it },
                label = { Text("Doctor Name") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, "Doctor") }
            )

            // Doctor Phone
            OutlinedTextField(
                value = doctorPhone,
                onValueChange = { doctorPhone = it },
                label = { Text("Doctor Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Phone, "Phone") },
                placeholder = { Text("+1234567890") }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Current Medications",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            // Add Medication
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newMedication,
                    onValueChange = { newMedication = it },
                    label = { Text("Add Medication") },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.MedicalServices, "Med") }
                )
                IconButton(
                    onClick = {
                        if (newMedication.isNotBlank()) {
                            medications = medications + newMedication
                            newMedication = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, "Add")
                }
            }

            // Medications List
            if (medications.isNotEmpty()) {
                medications.forEach { med ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(med)
                            IconButton(
                                onClick = {
                                    medications = medications.filter { it != med }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, "Remove", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Date Picker Dialog
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
            title = { Text("Save Changes?") },
            text = { Text("Do you want to save these medical profile changes?") },
            confirmButton = {
                Button(
                    onClick = {
                        // Validate data
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

                        // Save data
                        val medicalInfo = MedicalInfo(
                            userId = userId,
                            dateOfBirth = dateOfBirth,
                            bloodType = selectedBloodGroup,
                            emergencyNotes = emergencyNotes,
                            preferredHospital = preferredHospital,
                            doctorName = doctorName,
                            doctorPhone = doctorPhone,
                            baselineHeartRate = existingData?.baselineHeartRate ?: 70
                        )

                        dbHelper.insertOrUpdateMedicalInfo(medicalInfo)
                        showSaveDialog = false

                        // Show success message
                        onProfileSaved()
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
 * Reusable Dropdown Menu Field
 */
@Composable
fun DropdownMenuField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChanged: (String) -> Unit,
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Dropdown"
                )
            },
            isError = isError,
            supportingText = {
                if (isError) {
                    Text("$label is required", color = MaterialTheme.colorScheme.error)
                }
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChanged(option)
                        expanded = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
                    onValueChange = {
                        if (it.length <= 2 && it.toIntOrNull() != null && it.toInt() in 1..31) {
                            selectedDay = it.padStart(2, '0')
                        }
                    },
                    label = { Text("DD") },
                    modifier = Modifier.weight(1f)
                )

                // Month
                OutlinedTextField(
                    value = selectedMonth,
                    onValueChange = {
                        if (it.length <= 2 && it.toIntOrNull() != null && it.toInt() in 1..12) {
                            selectedMonth = it.padStart(2, '0')
                        }
                    },
                    label = { Text("MM") },
                    modifier = Modifier.weight(1f)
                )

                // Year
                OutlinedTextField(
                    value = selectedYear,
                    onValueChange = {
                        if (it.length <= 4 && it.toIntOrNull() != null) {
                            selectedYear = it
                        }
                    },
                    label = { Text("YYYY") },
                    modifier = Modifier.weight(1f)
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
    if (y < 1900 || y > Calendar.getInstance().get(Calendar.YEAR)) return false

    return true
}

