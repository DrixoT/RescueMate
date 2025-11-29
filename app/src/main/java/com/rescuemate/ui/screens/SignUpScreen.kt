package com.rescuemate.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescuemate.R
import com.rescuemate.data.UserPreferences
import com.rescuemate.data.repository.AuthRepository
import com.rescuemate.ui.theme.*
import com.rescuemate.utils.ValidationUtils
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val authRepo = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Log.d("SignUpScreen", "Screen rendered")

    // Function to save user profile
    fun performSignUp() {
        Log.d("SignUpScreen", "Starting validation and sign up")

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

        Log.d("SignUpScreen", "Validation passed - Creating account")

        isLoading = true
        errorMessage = null

        scope.launch {
            try {
                Log.d("SignUpScreen", "Starting Firebase account creation for: ${email.trim()}")
                
                // Create account with Firebase
                val signUpResult = authRepo.signUpWithEmail(email.trim(), password)
                
                if (signUpResult.isSuccess) {
                    Log.d("SignUpScreen", "Firebase account created successfully")
                    
                    // Mark onboarding as complete so they don't see intro slides again
                    // But DO NOT mark setup as complete, so they go to SetupWizard
                    try {
                        userPrefs.setOnboardingComplete(true)
                        Log.d("SignUpScreen", "Onboarding marked as complete")
                    } catch (e: Exception) {
                        Log.e("SignUpScreen", "Error setting onboarding complete", e)
                        // Non-critical error
                    }

                    Log.d("SignUpScreen", "Navigating to Setup Wizard")

                    Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()

                    // Navigate to Setup Wizard (via onComplete)
                    onComplete()
                } else {
                    val exception = signUpResult.exceptionOrNull()
                    val errorMsg = when {
                        exception?.message?.contains("email address is already in use", ignoreCase = true) == true ->
                            "This email is already registered. Please sign in instead."
                        exception?.message?.contains("network", ignoreCase = true) == true ->
                            "Network error. Please check your internet connection."
                        exception?.message?.contains("weak-password", ignoreCase = true) == true ->
                            "Password is too weak. Please use a stronger password."
                        exception?.message?.contains("invalid-email", ignoreCase = true) == true ->
                            "Invalid email format. Please check your email."
                        else -> "Registration failed: ${exception?.message ?: "Unknown error"}"
                    }
                    
                    Log.e("SignUpScreen", "Registration error: $errorMsg", exception)
                    errorMessage = errorMsg
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("SignUpScreen", "Critical exception during registration", e)
                val errorMsg = when {
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Please check your connection and try again."
                    e.message?.contains("permission", ignoreCase = true) == true ->
                        "Permission error. Please check app permissions."
                    else -> "Error: ${e.message ?: "Unknown error occurred"}"
                }
                errorMessage = errorMsg
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
                Log.d("SignUpScreen", "Registration flow completed (loading=$isLoading)")
            }
        }
    }

    com.rescuemate.ui.components.CosmicScaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        text = "Get Started",
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
                    onClick = { performSignUp() },
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
                            text = "Continue to Setup",
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
