package com.rescuemate.ui.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescuemate.R
import com.rescuemate.data.UserPreferences
import com.rescuemate.data.repository.AuthRepository
import com.rescuemate.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onEmailLogin: () -> Unit = {},
    onPhoneLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPrefs = remember { UserPreferences(context) }
    val authRepo = remember { AuthRepository(context) }
    val isGoogleLoading = remember { mutableStateOf(false) }

    // Google Sign In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isGoogleLoading.value = false  // Reset loading state
        Log.d("SignInScreen", "Google Sign-In launcher result: resultCode=${result.resultCode}")
        
        // Always try to process the intent, even if resultCode is not RESULT_OK
        // This allows us to get the actual error code (like status code 10) instead of just showing "cancelled"
        val intent = result.data
        if (intent != null) {
                scope.launch {
                    try {
                        Log.d("SignInScreen", "Processing Google Sign-In result...")
                        val signInResult = authRepo.signInWithGoogle(intent)
                        
                        if (signInResult.isSuccess) {
                            Log.d("SignInScreen", "Google Sign-In successful")
                            
                            // Explicitly ensure onboarding is marked complete for returning users
                            try {
                                userPrefs.setOnboardingComplete(true)
                                Log.d("SignInScreen", "Onboarding marked complete")
                            } catch (e: Exception) {
                                Log.e("SignInScreen", "Error setting onboarding complete", e)
                            }
                            
                            // Verify login state was saved with retry mechanism
                            var isLoggedIn = userPrefs.isLoggedIn()
                            Log.d("SignInScreen", "Login state after Google auth: $isLoggedIn")

                            // If login state wasn't saved, wait a bit and check again (safety net for any remaining race conditions)
                            if (!isLoggedIn) {
                                Log.w("SignInScreen", "Login state not immediately available, waiting and retrying...")
                                kotlinx.coroutines.delay(100) // Small delay to ensure SharedPreferences commit completed
                                isLoggedIn = userPrefs.isLoggedIn()
                                Log.d("SignInScreen", "Login state after retry: $isLoggedIn")
                            }

                            if (!isLoggedIn) {
                                Log.e("SignInScreen", "Login state not properly saved after Google auth!")
                                Toast.makeText(context, "Authentication error - please try again", Toast.LENGTH_LONG).show()
                                return@launch
                            }
                            
                            Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                            Log.d("SignInScreen", "Navigating to home/setup...")
                            onSignIn()
                        } else {
                            val exception = signInResult.exceptionOrNull()
                            val errorMsg = when {
                                exception?.message?.contains("statusCode=10", ignoreCase = true) == true ||
                                exception?.message?.contains("Code: 10", ignoreCase = true) == true ->
                                    "Configuration error: Please verify Firebase setup and SHA-1 certificate fingerprint"
                                exception?.message?.contains("statusCode=8", ignoreCase = true) == true ||
                                exception?.message?.contains("Code: 8", ignoreCase = true) == true ->
                                    "Google Sign-In service error. Please try again later."
                                exception?.message?.contains("statusCode=7", ignoreCase = true) == true ||
                                exception?.message?.contains("Code: 7", ignoreCase = true) == true ->
                                    "Network error. Please check your internet connection."
                                exception?.message?.contains("statusCode=12500", ignoreCase = true) == true ||
                                exception?.message?.contains("Code: 12500", ignoreCase = true) == true ->
                                    "Sign-in was cancelled"
                                exception?.message?.contains("statusCode=12501", ignoreCase = true) == true ||
                                exception?.message?.contains("Code: 12501", ignoreCase = true) == true ->
                                    "Another sign-in is already in progress"
                                exception?.message?.contains("Web Client ID", ignoreCase = true) == true ->
                                    "Configuration error: Please contact support"
                                exception?.message?.contains("network", ignoreCase = true) == true ->
                                    "Network error. Please check your internet connection."
                                else -> {
                                    Log.e("SignInScreen", "Full error details: ${exception?.javaClass?.simpleName} - ${exception?.message}")
                                    "Sign in failed: ${exception?.message ?: "Unknown error"}"
                                }
                            }
                            
                            Log.e("SignInScreen", "Google Sign-In failed: $errorMsg", exception)
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("SignInScreen", "Critical error in Google Sign-In flow", e)
                        Toast.makeText(context, "Sign in error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else if (result.resultCode != Activity.RESULT_OK) {
            // Only show "cancelled" if there's no intent data AND result is not OK
            // This means the user actually cancelled (back button, etc.)
            Log.w("SignInScreen", "Google Sign-In cancelled by user (resultCode=${result.resultCode})")
            Toast.makeText(context, "Sign in cancelled", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("SignInScreen", "Google Sign-In intent is null")
            Toast.makeText(context, "Sign in failed - no data received", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Function to handle successful sign in (Mock/Email)
    fun handleSuccessfulSignIn(email: String = "user@rescuemate.com") {
        Log.d("SignInScreen", "Starting mock sign-in for: $email")
        Log.w("SignInScreen", "NOTE: This is a MOCK implementation - Apple Sign-In not fully integrated")
        Log.w("SignInScreen", "   Firebase features requiring authentication may not work with mock sign-in")
        try {
            // Save login state
            userPrefs.saveUserCredentials(email, "MOCK_AUTH_PLACEHOLDER")
            Log.d("SignInScreen", "Mock credentials saved")
            
            // Mark onboarding as complete for returning users
            userPrefs.setOnboardingComplete(true)
            Log.d("SignInScreen", "Onboarding marked as complete")
            
            // Verify login state
            val isLoggedIn = userPrefs.isLoggedIn()
            Log.d("SignInScreen", "Login state verified: $isLoggedIn")
            
            if (!isLoggedIn) {
                Log.e("SignInScreen", "Login state not properly saved!")
                Toast.makeText(context, "Login failed - please try again", Toast.LENGTH_SHORT).show()
                return
            }
            
            Log.d("SignInScreen", "Mock sign-in successful, navigating...")
            onSignIn()
        } catch (e: Exception) {
            Log.e("SignInScreen", "Error during mock sign-in", e)
            Toast.makeText(context, "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    com.rescuemate.ui.components.CosmicScaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo - Outlined Shield
            Canvas(modifier = Modifier.size(64.dp)) {
                val shieldPath = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.05f)
                    lineTo(size.width * 0.85f, size.height * 0.2f)
                    lineTo(size.width * 0.85f, size.height * 0.55f)
                    cubicTo(
                        size.width * 0.85f, size.height * 0.75f,
                        size.width * 0.65f, size.height * 0.92f,
                        size.width * 0.5f, size.height * 0.95f
                    )
                    cubicTo(
                        size.width * 0.35f, size.height * 0.92f,
                        size.width * 0.15f, size.height * 0.75f,
                        size.width * 0.15f, size.height * 0.55f
                    )
                    lineTo(size.width * 0.15f, size.height * 0.2f)
                    close()
                }

                drawPath(
                    path = shieldPath,
                    color = Color(0xFFE91E63),
                    style = Stroke(
                        width = 6f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.welcome_back),
                style = MaterialTheme.typography.headlineMedium,
                color = CosmicTextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.sign_in_to_continue),
                style = MaterialTheme.typography.labelSmall,
                color = CosmicTextSecondary,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Sign In Options
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SignInButton(
                    text = if (isGoogleLoading.value) stringResource(R.string.signing_in) else stringResource(R.string.continue_with_google),
                    icon = Icons.Default.Login,
                    enabled = !isGoogleLoading.value,
                    onClick = {
                        if (!isGoogleLoading.value) {
                            isGoogleLoading.value = true
                            val signInIntent = authRepo.getGoogleSignInIntent()
                            googleSignInLauncher.launch(signInIntent)
                        }
                    }
                )
                SignInButton(
                    text = stringResource(R.string.continue_with_apple),
                    icon = Icons.Default.PhoneIphone, // Apple icon alternative
                    onClick = {
                        Log.d("SignInScreen", "Apple Sign-In clicked (Mock)")
                        Toast.makeText(context, "Apple Sign-In (Demo Mode)", Toast.LENGTH_SHORT).show()
                        handleSuccessfulSignIn("apple_user@rescuemate.com")
                    }
                )
                SignInButton(
                    text = stringResource(R.string.continue_with_phone),
                    icon = Icons.Default.Phone,
                    onClick = onPhoneLogin
                )
                SignInButton(
                    text = stringResource(R.string.continue_with_email),
                    icon = Icons.Default.Email,
                    onClick = onEmailLogin // Navigate to email login screen
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(
                    modifier = Modifier.weight(1f),
                    color = CosmicBorder
                )
                Text(
                    text = stringResource(R.string.new_user),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicTextSecondary,
                    letterSpacing = 2.sp
                )
                Divider(
                    modifier = Modifier.weight(1f),
                    color = CosmicBorder
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sign Up Button
            Button(
                onClick = onSignUp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicPrimary
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    text = stringResource(R.string.create_new_account),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Privacy Note
            Text(
                text = stringResource(R.string.terms_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = CosmicTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun SignInButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = CosmicCard,
            contentColor = CosmicTextPrimary
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
