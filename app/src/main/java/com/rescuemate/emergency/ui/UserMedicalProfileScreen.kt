package com.rescuemate.emergency.ui

import android.app.DatePickerDialog
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.rescuemate.data.MedicalDataConstants
import com.rescuemate.data.UserPreferences
import com.rescuemate.data.repository.EmergencyRepository
import com.rescuemate.data.repository.FirestoreRepository
import com.rescuemate.emergency.data.MedicalInfo
import com.rescuemate.emergency.data.Medication
import com.rescuemate.ui.components.SearchableTagSelector
import com.rescuemate.ui.theme.*
import kotlinx.coroutines.launch
import java.util.*

/**
 * User Medical Profile Screen - Updated to use Firestore and SearchableTagSelector
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMedicalProfileScreen(
    userId: String = "default_user",
    onNavigateBack: () -> Unit = {},
    onProfileSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPrefs = remember { UserPreferences(context) }
    // Maintain local repository for offline/legacy support if needed, but focus on Firestore
    val repository = remember { EmergencyRepository(context) }
    val firestoreRepo = remember { FirestoreRepository() }

    Log.d("MedicalProfile", "🎨 Screen loaded for userId: $userId")

    // State
    var isLoading by remember { mutableStateOf(true) }
    
    // Photo State
    var profilePhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showEnlargedPhoto by remember { mutableStateOf(false) }

    // Basic Info State
    var fullName by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var selectedSex by remember { mutableStateOf("") }
    var selectedBloodType by remember { mutableStateOf("") }

    // Medical Info State
    var selectedConditions by remember { mutableStateOf(listOf<String>()) }
    var selectedMedications by remember { mutableStateOf(listOf<String>()) } // Storing names for UI
    var selectedAllergies by remember { mutableStateOf(listOf<String>()) }

    var showDatePicker by remember { mutableStateOf(false) }
    
    // Load Data
    LaunchedEffect(userId) {
        // Try to load from Firestore first, then fallback to local
        val firestoreInfo = firestoreRepo.getMedicalInfo()
        val firestoreProfile = firestoreRepo.getUserProfile()
        
        if (firestoreInfo != null) {
            // Populate from Firestore
            dateOfBirth = firestoreInfo.dateOfBirth ?: ""
            selectedBloodType = firestoreInfo.bloodType ?: ""
            selectedConditions = firestoreInfo.knownConditions
            selectedMedications = firestoreInfo.currentMedications.map { it.name }
            selectedAllergies = firestoreInfo.allergies
        } else {
            // Fallback to existing local DB logic
            val localInfo = repository.getMedicalInfo()
            if (localInfo != null) {
                dateOfBirth = localInfo.dateOfBirth ?: ""
                selectedBloodType = localInfo.bloodType ?: ""
                selectedConditions = localInfo.knownConditions
                selectedMedications = localInfo.currentMedications.map { it.name }
                selectedAllergies = localInfo.allergies
            }
        }

        if (firestoreProfile != null) {
            fullName = firestoreProfile["name"] as? String ?: userPrefs.getUserName() ?: ""
            selectedSex = firestoreProfile["gender"] as? String ?: ""
        } else {
            fullName = userPrefs.getUserName() ?: ""
            selectedSex = userPrefs.getUserGender() ?: ""
        }
        
        if (dateOfBirth.isBlank()) dateOfBirth = userPrefs.getDateOfBirth() ?: ""
        
        isLoading = false
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profilePhotoUri = it
            Log.d("MedicalProfile", "📸 Photo selected: $it")
            Toast.makeText(context, "Photo uploaded!", Toast.LENGTH_SHORT).show()
        }
    }

    // Save function
    fun saveProfile() {
        scope.launch {
            Log.d("MedicalProfile", "💾 Save button clicked")

            // Validation
            if (fullName.isBlank() || dateOfBirth.isBlank() || selectedSex.isBlank()) {
                Toast.makeText(context, "Please fill in basic info", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Save to Firestore
            try {
                firestoreRepo.saveUserProfile(
                    name = fullName,
                    dateOfBirth = dateOfBirth,
                    gender = selectedSex,
                    phone = userPrefs.getUserPhone() ?: ""
                )

                val medicalInfo = MedicalInfo(
                    userId = userId,
                    dateOfBirth = dateOfBirth,
                    bloodType = selectedBloodType,
                    knownConditions = selectedConditions,
                    currentMedications = selectedMedications.map { Medication(it, "", "") },
                    allergies = selectedAllergies,
                    baselineHeartRate = 70
                )
                firestoreRepo.saveMedicalInfo(medicalInfo)
                
                // Also update local prefs for offline components
                userPrefs.saveUserProfile(fullName, calculateAge(dateOfBirth), selectedSex, userPrefs.getUserPhone() ?: "")
                userPrefs.saveMedicalInfo(
                    selectedConditions.joinToString(", "),
                    selectedMedications.joinToString(", "),
                    selectedAllergies.joinToString(", "),
                    selectedBloodType
                )
                // And local DB
                repository.saveMedicalInfo(medicalInfo)

                Log.d("MedicalProfile", "✅ Medical profile saved successfully!")
                Toast.makeText(context, "✅ Profile saved successfully!", Toast.LENGTH_SHORT).show()
                onProfileSaved()
            } catch (e: Exception) {
                Log.e("MedicalProfile", "❌ Failed to save profile", e)
                Toast.makeText(context, "❌ Failed to save profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Medical Profile", color = CosmicTextPrimary)
                        Text(
                            "Emergency medical information",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = CosmicTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { saveProfile() }) {
                        Icon(Icons.Default.Check, "Save", tint = CosmicPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CosmicCard
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CosmicPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Profile Photo Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(3.dp, CosmicPrimary, CircleShape)
                            .clickable {
                                if (profilePhotoUri != null) {
                                    showEnlargedPhoto = true
                                }
                            }
                            .background(CosmicCard),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profilePhotoUri != null) {
                            AsyncImage(
                                model = profilePhotoUri,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default Avatar",
                                modifier = Modifier.size(60.dp),
                                tint = CosmicTextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                    ) {
                        Icon(Icons.Default.CameraAlt, "Camera", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Change Photo")
                    }
                }

                Divider(color = CosmicBorder)

                // Basic Information
                Text(
                    text = "Basic Information",
                    style = MaterialTheme.typography.titleLarge,
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, "Name", tint = CosmicPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicPrimary,
                        unfocusedBorderColor = CosmicBorder,
                        focusedTextColor = CosmicTextPrimary,
                        unfocusedTextColor = CosmicTextPrimary,
                        focusedContainerColor = CosmicCard,
                        unfocusedContainerColor = CosmicCard
                    )
                )

                OutlinedTextField(
                    value = dateOfBirth,
                    onValueChange = {},
                    label = { Text("Date of Birth") },
                    placeholder = { Text("DD/MM/YYYY") },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, "DOB", tint = CosmicPrimary) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, "Pick Date", tint = CosmicPrimary)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicPrimary,
                        unfocusedBorderColor = CosmicBorder,
                        focusedTextColor = CosmicTextPrimary,
                        unfocusedTextColor = CosmicTextPrimary,
                        focusedContainerColor = CosmicCard,
                        unfocusedContainerColor = CosmicCard
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DropdownSelector(
                        label = "Sex",
                        selectedValue = selectedSex,
                        options = listOf("Male", "Female"),
                        onValueChange = { selectedSex = it },
                        icon = Icons.Default.Person,
                        modifier = Modifier.weight(1f)
                    )

                    DropdownSelector(
                        label = "Blood Type",
                        selectedValue = selectedBloodType,
                        options = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"),
                        onValueChange = { selectedBloodType = it },
                        icon = Icons.Default.Bloodtype,
                        modifier = Modifier.weight(1f)
                    )
                }

                Divider(color = CosmicBorder)

                // Medical Information - Using New SearchableTagSelector
                Text(
                    text = "Medical Information",
                    style = MaterialTheme.typography.titleLarge,
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                SearchableTagSelector(
                    label = "Medical Conditions",
                    allOptions = MedicalDataConstants.getMedicalConditions(),
                    selectedOptions = selectedConditions,
                    onOptionSelected = { selectedConditions = selectedConditions + it },
                    onOptionRemoved = { selectedConditions = selectedConditions - it }
                )

                SearchableTagSelector(
                    label = "Current Medications",
                    allOptions = MedicalDataConstants.getCommonMedications(),
                    selectedOptions = selectedMedications,
                    onOptionSelected = { selectedMedications = selectedMedications + it },
                    onOptionRemoved = { selectedMedications = selectedMedications - it }
                )

                SearchableTagSelector(
                    label = "Allergies",
                    allOptions = MedicalDataConstants.getCommonAllergies(),
                    selectedOptions = selectedAllergies,
                    onOptionSelected = { selectedAllergies = selectedAllergies + it },
                    onOptionRemoved = { selectedAllergies = selectedAllergies - it }
                )

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                dateOfBirth = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                showDatePicker = false
            },
            year,
            month,
            day
        ).apply {
            setOnCancelListener { showDatePicker = false }
            show()
        }
    }

    // Enlarged Photo Dialog
    if (showEnlargedPhoto && profilePhotoUri != null) {
        Dialog(onDismissRequest = { showEnlargedPhoto = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.large)
                    .background(CosmicCard)
                    .clickable { showEnlargedPhoto = false }
            ) {
                AsyncImage(
                    model = profilePhotoUri,
                    contentDescription = "Enlarged Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

// Helper function kept here for simplicity
fun calculateAge(dateOfBirth: String): String {
    return try {
        val parts = dateOfBirth.split("/")
        if (parts.size == 3) {
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()

            val today = Calendar.getInstance()
            val birthDate = Calendar.getInstance().apply {
                set(year, month - 1, day)
            }

            var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            age.toString()
        } else {
            "0"
        }
    } catch (e: Exception) {
        "0"
    }
}

@Composable
fun DropdownSelector(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedValue.ifBlank { "Select" },
            onValueChange = {},
            label = { Text(label) },
            leadingIcon = {
                Icon(icon, label, tint = CosmicPrimary)
            },
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    "Dropdown",
                    tint = CosmicPrimary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            readOnly = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CosmicPrimary,
                unfocusedBorderColor = CosmicBorder,
                focusedTextColor = CosmicTextPrimary,
                unfocusedTextColor = CosmicTextPrimary,
                focusedContainerColor = CosmicCard,
                unfocusedContainerColor = CosmicCard
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .background(CosmicCard)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = CosmicTextPrimary) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
