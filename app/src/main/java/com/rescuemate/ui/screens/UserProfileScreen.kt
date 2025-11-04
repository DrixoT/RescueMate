package com.rescuemate.ui.screens

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
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }
    var currentMedication by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var selectedConditions by remember { mutableStateOf(setOf<String>()) }
    var showConditionsDialog by remember { mutableStateOf(false) }
    var showMedicationDialog by remember { mutableStateOf(false) }
    var showAllergiesDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CosmicBackground,
                        CosmicCard,
                        CosmicCardHover
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    IconButton(onClick = { /* Save */ }) {
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
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(50.dp),
                                tint = CosmicTextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { /* Change photo */ }) {
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

                // Medical Conditions
                ProfileClickableField(
                    label = "Medical Conditions",
                    value = if (selectedConditions.isEmpty()) "Tap to select"
                           else selectedConditions.joinToString(", "),
                    icon = Icons.Default.MedicalServices,
                    onClick = { showConditionsDialog = true }
                )

                // Current Medications
                ProfileClickableField(
                    label = "Current Medications",
                    value = currentMedication.ifEmpty { "Tap to add" },
                    icon = Icons.Default.Medication,
                    onClick = { showMedicationDialog = true }
                )

                // Allergies
                ProfileClickableField(
                    label = "Allergies",
                    value = allergies.ifEmpty { "Tap to add" },
                    icon = Icons.Default.Warning,
                    onClick = { showAllergiesDialog = true }
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
        colors = TextFieldDefaults.outlinedTextFieldColors(
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

