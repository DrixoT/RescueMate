package com.rescuemate.ui.screens

import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescuemate.R
import com.rescuemate.data.UserPreferences
import com.rescuemate.data.repository.AuthRepository
import com.rescuemate.data.repository.EmergencyRepository
import com.rescuemate.ui.theme.*
import com.rescuemate.utils.ValidationUtils
import com.rescuemate.utils.ProfanityFilter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val repository = remember { EmergencyRepository(context) }
    val authRepo = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var medicalHistory by remember { mutableStateOf("") }
    var currentMedication by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Calculate age from date of birth
    fun calculateAge(dob: String): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val birthDate = LocalDate.parse(dob, formatter)
            val age = Period.between(birthDate, LocalDate.now()).years
            age.toString()
        } catch (e: Exception) {
            ""
        }
    }

    Log.d("SignUpScreen", "🎨 Screen rendered")

    // Function to save user profile
    fun saveUserProfile() {
        Log.d("SignUpScreen", "Starting validation and save")

        // Validate date with proper date validation
        val dateValidation = ValidationUtils.validateDateDDMMYYYY(dateOfBirth)
        if (!dateValidation.isValid) {
            errorMessage = dateValidation.errorMessage
            Toast.makeText(context, dateValidation.errorMessage, Toast.LENGTH_LONG).show()
            return
        }

        val calculatedAge = calculateAge(dateOfBirth)
        if (calculatedAge.isEmpty()) {
            errorMessage = "Please enter a valid date of birth (DD/MM/YYYY)"
            Toast.makeText(context, "Please enter a valid date of birth", Toast.LENGTH_LONG).show()
            return
        }
        
        // Check for profanity in name
        if (ProfanityFilter.containsProfanity(name)) {
            errorMessage = "Name contains inappropriate language"
            Toast.makeText(context, "Name contains inappropriate language", Toast.LENGTH_LONG).show()
            return
        }

        // Validate Email and Password
        val emailValidation = ValidationUtils.validateEmail(email)
        if (!emailValidation.isValid) {
            errorMessage = emailValidation.errorMessage
            return
        }

        val passwordValidation = ValidationUtils.validatePassword(password)
        if (!passwordValidation.isValid) {
            errorMessage = passwordValidation.errorMessage
            return
        }

        if (password != confirmPassword) {
            errorMessage = "Passwords do not match"
            return
        }

        Log.d("SignUpScreen", "Input values: name='$name', dob='$dateOfBirth', calculated age='$calculatedAge', gender='$gender', phone='$phone'")

        // Validate required fields
        val validation = repository.validateUserProfile(name, calculatedAge, gender, phone)
        if (!validation.isValid) {
            Log.w("SignUpScreen", "Validation failed: ${validation.message}")
            errorMessage = validation.message
            Toast.makeText(context, validation.message, Toast.LENGTH_LONG).show()
            return
        }

        Log.d("SignUpScreen", "Validation passed - Creating account")

        isLoading = true
        errorMessage = null

        scope.launch {
            try {
                // Create account with Firebase
                val signUpResult = authRepo.signUpWithEmail(email.trim(), password)
                
                if (signUpResult.isSuccess) {
                    Log.d("SignUpScreen", "✅ Account created successfully")
                    
                    // Save user profile to SharedPreferences
                    Log.d("SignUpScreen", "Saving user profile to SharedPreferences")
                    userPrefs.saveUserProfile(
                        name = name.trim(),
                        age = calculatedAge,
                        gender = gender,
                        phone = phone.trim()
                    )
                    userPrefs.saveDateOfBirth(dateOfBirth.trim())
                    
                    // Save medical information
                    userPrefs.saveMedicalInfo(
                        medicalHistory = medicalHistory.trim(),
                        currentMedication = currentMedication.trim(),
                        allergies = allergies.trim()
                    )
                    
                    // Save login credentials locally (optional, Firebase SDK manages session)
                    // But we do it to keep local consistency with existing logic
                    // We store a hash or token, but since we just signed up, we can skip manual credential saving 
                    // or save email for display. The AuthRepository updates local user data on login/signup.

                    // Mark onboarding as complete
                    userPrefs.setOnboardingComplete(true)

                    Log.d("SignUpScreen", "ALL DATA SAVED SUCCESSFULLY")
                    Log.d("SignUpScreen", "Navigating to Setup Wizard")

                    Toast.makeText(context, "Registration complete!", Toast.LENGTH_SHORT).show()

                    // Navigate to Setup Wizard (via onComplete)
                    onComplete()
                } else {
                    val error = signUpResult.exceptionOrNull()?.message ?: "Registration failed"
                    Log.e("SignUpScreen", "❌ Registration error: $error")
                    errorMessage = "Registration failed: $error"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("SignUpScreen", "Exception during save: ${e.message}", e)
                errorMessage = "Error saving data: ${e.message}"
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

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
                        text = "Account Credentials",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 2.sp
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text("Email Address *") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = (errorMessage?.contains("email", ignoreCase = true) == true)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Password *") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image = if (passwordVisible)
                                Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff

                            val description = if (passwordVisible) "Hide password" else "Show password"

                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, description)
                            }
                        },
                        isError = (errorMessage?.contains("password", ignoreCase = true) == true)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = null
                        },
                        label = { Text("Confirm Password *") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image = if (confirmPasswordVisible)
                                Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff

                            val description = if (confirmPasswordVisible) "Hide password" else "Show password"

                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(imageVector = image, description)
                            }
                        },
                        isError = (errorMessage?.contains("match", ignoreCase = true) == true)
                    )

                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 2.sp
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                            Log.d("SignUpScreen", "📝 Name input: '$it'")
                        },
                        label = { Text("Full Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        singleLine = true,
                        isError = (errorMessage?.contains("name", ignoreCase = true) == true) || 
                                  (name.isNotEmpty() && ProfanityFilter.containsProfanity(name)),
                        supportingText = {
                            if (name.isNotEmpty() && ProfanityFilter.containsProfanity(name)) {
                                Text("Name contains inappropriate language", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = dateOfBirth,
                            onValueChange = {
                                dateOfBirth = it
                                errorMessage = null
                                Log.d("SignUpScreen", "Date of Birth input: '$it'")
                            },
                            label = { Text("Date of Birth (DD/MM/YYYY) *") },
                            placeholder = { Text("31/12/1990") },
                            modifier = Modifier.weight(1f),
                            colors = textFieldColors(),
                            singleLine = true,
                            isError = (errorMessage?.contains("date", ignoreCase = true) == true || 
                                      errorMessage?.contains("birth", ignoreCase = true) == true) ||
                                      (dateOfBirth.isNotEmpty() && !ValidationUtils.validateDateDDMMYYYY(dateOfBirth).isValid),
                            supportingText = {
                                if (dateOfBirth.isNotEmpty()) {
                                    val validation = ValidationUtils.validateDateDDMMYYYY(dateOfBirth)
                                    if (!validation.isValid) {
                                        Text(validation.errorMessage ?: "Invalid date", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
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
                                isError = errorMessage?.contains("gender", ignoreCase = true) == true,
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
                                                errorMessage = null
                                                Log.d("SignUpScreen", "📝 Gender selected: '$option'")
                                            }
                                        )
                                    }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            errorMessage = null
                            Log.d("SignUpScreen", "📝 Phone input: '$it'")
                        },
                        label = { Text("Phone Number *") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        singleLine = true,
                        isError = errorMessage?.contains("phone", ignoreCase = true) == true
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
                        onValueChange = {
                            allergies = it
                            Log.d("SignUpScreen", "📝 Allergies input: '${it.take(30)}...'")
                        },
                        label = { Text("Allergies") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        minLines = 3
                    )
                }

                // Error Message Display
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Submit Button
                Button(
                    onClick = { saveUserProfile() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicPrimary
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = androidx.compose.ui.graphics.Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Complete Registration",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
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

