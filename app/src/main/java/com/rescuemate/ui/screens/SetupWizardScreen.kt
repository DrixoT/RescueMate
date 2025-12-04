package com.rescuemate.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rescuemate.data.MedicalDataConstants
import com.rescuemate.data.UserPreferences
import com.rescuemate.data.repository.AuthRepository
import com.rescuemate.data.repository.FirestoreRepository
import com.rescuemate.emergency.data.EmergencyContact
import com.rescuemate.emergency.data.MedicalInfo
import com.rescuemate.emergency.data.Medication
import com.rescuemate.ui.components.SearchableTagSelector
import com.rescuemate.ui.theme.CosmicBackground
import com.rescuemate.ui.theme.CosmicPrimary
import com.rescuemate.ui.theme.CosmicTextPrimary
import com.rescuemate.ui.theme.CosmicTextSecondary
import com.rescuemate.utils.ValidationUtils
import com.rescuemate.utils.ProfanityFilter
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPrefs = remember { UserPreferences(context) }
    val firestoreRepo = remember { FirestoreRepository() }
    val authRepo = remember { AuthRepository(context) }
    val emergencyRepo = remember { com.rescuemate.data.repository.EmergencyRepository(context) }

    // Get current Firebase user to pre-fill name if logged in via Google
    val currentUser = authRepo.getCurrentUser()
    val initialName = remember {
        currentUser?.displayName ?: userPrefs.getUserName() ?: ""
    }

    var currentStep by remember { mutableStateOf(0) }
    val steps = 4
    var isLoading by remember { mutableStateOf(false) }

    // Step 0: Name Entry
    var name by remember { mutableStateOf(initialName) }

    // Step 1: Basic Info State
    var dateOfBirth by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var userPhone by remember { mutableStateOf(currentUser?.phoneNumber ?: userPrefs.getUserPhone() ?: "") }

    // Step 2: Medical Info State
    var conditions by remember { mutableStateOf(listOf<String>()) }
    var medications by remember { mutableStateOf(listOf<String>()) }
    var allergies by remember { mutableStateOf(listOf<String>()) }
    var bloodType by remember { mutableStateOf("") }
    var isBloodTypeExpanded by remember { mutableStateOf(false) }

    // Step 3: Emergency Contact State - Start empty, only load from Firestore (user-specific)
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactRelation by remember { mutableStateOf("") }
    
    // Load existing user data on screen open
    LaunchedEffect(Unit) {
        try {
            Log.d("SetupWizardScreen", "Loading existing user data...")

            // First, load from local preferences (populated from SignUpScreen)
            userPrefs.getUserName()?.let { name = it }
            userPrefs.getDateOfBirth()?.let { dateOfBirth = it }
            userPrefs.getUserGender()?.let { gender = it }
            userPrefs.getUserPhone()?.let { userPhone = it }

            // Load medical data from local preferences
            userPrefs.getMedicalHistory()?.let { history ->
                if (history.isNotBlank()) {
                    conditions = history.split(", ").filter { it.isNotBlank() }
                }
            }
            userPrefs.getCurrentMedication()?.let { meds ->
                if (meds.isNotBlank()) {
                    medications = meds.split(", ").filter { it.isNotBlank() }
                }
            }
            userPrefs.getAllergies()?.let { allergyStr ->
                if (allergyStr.isNotBlank()) {
                    allergies = allergyStr.split(", ").filter { it.isNotBlank() }
                }
            }
            userPrefs.getBloodType()?.let { bloodType = it }

            // If we have minimal local data, try to load from Firestore for more complete profile
            if (name.isBlank() || conditions.isEmpty()) {
                Log.d("SetupWizardScreen", "Local data incomplete, fetching from Firestore...")

                try {
                    // Load user profile from Firestore
                    val userProfile = firestoreRepo.getUserProfile()
                    userProfile?.let { profile ->
                        if (name.isBlank()) name = profile["name"] as? String ?: ""
                        if (dateOfBirth.isBlank()) dateOfBirth = profile["dateOfBirth"] as? String ?: ""
                        if (gender.isBlank()) gender = profile["gender"] as? String ?: ""
                        if (userPhone.isBlank()) userPhone = profile["phone"] as? String ?: ""
                    }

                    // Load medical info from Firestore
                    val medicalInfo = firestoreRepo.getMedicalInfo()
                    medicalInfo?.let { info ->
                        if (conditions.isEmpty()) conditions = info.knownConditions
                        if (allergies.isEmpty()) allergies = info.allergies
                        if (bloodType.isBlank()) bloodType = info.bloodType ?: ""
                        if (medications.isEmpty()) {
                            medications = info.currentMedications.map { it.name }
                        }
                    }

                    // Load emergency contacts from Firestore (already filtered by current user's ID)
                    // Only load if contacts exist and belong to current user
                    val contacts = firestoreRepo.getContacts()
                    if (contacts.isNotEmpty()) {
                        contacts.firstOrNull()?.let { contact ->
                            // Only populate if contact data is valid and belongs to current user
                            // FirestoreRepository.getContacts() already filters by auth.currentUser?.uid
                            contactName = contact.name
                            contactPhone = contact.phoneNumber
                            contactRelation = contact.relationship ?: ""
                            Log.d("SetupWizardScreen", "Loaded contact from Firestore: ${contact.name}")
                        }
                    } else {
                        // Ensure fields are empty if no contacts found
                        contactName = ""
                        contactPhone = ""
                        contactRelation = ""
                        Log.d("SetupWizardScreen", "No contacts found in Firestore - fields remain empty")
                    }

                    Log.d("SetupWizardScreen", "Successfully loaded data from Firestore")
                } catch (e: Exception) {
                    Log.w("SetupWizardScreen", "Failed to load from Firestore, using local data only: ${e.message}")
                }
            }

            Log.d("SetupWizardScreen", "Data loading complete - Name: '$name', DOB: '$dateOfBirth', Conditions: ${conditions.size}, Contacts: '$contactName'")
        } catch (e: Exception) {
            Log.e("SetupWizardScreen", "Error loading user data: ${e.message}", e)
            Toast.makeText(context, "Error loading profile data", Toast.LENGTH_SHORT).show()
        }
    }

    // Validation Helpers
    val dobRegex = Regex("""\d{2}/\d{2}/\d{4}""")
    val phoneRegex = Regex("""^\+?[0-9\s-]{10,}$""")

    fun canProceed(): Boolean {
        return when (currentStep) {
            0 -> name.isNotBlank() && !ProfanityFilter.containsProfanity(name)
            1 -> {
                val dateValid = ValidationUtils.validateDateDDMMYYYY(dateOfBirth).isValid
                dateValid && gender.isNotBlank()
            }
            2 -> bloodType.isNotBlank()
            3 -> {
                val nameValid = contactName.all { it.isLetter() || it.isWhitespace() } && 
                     contactName.isNotBlank() && 
                     !ProfanityFilter.containsProfanity(contactName)
                val phoneValid = contactPhone.matches(phoneRegex)
                val relationValid = contactRelation.isBlank() || !ProfanityFilter.containsProfanity(contactRelation)
                nameValid && phoneValid && relationValid
            }
            else -> false
        }
    }

    fun onNext() {
        if (currentStep < steps - 1) {
            currentStep++
        } else {
            scope.launch {
                try {
                    isLoading = true
                    Log.d("SetupWizardScreen", "════════════════════════════════════════")
                    Log.d("SetupWizardScreen", "Starting setup wizard completion")
                    Log.d("SetupWizardScreen", "════════════════════════════════════════")
                    
                    // Save to local preferences first (always succeeds)
                    try {
                        val phoneToSave = userPhone.ifEmpty { "" }
                        Log.d("SetupWizardScreen", "Saving user profile locally...")
                        userPrefs.saveUserProfile(name, "0", gender, phoneToSave)
                        userPrefs.saveDateOfBirth(dateOfBirth)
                        Log.d("SetupWizardScreen", "User profile saved locally")
                        
                        Log.d("SetupWizardScreen", "Saving medical info locally...")
                        userPrefs.saveMedicalInfo(
                            conditions.joinToString(", "),
                            medications.joinToString(", "),
                            allergies.joinToString(", "),
                            bloodType
                        )
                        Log.d("SetupWizardScreen", "Medical info saved locally")
                        
                        // Mark setup complete locally first
                        userPrefs.setSetupComplete(true)
                        Log.d("SetupWizardScreen", "Setup marked complete locally")
                    } catch (localError: Exception) {
                        Log.e("SetupWizardScreen", "CRITICAL: Failed to save locally", localError)
                        throw Exception("Failed to save profile: ${localError.message}")
                    }
                    
                    // Try to save to Firestore (can fail without blocking)
                    try {
                        Log.d("SetupWizardScreen", "Syncing to Firestore...")
                        val phoneToSave = userPhone.ifEmpty { "" }
                        firestoreRepo.saveUserProfile(name, dateOfBirth, gender, phoneToSave)
                        Log.d("SetupWizardScreen", "User profile synced to Firestore")

                        val medicalInfo = MedicalInfo(
                            userId = userPrefs.getUserId(),
                            dateOfBirth = dateOfBirth,
                            bloodType = bloodType,
                            knownConditions = conditions,
                            currentMedications = medications.map { Medication(it, "", "") },
                            allergies = allergies
                        )
                        firestoreRepo.saveMedicalInfo(medicalInfo)
                        Log.d("SetupWizardScreen", "Medical info synced to Firestore")

                        val contact = EmergencyContact(
                            id = UUID.randomUUID().toString(),
                            name = contactName,
                            phoneNumber = contactPhone,
                            relationship = contactRelation,
                            isPrimaryContact = true
                        )
                        // Save contact using EmergencyRepository (handles local DB + Firestore sync)
                        emergencyRepo.addContact(contact)
                        Log.d("SetupWizardScreen", "Emergency contact saved via Repository")
                    } catch (firestoreError: Exception) {
                        // Log but don't fail - local data is saved
                        Log.w("SetupWizardScreen", "Firestore sync failed (will retry later)", firestoreError)
                        Log.w("SetupWizardScreen", "   Error: ${firestoreError.message}")
                        Toast.makeText(context, "Profile saved locally. Cloud sync will retry later.", Toast.LENGTH_SHORT).show()
                    }
                    
                    Log.d("SetupWizardScreen", "════════════════════════════════════════")
                    Log.d("SetupWizardScreen", "Setup wizard completed successfully")
                    Log.d("SetupWizardScreen", "Navigating to Home")
                    Log.d("SetupWizardScreen", "════════════════════════════════════════")
                    
                    isLoading = false
                    onComplete()
                } catch (e: Exception) {
                    Log.e("SetupWizardScreen", "CRITICAL: Setup wizard failed", e)
                    Log.e("SetupWizardScreen", "   Exception type: ${e.javaClass.simpleName}")
                    Log.e("SetupWizardScreen", "   Exception message: ${e.message}")
                    isLoading = false
                    
                    val errorMsg = when {
                        e.message?.contains("network", ignoreCase = true) == true ->
                            "Network error. Please check your connection and try again."
                        e.message?.contains("permission", ignoreCase = true) == true ->
                            "Permission denied. Please check app permissions."
                        else -> "Error saving profile: ${e.message}"
                    }
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun onBack() {
        if (currentStep > 0) {
            currentStep--
        }
    }

    com.rescuemate.ui.components.CosmicScaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 0) {
                    OutlinedButton(onClick = { onBack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                        Spacer(Modifier.width(8.dp))
                        Text("Back")
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Button(
                    onClick = { onNext() },
                    enabled = canProceed() && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicPrimary,
                        disabledContainerColor = CosmicPrimary.copy(alpha = 0.5f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = CosmicTextPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (currentStep == steps - 1) "Finish" else "Next")
                        Spacer(Modifier.width(8.dp))
                        Icon(if (currentStep == steps - 1) Icons.Default.Check else Icons.Default.ArrowForward, null)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = (currentStep + 1).toFloat() / steps,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(CosmicTextSecondary.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
                color = CosmicPrimary,
            )
            
            Spacer(Modifier.height(32.dp))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "WizardStep"
            ) { step ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (step) {
                        0 -> {
                            Text(
                                "What's your name?",
                                style = MaterialTheme.typography.headlineMedium,
                                color = CosmicTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Let's get you set up.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = CosmicTextSecondary
                            )
                            
                            Spacer(Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = name.isNotEmpty() && ProfanityFilter.containsProfanity(name),
                                supportingText = {
                                    if (name.isNotEmpty() && ProfanityFilter.containsProfanity(name)) {
                                        Text("Name contains inappropriate language", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CosmicPrimary,
                                    unfocusedBorderColor = CosmicTextSecondary
                                )
                            )
                        }
                        1 -> {
                            Text(
                                "Basic Information",
                                style = MaterialTheme.typography.headlineMedium,
                                color = CosmicTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "This helps emergency responders identify you.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = CosmicTextSecondary
                            )
                            
                            OutlinedTextField(
                                value = dateOfBirth,
                                onValueChange = { dateOfBirth = it },
                                label = { Text("Date of Birth (DD/MM/YYYY)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = dateOfBirth.isNotEmpty() && !ValidationUtils.validateDateDDMMYYYY(dateOfBirth).isValid,
                                supportingText = {
                                    if (dateOfBirth.isNotEmpty()) {
                                        val validation = ValidationUtils.validateDateDDMMYYYY(dateOfBirth)
                                        if (!validation.isValid) {
                                            Text(validation.errorMessage ?: "Invalid date", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CosmicPrimary,
                                    unfocusedBorderColor = CosmicTextSecondary
                                )
                            )
                            
                            Text("Gender", color = CosmicTextPrimary, style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                listOf("Male", "Female", "Other").forEach { g ->
                                    val selected = gender == g
                                    FilterChip(
                                        selected = selected,
                                        onClick = { gender = g },
                                        label = { Text(g, color = if (selected) CosmicTextPrimary else CosmicTextSecondary) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CosmicPrimary,
                                            selectedLabelColor = CosmicTextPrimary
                                        )
                                    )
                                }
                            }
                            
                            // Show phone number if user logged in via phone (read-only)
                            if (userPhone.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = userPhone,
                                    onValueChange = {},
                                    label = { Text("Phone Number") },
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = CosmicTextSecondary.copy(alpha = 0.5f),
                                        disabledTextColor = CosmicTextSecondary
                                    )
                                )
                            }
                        }
                        2 -> {
                            Text(
                                "Medical Profile",
                                style = MaterialTheme.typography.headlineMedium,
                                color = CosmicTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Accurate info saves lives.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = CosmicTextSecondary
                            )
                            
                            ExposedDropdownMenuBox(
                                expanded = isBloodTypeExpanded,
                                onExpandedChange = { isBloodTypeExpanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = bloodType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Blood Type (Required)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isBloodTypeExpanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = CosmicPrimary,
                                        unfocusedBorderColor = CosmicTextSecondary
                                    ),
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = isBloodTypeExpanded,
                                    onDismissRequest = { isBloodTypeExpanded = false }
                                ) {
                                    listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-").forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                bloodType = type
                                                isBloodTypeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            SearchableTagSelector(
                                label = "Medical Conditions",
                                allOptions = MedicalDataConstants.getMedicalConditions(),
                                selectedOptions = conditions,
                                onOptionSelected = { conditions = conditions + it },
                                onOptionRemoved = { conditions = conditions - it }
                            )
                            
                            SearchableTagSelector(
                                label = "Allergies",
                                allOptions = MedicalDataConstants.getCommonAllergies(),
                                selectedOptions = allergies,
                                onOptionSelected = { allergies = allergies + it },
                                onOptionRemoved = { allergies = allergies - it }
                            )
                            
                            SearchableTagSelector(
                                label = "Current Medications",
                                allOptions = MedicalDataConstants.getCommonMedications(),
                                selectedOptions = medications,
                                onOptionSelected = { medications = medications + it },
                                onOptionRemoved = { medications = medications - it }
                            )
                        }
                        3 -> {
                            Text(
                                "Emergency Contact",
                                style = MaterialTheme.typography.headlineMedium,
                                color = CosmicTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Who should be notified in case of an emergency?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = CosmicTextSecondary
                            )
                            
                            OutlinedTextField(
                                value = contactName,
                                onValueChange = { contactName = it },
                                label = { Text("Contact Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = contactName.isNotEmpty() && (contactName.any { !it.isLetter() && !it.isWhitespace() } || ProfanityFilter.containsProfanity(contactName)),
                                supportingText = {
                                    if (contactName.isNotEmpty()) {
                                        when {
                                            ProfanityFilter.containsProfanity(contactName) -> 
                                                Text("Name contains inappropriate language", color = MaterialTheme.colorScheme.error)
                                            contactName.any { !it.isLetter() && !it.isWhitespace() } -> 
                                                Text("Letters only", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CosmicPrimary,
                                    unfocusedBorderColor = CosmicTextSecondary
                                )
                            )
                            
                            OutlinedTextField(
                                value = contactPhone,
                                onValueChange = { contactPhone = it },
                                label = { Text("Contact Phone") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                isError = contactPhone.isNotEmpty() && !contactPhone.matches(phoneRegex),
                                supportingText = {
                                    if (contactPhone.isNotEmpty() && !contactPhone.matches(phoneRegex)) {
                                        Text("Invalid phone format", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CosmicPrimary,
                                    unfocusedBorderColor = CosmicTextSecondary
                                )
                            )
                            
                            OutlinedTextField(
                                value = contactRelation,
                                onValueChange = { contactRelation = it },
                                label = { Text("Relationship (e.g., Mom, Spouse)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = contactRelation.isNotEmpty() && ProfanityFilter.containsProfanity(contactRelation),
                                supportingText = {
                                    if (contactRelation.isNotEmpty() && ProfanityFilter.containsProfanity(contactRelation)) {
                                        Text("Contains inappropriate language", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CosmicPrimary,
                                    unfocusedBorderColor = CosmicTextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
