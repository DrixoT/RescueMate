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
import androidx.compose.runtime.Composable
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

    // Google Sign In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            if (intent != null) {
                scope.launch {
                    val signInResult = authRepo.signInWithGoogle(intent)
                    if (signInResult.isSuccess) {
                        Log.d("SignInScreen", "✅ Google Sign-In successful")
                        
                        // Explicitly ensure onboarding is marked complete for returning users
                        userPrefs.setOnboardingComplete(true)
                        
                        // Verify login state was saved
                        val isLoggedIn = userPrefs.isLoggedIn()
                        Log.d("SignInScreen", "Login state after Google auth: $isLoggedIn")
                        
                        Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                        onSignIn()
                    } else {
                        Log.e("SignInScreen", "❌ Google Sign-In failed", signInResult.exceptionOrNull())
                        Toast.makeText(context, "Sign in failed: ${signInResult.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    // Function to handle successful sign in (Mock/Email)
    fun handleSuccessfulSignIn(email: String = "user@rescuemate.com") {
        // Save login state
        userPrefs.saveUserCredentials(email, "hashed_password")
        // userPrefs.setOnboardingComplete(true) // Done by intro/setup flow now
        android.util.Log.d("SignInScreen", "✅ User signed in and saved credentials")
        onSignIn()
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
            modifier = Modifier
                .fillMaxSize()
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
                    text = stringResource(R.string.continue_with_google),
                    icon = Icons.Default.Login, 
                    onClick = { 
                        val signInIntent = authRepo.getGoogleSignInIntent()
                        googleSignInLauncher.launch(signInIntent)
                    }
                )
                SignInButton(
                    text = stringResource(R.string.continue_with_apple),
                    icon = Icons.Default.PhoneIphone, // Apple icon alternative
                    onClick = { handleSuccessfulSignIn("apple_user@rescuemate.com") }
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
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
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
