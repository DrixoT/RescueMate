package com.rescuemate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rescuemate.R
import com.rescuemate.ui.theme.*

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var medicalHistory by remember { mutableStateOf("") }
    var currentMedication by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }

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
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = CosmicTextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.titleLarge,
                        color = CosmicTextPrimary
                    )
                    Text(
                        text = "Your Safety Profile",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 2.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = CosmicPrimary
                )
            }

            // Form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Personal Information Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 2.sp
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            label = { Text("Age *") },
                            modifier = Modifier.weight(1f),
                            colors = textFieldColors(),
                            singleLine = true
                        )

                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = gender,
                                onValueChange = {},
                                label = { Text("Gender *") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = textFieldColors(),
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null
                                        )
                                    }
                                }
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                listOf("Male", "Female", "Non-binary", "Prefer not to say")
                                    .forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                gender = option
                                                expanded = false
                                            }
                                        )
                                    }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number *") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        singleLine = true
                    )
                }

                // Medical Information Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Medical Information",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicTextSecondary,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "This information will be shared with emergency responders",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicTextSecondary
                        )
                    }

                    OutlinedTextField(
                        value = medicalHistory,
                        onValueChange = { medicalHistory = it },
                        label = { Text("Medical History") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        minLines = 4
                    )

                    OutlinedTextField(
                        value = currentMedication,
                        onValueChange = { currentMedication = it },
                        label = { Text("Current Medication") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        minLines = 3
                    )

                    OutlinedTextField(
                        value = allergies,
                        onValueChange = { allergies = it },
                        label = { Text("Allergies") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        minLines = 3
                    )
                }

                // Submit Button
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicPrimary
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = "Complete Registration",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = CosmicTextPrimary,
    unfocusedTextColor = CosmicTextPrimary,
    focusedBorderColor = CosmicPrimary,
    unfocusedBorderColor = CosmicBorder,
    focusedLabelColor = CosmicTextSecondary,
    unfocusedLabelColor = CosmicTextSecondary,
    focusedContainerColor = CosmicCard,
    unfocusedContainerColor = CosmicCard
)

