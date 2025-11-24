package com.rescuemate.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.rescuemate.data.UserPreferences
import com.rescuemate.data.repository.AuthRepository
import com.rescuemate.data.repository.EmergencyRepository
import com.rescuemate.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EmailLoginScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onSignUp: () -> Unit
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val repository = remember { EmergencyRepository(context) }
    val authRepo = remember { AuthRepository(context) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Log.d("EmailLoginScreen", "🎨 Screen rendered")

    // Login function
    fun performLogin() {
        Log.d("EmailLoginScreen", "🔐 SIGN IN CLICKED - Starting validation")
        Log.d("EmailLoginScreen", "📝 Email: '$email'")

        // Validate email
        val emailValidation = repository.validateEmail(email)
        if (!emailValidation.isValid) {
            Log.w("EmailLoginScreen", "❌ Email validation failed: ${emailValidation.message}")
            errorMessage = emailValidation.message
            Toast.makeText(context, "❌ ${emailValidation.message}", Toast.LENGTH_LONG).show()
            return
        }

        // Validate password
        val passwordValidation = repository.validatePassword(password)
        if (!passwordValidation.isValid) {
            Log.w("EmailLoginScreen", "❌ Password validation failed: ${passwordValidation.message}")
            errorMessage = passwordValidation.message
            Toast.makeText(context, "❌ ${passwordValidation.message}", Toast.LENGTH_LONG).show()
            return
        }

        Log.d("EmailLoginScreen", "✅ Validation passed - Processing login")

        isLoading = true
        errorMessage = null

        scope.launch {
            try {
                val result = authRepo.signInWithEmail(email.trim(), password)
                
                if (result.isSuccess) {
                    Log.d("EmailLoginScreen", "✅ Firebase sign-in successful")
                    
                    // Explicitly mark onboarding as complete
                    try {
                        userPrefs.setOnboardingComplete(true)
                        Log.d("EmailLoginScreen", "✅ Onboarding marked complete")
                    } catch (e: Exception) {
                        Log.e("EmailLoginScreen", "❌ Error setting onboarding complete", e)
                    }
                    
                    // Verify login state
                    val isLoggedIn = userPrefs.isLoggedIn()
                    Log.d("EmailLoginScreen", "Login state verified: $isLoggedIn")
                    
                    if (!isLoggedIn) {
                        Log.e("EmailLoginScreen", "❌ Login state not properly saved!")
                        errorMessage = "Authentication error - please try again"
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    
                    Toast.makeText(context, "✅ Logged in successfully", Toast.LENGTH_SHORT).show()
                    Log.d("EmailLoginScreen", "✅ LOGIN SUCCESSFUL")
                    Log.d("EmailLoginScreen", "🧭 Navigating to home screen")
                    
                    // Use onLogin callback to navigate
                    onLogin()
                } else {
                    val exception = result.exceptionOrNull()
                    val errorMsg = when {
                        exception?.message?.contains("no user record", ignoreCase = true) == true ||
                        exception?.message?.contains("user not found", ignoreCase = true) == true ||
                        exception?.message?.contains("ERROR_USER_NOT_FOUND", ignoreCase = true) == true ->
                            "Account not found. Please create an account."
                        exception?.message?.contains("wrong password", ignoreCase = true) == true ||
                        exception?.message?.contains("invalid-credential", ignoreCase = true) == true ||
                        exception?.message?.contains("ERROR_WRONG_PASSWORD", ignoreCase = true) == true ||
                        exception?.message?.contains("ERROR_INVALID_CREDENTIAL", ignoreCase = true) == true ->
                            "Incorrect password. Please try again."
                        exception?.message?.contains("network", ignoreCase = true) == true ||
                        exception?.message?.contains("ERROR_NETWORK", ignoreCase = true) == true ->
                            "Network error. Please check your connection."
                        exception?.message?.contains("too many requests", ignoreCase = true) == true ||
                        exception?.message?.contains("ERROR_TOO_MANY_REQUESTS", ignoreCase = true) == true ->
                            "Too many failed attempts. Please try again later."
                        exception?.message?.contains("invalid-email", ignoreCase = true) == true ||
                        exception?.message?.contains("ERROR_INVALID_EMAIL", ignoreCase = true) == true ->
                            "Invalid email format. Please check your email."
                        else -> {
                            Log.e("EmailLoginScreen", "Full error details: ${exception?.javaClass?.simpleName} - ${exception?.message}")
                            "Login failed: ${exception?.message ?: "Unknown error"}"
                        }
                    }
                    
                    Log.e("EmailLoginScreen", "❌ Login error: $errorMsg", exception)
                    errorMessage = errorMsg
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("EmailLoginScreen", "❌ Critical error during login", e)
                val errorMsg = when {
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Please check your connection."
                    else -> "Error: ${e.message ?: "Unknown error occurred"}"
                }
                errorMessage = errorMsg
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
                Log.d("EmailLoginScreen", "🏁 Login flow completed")
            }
        }
    }

    // Animated entry
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
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
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Back Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = {
                            visible = false
                            scope.launch {
                                delay(300)
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = CosmicTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.headlineLarge,
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your credentials to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CosmicTextSecondary
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                        Log.d("EmailLoginScreen", "📝 Email input: '$it'")
                    },
                    label = { Text("Email") },
                    placeholder = { Text("your.email@example.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = CosmicPrimary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage?.contains("email", ignoreCase = true) == true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CosmicCard,
                        unfocusedContainerColor = CosmicCard,
                        focusedIndicatorColor = CosmicPrimary,
                        unfocusedIndicatorColor = CosmicBorder,
                        focusedLabelColor = CosmicPrimary,
                        unfocusedLabelColor = CosmicTextSecondary,
                        cursorColor = CosmicPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                        Log.d("EmailLoginScreen", "📝 Password input changed (length: ${it.length})")
                    },
                    label = { Text("Password") },
                    placeholder = { Text("Enter your password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CosmicPrimary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility
                                             else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password"
                                                    else "Show password",
                                tint = CosmicTextSecondary
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                                          else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage?.contains("password", ignoreCase = true) == true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            performLogin()
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CosmicCard,
                        unfocusedContainerColor = CosmicCard,
                        focusedIndicatorColor = CosmicPrimary,
                        unfocusedIndicatorColor = CosmicBorder,
                        focusedLabelColor = CosmicPrimary,
                        unfocusedLabelColor = CosmicTextSecondary,
                        cursorColor = CosmicPrimary
                    )
                )

                // Error Message
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    errorMessage?.let { message ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Forgot Password
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { /* Handle forgot password */ }) {
                        Text(
                            text = "Forgot Password?",
                            color = CosmicPrimary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Sign In Button
                Button(
                    onClick = { performLogin() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicPrimary
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = androidx.compose.ui.graphics.Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Login,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign In",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sign Up Prompt
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmicTextSecondary
                    )
                    TextButton(onClick = onSignUp) {
                        Text(
                            text = "Sign Up",
                            color = CosmicPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

