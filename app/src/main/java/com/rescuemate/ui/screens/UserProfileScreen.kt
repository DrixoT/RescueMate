package com.rescuemate.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescuemate.ui.theme.*
import com.rescuemate.data.MedicalDataProvider
import com.rescuemate.ui.components.AutoCompleteTextField

// Load medical data from the CSV database
val medicalConditions = MedicalDataProvider.getMedicalConditions().map { it.name }
val commonMedications = MedicalDataProvider.getMedications().map { it.name }
val commonAllergies = MedicalDataProvider.getAllergies().map { it.name }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserProfileScreen(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val userPrefs = remember { com.rescuemate.data.UserPreferences(context) }
    val scope = rememberCoroutineScope()
    val firestoreRepo = remember { com.rescuemate.data.repository.FirestoreRepository() }

    // Load existing data
    var name by remember { mutableStateOf(userPrefs.getUserName() ?: "") }
    var photoUrl by remember { mutableStateOf(userPrefs.getProfilePhotoUrl()) }
    
    // State for selected photo URI to trigger upload
    var selectedPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri  // Set state instead of launching coroutine directly
        }
    }
    
    // Handle photo upload using LaunchedEffect to avoid coroutine leaving composition
    LaunchedEffect(selectedPhotoUri) {
        selectedPhotoUri?.let { uri ->
            try {
                android.widget.Toast.makeText(context, "Uploading photo...", android.widget.Toast.LENGTH_SHORT).show()
                val url = firestoreRepo.uploadProfilePhoto(uri)
                photoUrl = url
                userPrefs.saveProfilePhotoUrl(url)
                android.widget.Toast.makeText(context, "Photo updated successfully", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("UserProfileScreen", "Error uploading photo", e)
                val errorMessage = when {
                    e.message?.contains("Object does not exist") == true -> "Storage bucket configuration error. Please contact support."
                    e.message?.contains("unauthorized") == true -> "Permission denied. Please log in again."
                    else -> "Failed to upload photo: ${e.message}"
                }
                android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
            } finally {
                selectedPhotoUri = null  // Reset after processing
            }
        }
    }

    var dateOfBirth by remember { mutableStateOf(userPrefs.getDateOfBirth() ?: "") }
    var age by remember { mutableStateOf(userPrefs.getUserAge() ?: "") }
    var sex by remember { mutableStateOf(userPrefs.getUserGender() ?: "") }
    var phone by remember { mutableStateOf(userPrefs.getUserPhone() ?: "") }
    var bloodType by remember { mutableStateOf(userPrefs.getBloodType() ?: "") }
    // Medical data state
    var medicalHistory by remember { mutableStateOf(userPrefs.getMedicalHistory() ?: "") }
    var currentMedication by remember { mutableStateOf(userPrefs.getCurrentMedication() ?: "") }
    var allergies by remember { mutableStateOf(userPrefs.getAllergies() ?: "") }

    // Auto-complete state
    var medicalHistoryQuery by remember { mutableStateOf("") }
    var medicationQuery by remember { mutableStateOf("") }
    var allergyQuery by remember { mutableStateOf("") }

    // Selected items as lists
    var selectedMedicalConditions by remember { mutableStateOf(medicalHistory.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()) }
    var selectedMedications by remember { mutableStateOf(currentMedication.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()) }
    var selectedAllergies by remember { mutableStateOf(allergies.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()) }

    // Calculate age from date of birth
    fun calculateAge(dob: String): String {
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val birthDate = java.time.LocalDate.parse(dob, formatter)
            val currentAge = java.time.Period.between(birthDate, java.time.LocalDate.now()).years
            currentAge.toString()
        } catch (e: Exception) {
            age
        }
    }

    // Update age when date of birth changes
    LaunchedEffect(dateOfBirth) {
        if (dateOfBirth.isNotEmpty()) {
            val calculatedAge = calculateAge(dateOfBirth)
            if (calculatedAge.isNotEmpty()) {
                age = calculatedAge
            }
        }
    }

    // Save function
    fun saveProfile() {
        try {
            // Save basic profile
            userPrefs.saveUserProfile(
                name = name,
                age = age,
                gender = sex,
                phone = phone
            )

            if (dateOfBirth.isNotEmpty()) {
                userPrefs.saveDateOfBirth(dateOfBirth)
            }

            // Update the text fields from selected items
            medicalHistory = selectedMedicalConditions.joinToString(", ")
            currentMedication = selectedMedications.joinToString(", ")
            allergies = selectedAllergies.joinToString(", ")

            // Save medical info to UserPreferences
            userPrefs.saveMedicalInfo(
                medicalHistory = medicalHistory,
                currentMedication = currentMedication,
                allergies = allergies,
                bloodType = bloodType
            )

            // Also save to EmergencyDatabaseHelper for emergency events
            val repository = com.rescuemate.data.repository.EmergencyRepository(context)
            val userId = userPrefs.getUserId()

            // Use selected items for structured data
            val medications = selectedMedications.map {
                com.rescuemate.emergency.data.Medication(
                    name = it,
                    dosage = "",
                    frequency = ""
                )
            }.filter { it.name.isNotEmpty() }
            
            val medicalInfo = com.rescuemate.emergency.data.MedicalInfo(
                userId = userId,
                dateOfBirth = dateOfBirth.ifEmpty { null },
                bloodType = bloodType.ifEmpty { null },
                knownConditions = selectedMedicalConditions,
                currentMedications = medications,
                allergies = selectedAllergies,
                baselineHeartRate = 70
            )
            
            repository.saveMedicalInfo(medicalInfo)

            // Save medical info to Firestore as well
            scope.launch {
                try {
                    firestoreRepo.saveMedicalInfo(medicalInfo)
                    android.util.Log.d("UserProfileScreen", "✅ Medical info saved to Firestore")
                } catch (e: Exception) {
                    android.util.Log.e("UserProfileScreen", "Error saving medical info to Firestore", e)
                }
            }

            android.util.Log.d("UserProfileScreen", "✅ Profile saved to both UserPreferences and EmergencyDatabase")
            android.widget.Toast.makeText(context, "Profile saved successfully", android.widget.Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            android.util.Log.e("UserProfileScreen", "Error saving profile", e)
            android.widget.Toast.makeText(context, "Error saving profile", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    com.rescuemate.ui.components.CosmicScaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CosmicCard.copy(alpha = 0.8f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = CosmicTextPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Medical Profile",
                            style = MaterialTheme.typography.titleLarge,
                            color = CosmicTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Emergency medical information",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicTextSecondary,
                            letterSpacing = 1.5.sp
                        )
                    }
                    IconButton(onClick = { saveProfile() }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = CosmicPrimary
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Profile Photo Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = CosmicCard,
                        border = androidx.compose.foundation.BorderStroke(2.dp, CosmicPrimary)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (photoUrl != null) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp),
                                    tint = CosmicTextPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { 
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text("Change Photo", color = CosmicPrimary)
                    }
                }

                // Basic Information
                SectionTitle("Basic Information")

                ProfileTextField(
                    label = "Full Name",
                    value = name,
                    onValueChange = { name = it },
                    icon = Icons.Default.Person
                )

                ProfileTextField(
                    label = "Phone Number",
                    value = phone,
                    onValueChange = { phone = it },
                    icon = Icons.Default.Phone
                )

                ProfileTextField(
                    label = "Date of Birth (DD/MM/YYYY)",
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = it },
                    icon = Icons.Default.CalendarMonth
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileTextField(
                            label = "Age",
                            value = age,
                            onValueChange = { age = it },
                            icon = Icons.Default.CalendarMonth
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileTextField(
                            label = "Sex",
                            value = sex,
                            onValueChange = { sex = it },
                            icon = Icons.Default.Person
                        )
                    }
                }

                ProfileTextField(
                    label = "Blood Type",
                    value = bloodType,
                    onValueChange = { bloodType = it },
                    icon = Icons.Default.Bloodtype
                )

                // Medical Information
                Spacer(modifier = Modifier.height(8.dp))
                SectionTitle("Medical Information")

                // Medical History with Auto-complete
                Column(modifier = Modifier.fillMaxWidth()) {
                    AutoCompleteTextField(
                        label = "Medical History",
                        value = medicalHistoryQuery,
                        onValueChange = { medicalHistoryQuery = it },
                        suggestions = medicalConditions,
                        onSuggestionSelected = { suggestion ->
                            if (!selectedMedicalConditions.contains(suggestion)) {
                                selectedMedicalConditions.add(suggestion)
                            }
                            medicalHistoryQuery = ""
                        },
                        placeholder = "Type to search medical conditions...",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = CosmicPrimary
                            )
                        }
                    )

                    // Display selected medical conditions as chips
                    if (selectedMedicalConditions.isNotEmpty()) {
                        Text(
                            text = "Selected Conditions:",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicTextSecondary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            selectedMedicalConditions.forEach { condition ->
                                AssistChip(
                                    onClick = { selectedMedicalConditions.remove(condition) },
                                    label = { Text(condition, style = MaterialTheme.typography.bodySmall) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = CosmicPrimary.copy(alpha = 0.1f),
                                        labelColor = CosmicPrimary,
                                        trailingIconContentColor = CosmicPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // Current Medications with Auto-complete
                Column(modifier = Modifier.fillMaxWidth()) {
                    AutoCompleteTextField(
                        label = "Current Medications",
                        value = medicationQuery,
                        onValueChange = { medicationQuery = it },
                        suggestions = commonMedications,
                        onSuggestionSelected = { suggestion ->
                            if (!selectedMedications.contains(suggestion)) {
                                selectedMedications.add(suggestion)
                            }
                            medicationQuery = ""
                        },
                        placeholder = "Type to search medications...",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = CosmicPrimary
                            )
                        }
                    )

                    // Display selected medications as chips
                    if (selectedMedications.isNotEmpty()) {
                        Text(
                            text = "Selected Medications:",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicTextSecondary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            selectedMedications.forEach { medication ->
                                AssistChip(
                                    onClick = { selectedMedications.remove(medication) },
                                    label = { Text(medication, style = MaterialTheme.typography.bodySmall) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = CosmicPrimary.copy(alpha = 0.1f),
                                        labelColor = CosmicPrimary,
                                        trailingIconContentColor = CosmicPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // Allergies with Auto-complete
                Column(modifier = Modifier.fillMaxWidth()) {
                    AutoCompleteTextField(
                        label = "Allergies",
                        value = allergyQuery,
                        onValueChange = { allergyQuery = it },
                        suggestions = commonAllergies,
                        onSuggestionSelected = { suggestion ->
                            if (!selectedAllergies.contains(suggestion)) {
                                selectedAllergies.add(suggestion)
                            }
                            allergyQuery = ""
                        },
                        placeholder = "Type to search allergies...",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = CosmicPrimary
                            )
                        }
                    )

                    // Display selected allergies as chips
                    if (selectedAllergies.isNotEmpty()) {
                        Text(
                            text = "Selected Allergies:",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicTextSecondary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            selectedAllergies.forEach { allergy ->
                                AssistChip(
                                    onClick = { selectedAllergies.remove(allergy) },
                                    label = { Text(allergy, style = MaterialTheme.typography.bodySmall) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = CosmicPrimary.copy(alpha = 0.1f),
                                        labelColor = CosmicPrimary,
                                        trailingIconContentColor = CosmicPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // Emergency Note
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CosmicPrimary.copy(alpha = 0.1f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CosmicPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CosmicPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Medical Information Privacy",
                                style = MaterialTheme.typography.titleSmall,
                                color = CosmicTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your medical information is encrypted and only shared with emergency responders when you activate SOS.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CosmicTextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = CosmicTextPrimary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CosmicPrimary
            )
        },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CosmicPrimary,
            unfocusedBorderColor = CosmicBorder,
            focusedLabelColor = CosmicPrimary,
            unfocusedLabelColor = CosmicTextSecondary,
            cursorColor = CosmicPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileClickableField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CosmicCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CosmicPrimary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicTextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CosmicTextPrimary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CosmicTextSecondary
            )
        }
    }
}
