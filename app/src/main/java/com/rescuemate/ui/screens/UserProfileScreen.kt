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

data class MedicalCondition(val id: String, val name: String)

// Sample medical conditions database
val medicalConditions = listOf(
    MedicalCondition("diabetes", "Diabetes"),
    MedicalCondition("hypertension", "Hypertension"),
    MedicalCondition("asthma", "Asthma"),
    MedicalCondition("heart_disease", "Heart Disease"),
    MedicalCondition("epilepsy", "Epilepsy"),
    MedicalCondition("allergies", "Allergies"),
    MedicalCondition("arthritis", "Arthritis"),
    MedicalCondition("cancer", "Cancer"),
    MedicalCondition("copd", "COPD"),
    MedicalCondition("depression", "Depression/Anxiety")
)

val commonMedications = listOf(
    "Aspirin", "Ibuprofen", "Metformin", "Lisinopril", "Atorvastatin",
    "Levothyroxine", "Metoprolol", "Amlodipine", "Omeprazole", "Losartan"
)

val commonAllergies = listOf(
    "Penicillin", "Sulfa drugs", "Aspirin", "Ibuprofen", "Codeine",
    "Latex", "Peanuts", "Shellfish", "Bee stings", "No known allergies"
)

@OptIn(ExperimentalMaterial3Api::class)
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
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
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
                }
            }
        }
    }

    var dateOfBirth by remember { mutableStateOf(userPrefs.getDateOfBirth() ?: "") }
    var age by remember { mutableStateOf(userPrefs.getUserAge() ?: "") }
    var sex by remember { mutableStateOf(userPrefs.getUserGender() ?: "") }
    var phone by remember { mutableStateOf(userPrefs.getUserPhone() ?: "") }
    var bloodType by remember { mutableStateOf(userPrefs.getBloodType() ?: "") }
    var medicalHistory by remember { mutableStateOf(userPrefs.getMedicalHistory() ?: "") }
    var currentMedication by remember { mutableStateOf(userPrefs.getCurrentMedication() ?: "") }
    var allergies by remember { mutableStateOf(userPrefs.getAllergies() ?: "") }
    var selectedConditions by remember { mutableStateOf(setOf<String>()) }
    var showConditionsDialog by remember { mutableStateOf(false) }
    var showMedicationDialog by remember { mutableStateOf(false) }
    var showAllergiesDialog by remember { mutableStateOf(false) }

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
            
            // Parse medical history, medications, and allergies from text
            val conditions = medicalHistory.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val medications = currentMedication.split(",").map { 
                com.rescuemate.emergency.data.Medication(
                    name = it.trim(),
                    dosage = "",
                    frequency = ""
                )
            }.filter { it.name.isNotEmpty() }
            val allergyList = allergies.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            
            val medicalInfo = com.rescuemate.emergency.data.MedicalInfo(
                userId = userId,
                dateOfBirth = dateOfBirth.ifEmpty { null },
                bloodType = bloodType.ifEmpty { null },
                knownConditions = conditions,
                currentMedications = medications,
                allergies = allergyList,
                baselineHeartRate = 70
            )
            
            repository.saveMedicalInfo(medicalInfo)
            
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

                // Medical History
                OutlinedTextField(
                    value = medicalHistory,
                    onValueChange = { medicalHistory = it },
                    label = { Text("Medical History") },
                    placeholder = { Text("Diabetes, Hypertension, etc.") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = CosmicPrimary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicPrimary,
                        unfocusedBorderColor = CosmicBorder,
                        focusedLabelColor = CosmicPrimary,
                        unfocusedLabelColor = CosmicTextSecondary,
                        cursorColor = CosmicPrimary
                    )
                )

                // Current Medications
                OutlinedTextField(
                    value = currentMedication,
                    onValueChange = { currentMedication = it },
                    label = { Text("Current Medications") },
                    placeholder = { Text("Aspirin, Metformin, etc.") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = CosmicPrimary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicPrimary,
                        unfocusedBorderColor = CosmicBorder,
                        focusedLabelColor = CosmicPrimary,
                        unfocusedLabelColor = CosmicTextSecondary,
                        cursorColor = CosmicPrimary
                    )
                )

                // Allergies
                OutlinedTextField(
                    value = allergies,
                    onValueChange = { allergies = it },
                    label = { Text("Allergies") },
                    placeholder = { Text("Penicillin, Peanuts, etc.") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CosmicPrimary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicPrimary,
                        unfocusedBorderColor = CosmicBorder,
                        focusedLabelColor = CosmicPrimary,
                        unfocusedLabelColor = CosmicTextSecondary,
                        cursorColor = CosmicPrimary
                    )
                )

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

    // Medical Conditions Dialog
    if (showConditionsDialog) {
        AlertDialog(
            onDismissRequest = { showConditionsDialog = false },
            title = { Text("Select Medical Conditions") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    medicalConditions.forEach { condition ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedConditions.contains(condition.name),
                                onCheckedChange = {
                                    selectedConditions = if (it) {
                                        selectedConditions + condition.name
                                    } else {
                                        selectedConditions - condition.name
                                    }
                                }
                            )
                            Text(condition.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConditionsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Similar dialogs for medications and allergies would go here
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

