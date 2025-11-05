package com.rescuemate.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescuemate.R
import com.rescuemate.ui.theme.*

@Composable
fun SignInScreen(
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onEmailLogin: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val userPrefs = remember { com.rescuemate.data.UserPreferences(context) }
    
    // Function to handle successful sign in
    fun handleSuccessfulSignIn(email: String = "user@rescuemate.com") {
        // Save login state
        userPrefs.saveUserCredentials(email, "hashed_password")
        userPrefs.setOnboardingComplete(true)
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
                    icon = Icons.Default.Login, // TODO: Replace with Google logo
                    onClick = { handleSuccessfulSignIn("google_user@rescuemate.com") }
                )
                SignInButton(
                    text = stringResource(R.string.continue_with_apple),
                    icon = Icons.Default.PhoneIphone, // Apple icon alternative
                    onClick = { handleSuccessfulSignIn("apple_user@rescuemate.com") }
                )
                SignInButton(
                    text = stringResource(R.string.continue_with_phone),
                    icon = Icons.Default.Phone,
                    onClick = { handleSuccessfulSignIn("phone_user@rescuemate.com") }
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

