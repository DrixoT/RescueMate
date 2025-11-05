package com.rescuemate.emergency.ui

import android.app.DatePickerDialog
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.rescuemate.data.UserPreferences
import com.rescuemate.data.repository.EmergencyRepository
import com.rescuemate.emergency.data.MedicalInfo
import com.rescuemate.emergency.data.Medication
import com.rescuemate.emergency.data.database.EmergencyDatabaseHelper
import com.rescuemate.ui.theme.*
import java.util.*

/**
 * User Medical Profile Screen - COMPREHENSIVE IMPLEMENTATION
 * All 8 critical fixes implemented
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMedicalProfileScreen(
    userId: String = "default_user",
    onNavigateBack: () -> Unit = {},
    onProfileSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val dbHelper = remember { EmergencyDatabaseHelper(context) }
    val userPrefs = remember { UserPreferences(context) }
    val repository = remember { EmergencyRepository(context) }

    Log.d("MedicalProfile", "🎨 Screen loaded for userId: $userId")

    // Load existing data
    val existingData = remember { dbHelper.getMedicalInfo(userId) }

    // Photo State
    var profilePhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showEnlargedPhoto by remember { mutableStateOf(false) }

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

    // Basic Info State
    var fullName by remember { mutableStateOf(userPrefs.getUserName() ?: "") }
    var dateOfBirth by remember { mutableStateOf(existingData?.dateOfBirth ?: "") }
    var selectedSex by remember { mutableStateOf("") }
    var selectedBloodType by remember { mutableStateOf(existingData?.bloodType ?: "") }

    // Medical Info State
    var selectedConditions by remember { mutableStateOf(existingData?.knownConditions ?: emptyList()) }
    var selectedMedications by remember { mutableStateOf<List<Medication>>(existingData?.currentMedications ?: emptyList()) }
    var selectedAllergies by remember { mutableStateOf(existingData?.allergies ?: emptyList()) }

    // Dialog States
    var showConditionsDialog by remember { mutableStateOf(false) }
    var showMedicationsDialog by remember { mutableStateOf(false) }
    var showAllergiesDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Validation
    var showValidationError by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }

    // Save function
    fun saveProfile() {
        Log.d("MedicalProfile", "💾 Save button clicked")

        // Validation
        if (fullName.isBlank()) {
            validationMessage = "Please enter your full name"
            showValidationError = true
            return
        }
        if (dateOfBirth.isBlank()) {
            validationMessage = "Please select your date of birth"
            showValidationError = true
            return
        }
        if (selectedSex.isBlank()) {
            validationMessage = "Please select your sex"
            showValidationError = true
            return
        }
        if (selectedBloodType.isBlank()) {
            validationMessage = "Please select your blood type"
            showValidationError = true
            return
        }

        Log.d("MedicalProfile", "✅ Validation passed - saving data")

        // Save to UserPreferences
        userPrefs.saveUserProfile(
            name = fullName,
            age = calculateAge(dateOfBirth),
            gender = selectedSex,
            phone = userPrefs.getUserPhone() ?: ""
        )
        
        userPrefs.saveDateOfBirth(dateOfBirth)
        
        // Save medical info to UserPreferences as well
        userPrefs.saveMedicalInfo(
            medicalHistory = selectedConditions.joinToString(", "),
            currentMedication = selectedMedications.joinToString(", ") { it.name },
            allergies = selectedAllergies.joinToString(", "),
            bloodType = selectedBloodType
        )

        // Create medical info
        val medicalInfo = MedicalInfo(
            userId = userId,
            dateOfBirth = dateOfBirth,
            bloodType = selectedBloodType,
            knownConditions = selectedConditions,
            currentMedications = selectedMedications,
            allergies = selectedAllergies,
            baselineHeartRate = 70
        )

        // Save to database
        val success = repository.saveMedicalInfo(medicalInfo)

        if (success) {
            Log.d("MedicalProfile", "✅ Medical profile saved successfully!")
            Toast.makeText(context, "✅ Profile saved successfully!", Toast.LENGTH_SHORT).show()
            onProfileSaved()
        } else {
            Log.e("MedicalProfile", "❌ Failed to save profile")
            Toast.makeText(context, "❌ Failed to save profile", Toast.LENGTH_SHORT).show()
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
                    IconButton(onClick = {
                        Log.d("MedicalProfile", "⬅️ Back button clicked")
                        onNavigateBack()
                    }) {
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
                                Log.d("MedicalProfile", "🖼️ Photo clicked - showing enlarged")
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
                    onClick = {
                        Log.d("MedicalProfile", "📸 Change Photo clicked")
                        photoPickerLauncher.launch("image/*")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicPrimary
                    )
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

            // Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    Log.d("MedicalProfile", "📝 Name changed: $it")
                },
                label = { Text("Full Name") },
                leadingIcon = {
                    Icon(Icons.Default.Person, "Name", tint = CosmicPrimary)
                },
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

            // Date of Birth with Calendar Picker
            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = {},
                label = { Text("Date of Birth") },
                placeholder = { Text("DD/MM/YYYY") },
                leadingIcon = {
                    Icon(Icons.Default.CalendarMonth, "DOB", tint = CosmicPrimary)
                },
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

            // Sex and Blood Type Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sex Dropdown
                DropdownSelector(
                    label = "Sex",
                    selectedValue = selectedSex,
                    options = listOf("Male", "Female"),
                    onValueChange = {
                        selectedSex = it
                        Log.d("MedicalProfile", "📝 Sex selected: $it")
                    },
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f)
                )

                // Blood Type Dropdown
                DropdownSelector(
                    label = "Blood Type",
                    selectedValue = selectedBloodType,
                    options = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"),
                    onValueChange = {
                        selectedBloodType = it
                        Log.d("MedicalProfile", "📝 Blood type selected: $it")
                    },
                    icon = Icons.Default.Bloodtype,
                    modifier = Modifier.weight(1f)
                )
            }

            Divider(color = CosmicBorder)

            // Medical Information
            Text(
                text = "Medical Information",
                style = MaterialTheme.typography.titleLarge,
                color = CosmicTextPrimary,
                fontWeight = FontWeight.Bold
            )

            // Medical Conditions
            MedicalInfoSection(
                title = "Medical Conditions",
                icon = Icons.Default.MedicalServices,
                selectedItems = selectedConditions,
                onTap = {
                    Log.d("MedicalProfile", "🏥 Medical Conditions clicked")
                    showConditionsDialog = true
                },
                onRemove = { item ->
                    selectedConditions = selectedConditions.filter { it != item }
                    Log.d("MedicalProfile", "🗑️ Removed condition: $item")
                }
            )

            // Current Medications
            MedicalInfoSection(
                title = "Current Medications",
                icon = Icons.Default.Medication,
                selectedItems = selectedMedications.map { it.name },
                onTap = {
                    Log.d("MedicalProfile", " Medications clicked")
                    showMedicationsDialog = true
                },
                onRemove = { item ->
                    selectedMedications = selectedMedications.filter { it.name != item }
                    Log.d("MedicalProfile", " Removed medication: $item")
                }
            )

            // Allergies
            MedicalInfoSection(
                title = "Allergies",
                icon = Icons.Default.Warning,
                selectedItems = selectedAllergies,
                onTap = {
                    Log.d("MedicalProfile", "⚠️ Allergies clicked")
                    showAllergiesDialog = true
                },
                onRemove = { item ->
                    selectedAllergies = selectedAllergies.filter { it != item }
                    Log.d("MedicalProfile", "🗑️ Removed allergy: $item")
                }
            )

            Spacer(Modifier.height(32.dp))
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
                Log.d("MedicalProfile", "📅 Date selected: $dateOfBirth")
                Toast.makeText(context, "Date selected: $dateOfBirth", Toast.LENGTH_SHORT).show()
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

    // Medical Conditions Dialog
    if (showConditionsDialog) {
        SearchableSelectionDialog(
            title = "Select Medical Conditions",
            items = getMedicalConditions(),
            selectedItems = selectedConditions,
            onDismiss = { showConditionsDialog = false },
            onConfirm = { selected ->
                selectedConditions = selected
                showConditionsDialog = false
                Log.d("MedicalProfile", "✅ Conditions selected: ${selected.size}")
            }
        )
    }

    // Medications Dialog
    if (showMedicationsDialog) {
        SearchableSelectionDialog(
            title = "Select Current Medications",
            items = getCommonMedications(),
            selectedItems = selectedMedications.map { it.name },
            onDismiss = { showMedicationsDialog = false },
            onConfirm = { selected ->
                selectedMedications = selected.map { Medication(name = it, dosage = "", frequency = "") }
                showMedicationsDialog = false
                Log.d("MedicalProfile", "✅ Medications selected: ${selected.size}")
            }
        )
    }

    // Allergies Dialog
    if (showAllergiesDialog) {
        SearchableSelectionDialog(
            title = "Select Allergies",
            items = getCommonAllergies(),
            selectedItems = selectedAllergies,
            onDismiss = { showAllergiesDialog = false },
            onConfirm = { selected ->
                selectedAllergies = selected
                showAllergiesDialog = false
                Log.d("MedicalProfile", "✅ Allergies selected: ${selected.size}")
            }
        )
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

    // Validation Error Dialog
    if (showValidationError) {
        AlertDialog(
            onDismissRequest = { showValidationError = false },
            title = { Text("Validation Error") },
            text = { Text(validationMessage) },
            confirmButton = {
                Button(onClick = { showValidationError = false }) {
                    Text("OK")
                }
            }
        )
    }
}

/**
 * Dropdown Selector Component
 */
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

/**
 * Medical Info Section with Chips Display
 */
@Composable
fun MedicalInfoSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedItems: List<String>,
    onTap: () -> Unit,
    onRemove: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CosmicCard
        ),
        border = BorderStroke(1.dp, CosmicBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTap() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        title,
                        tint = CosmicPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            color = CosmicTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (selectedItems.isEmpty()) "Tap to select" else "${selectedItems.size} selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicTextSecondary
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    "Open",
                    tint = CosmicTextSecondary
                )
            }

            // Display selected items as chips
            if (selectedItems.isNotEmpty()) {
                Divider(color = CosmicBorder)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedItems.forEach { item ->
                        AssistChip(
                            onClick = {},
                            label = { Text(item) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onRemove(item) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        "Remove",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = CosmicPrimary.copy(alpha = 0.2f),
                                labelColor = CosmicTextPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Searchable Selection Dialog with Add Custom Option
 */
@Composable
fun SearchableSelectionDialog(
    title: String,
    items: List<String>,
    selectedItems: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentlySelected by remember { mutableStateOf(selectedItems.toSet()) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var customItem by remember { mutableStateOf("") }
    var customItems by remember { mutableStateOf<Set<String>>(emptySet()) }

    val allItems = items + customItems
    val filteredItems = allItems.filter {
        it.contains(searchQuery, ignoreCase = true)
    }.sorted()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(
                containerColor = CosmicCard
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CosmicPrimary)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, "Search", tint = CosmicPrimary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Clear", tint = CosmicTextSecondary)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicPrimary,
                        unfocusedBorderColor = CosmicBorder,
                        focusedTextColor = CosmicTextPrimary,
                        unfocusedTextColor = CosmicTextPrimary
                    ),
                    singleLine = true
                )

                // Selected count
                if (currentlySelected.isNotEmpty()) {
                    Text(
                        "${currentlySelected.size} selected",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmicPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Divider(color = CosmicBorder)

                // Items List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredItems) { item ->
                        val isSelected = currentlySelected.contains(item)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentlySelected = if (isSelected) {
                                        currentlySelected - item
                                    } else {
                                        currentlySelected + item
                                    }
                                    Log.d("SearchableDialog", "Item toggled: $item, selected: ${!isSelected}")
                                }
                                .background(
                                    if (isSelected) CosmicPrimary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) CosmicPrimary else CosmicTextPrimary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    "Selected",
                                    tint = CosmicPrimary
                                )
                            }
                        }
                        Divider(color = CosmicBorder.copy(alpha = 0.3f))
                    }
                }

                Divider(color = CosmicBorder)

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAddCustomDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CosmicPrimary
                        ),
                        border = BorderStroke(1.dp, CosmicPrimary)
                    ) {
                        Icon(Icons.Default.Add, "Add", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add New")
                    }

                    Button(
                        onClick = {
                            Log.d("SearchableDialog", "✅ Confirmed with ${currentlySelected.size} items")
                            onConfirm(currentlySelected.toList())
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CosmicPrimary
                        )
                    ) {
                        Icon(Icons.Default.Check, "Confirm", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Confirm")
                    }
                }
            }
        }
    }

    // Add Custom Item Dialog
    if (showAddCustomDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            title = { Text("Add Custom Item") },
            text = {
                OutlinedTextField(
                    value = customItem,
                    onValueChange = { customItem = it },
                    placeholder = { Text("Enter custom item...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customItem.isNotBlank()) {
                            customItems = customItems + customItem.trim()
                            currentlySelected = currentlySelected + customItem.trim()
                            Log.d("SearchableDialog", "➕ Custom item added: ${customItem.trim()}")
                            customItem = ""
                            showAddCustomDialog = false
                        }
                    },
                    enabled = customItem.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    customItem = ""
                    showAddCustomDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Calculate age from date of birth
 */
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

/**
 * FlowRow implementation for chips
 */
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }

        var xPos = 0
        var yPos = 0
        var maxHeight = 0
        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()

        placeables.forEach { placeable ->
            if (xPos + placeable.width > constraints.maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow.toList())
                currentRow = mutableListOf()
                xPos = 0
                yPos += maxHeight + 8
                maxHeight = 0
            }

            currentRow.add(placeable)
            xPos += placeable.width + 8
            maxHeight = maxOf(maxHeight, placeable.height)
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val totalHeight = yPos + maxHeight

        layout(constraints.maxWidth, totalHeight) {
            var currentY = 0
            rows.forEach { row ->
                var currentX = 0
                var rowHeight = 0
                row.forEach { placeable ->
                    placeable.placeRelative(currentX, currentY)
                    currentX += placeable.width + 8
                    rowHeight = maxOf(rowHeight, placeable.height)
                }
                currentY += rowHeight + 8
            }
        }
    }
}

/**
 * Medical Conditions Database (2025) - Comprehensive list
 */
fun getMedicalConditions(): List<String> {
    return listOf(
        "Asthma", "Diabetes Type 1", "Diabetes Type 2", "Hypertension",
        "Heart Disease", "Coronary Artery Disease", "Heart Failure", "Stroke",
        "Epilepsy", "Seizure Disorder", "COPD", "Chronic Bronchitis",
        "Emphysema", "Kidney Disease", "Chronic Kidney Disease", "Liver Disease",
        "Cirrhosis", "Hepatitis B", "Hepatitis C", "Thyroid Disorder",
        "Hypothyroidism", "Hyperthyroidism", "Arthritis", "Rheumatoid Arthritis",
        "Osteoarthritis", "Cancer", "Depression", "Anxiety Disorder",
        "Bipolar Disorder", "PTSD", "Sleep Apnea", "Insomnia",
        "Anemia", "Sickle Cell Disease", "Hemophilia", "Lupus",
        "Multiple Sclerosis", "Parkinson's Disease", "Alzheimer's Disease",
        "Dementia", "Autism Spectrum Disorder", "ADHD", "Celiac Disease",
        "Crohn's Disease", "Ulcerative Colitis", "IBS", "GERD",
        "Migraine", "Chronic Pain", "Fibromyalgia", "Osteoporosis",
        "Glaucoma", "Cataracts", "Macular Degeneration", "HIV/AIDS",
        "Tuberculosis", "Pneumonia", "COVID-19 Long Haul", "Obesity",
        "Eating Disorder", "Anorexia", "Bulimia", "Schizophrenia",
        "Substance Use Disorder", "Alcoholism", "None"
    ).sorted()
}

/**
 * Common Medications Database (2025) - Up-to-date list
 */
fun getCommonMedications(): List<String> {
    return listOf(
        "Aspirin", "Ibuprofen", "Acetaminophen", "Naproxen",
        "Metformin", "Insulin", "Lisinopril", "Amlodipine",
        "Atorvastatin", "Simvastatin", "Rosuvastatin", "Metoprolol",
        "Losartan", "Hydrochlorothiazide", "Furosemide", "Warfarin",
        "Apixaban", "Rivaroxaban", "Clopidogrel", "Albuterol",
        "Fluticasone", "Montelukast", "Omeprazole", "Pantoprazole",
        "Esomeprazole", "Levothyroxine", "Gabapentin", "Pregabalin",
        "Sertraline", "Escitalopram", "Fluoxetine", "Duloxetine",
        "Venlafaxine", "Bupropion", "Alprazolam", "Lorazepam",
        "Clonazepam", "Zolpidem", "Trazodone", "Prednisone",
        "Methylprednisolone", "Amoxicillin", "Azithromycin", "Ciprofloxacin",
        "Doxycycline", "Cephalexin", "Tamsulosin", "Finasteride",
        "Sildenafil", "Tadalafil", "Methotrexate", "Adalimumab",
        "Etanercept", "Infliximab", "Hydroxychloroquine", "Cyclobenzaprine",
        "Meloxicam", "Tramadol", "Oxycodone", "Hydrocodone",
        "Morphine", "Fentanyl", "Vitamin D", "Calcium",
        "Multivitamin", "Fish Oil", "Probiotic", "None"
    ).sorted()
}

/**
 * Common Allergies Database (2025) - Comprehensive allergy list
 */
fun getCommonAllergies(): List<String> {
    return listOf(
        // Drug Allergies
        "Penicillin", "Amoxicillin", "Ampicillin", "Sulfa drugs",
        "Aspirin", "Ibuprofen", "Naproxen", "Codeine",
        "Morphine", "Latex", "Anesthesia", "Contrast Dye", "Iodine",

        // Food Allergies
        "Peanuts", "Tree Nuts", "Milk/Dairy", "Eggs",
        "Wheat/Gluten", "Soy", "Fish", "Shellfish",
        "Sesame", "Corn", "Tomatoes", "Strawberries", "Chocolate",

        // Environmental Allergies
        "Pollen", "Grass", "Ragweed", "Dust Mites",
        "Mold", "Pet Dander (Cats)", "Pet Dander (Dogs)",
        "Bee Stings", "Wasp Stings", "Fire Ant Stings", "Mosquito Bites",

        // Other Common Allergies
        "Nickel", "Fragrances", "Preservatives", "Food Dyes",
        "MSG", "Sulfites",

        "No Known Allergies"
    ).sorted()
}

