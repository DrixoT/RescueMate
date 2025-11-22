package com.rescuemate.ui.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.rescuemate.data.UserPreferences
import com.rescuemate.data.repository.AuthRepository
import com.rescuemate.ui.theme.CosmicBackground
import com.rescuemate.ui.theme.CosmicPrimary
import com.rescuemate.ui.theme.CosmicTextPrimary
import com.rescuemate.ui.theme.CosmicTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneLoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPrefs = remember { UserPreferences(context) }
    val authRepo = remember { AuthRepository(context) }

    var phone by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var isCodeSent by remember { mutableStateOf(false) }
    var isPhoneVerified by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun sendOtp() {
        if (phone.isBlank()) {
            errorMessage = "Please enter a phone number"
            return
        }
        isLoading = true
        errorMessage = null
        
        // Normalize phone number to E.164 format (remove spaces, dashes, parentheses)
        val normalizedPhone = phone.replace(Regex("[\\s\\-\\(\\)]"), "")
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // For test numbers, Firebase auto-verifies
                isLoading = false
                scope.launch {
                    val result = authRepo.verifyWithCredential(credential)
                    if (result.isSuccess) {
                        isPhoneVerified = true
                        isCodeSent = true
                        userPrefs.saveUserCredentials("phone_${phone}", "PHONE_AUTH_TOKEN")
                        Toast.makeText(context, "Phone verified automatically!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    } else {
                        errorMessage = "Auto-verification failed: ${result.exceptionOrNull()?.message}"
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                isLoading = false
                errorMessage = "Verification failed: ${e.message}"
                Log.e("PhoneLoginScreen", "Verification failed", e)
            }

            override fun onCodeSent(vId: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = vId
                isCodeSent = true
                isLoading = false
                Toast.makeText(context, "Code sent! Check your messages.", Toast.LENGTH_SHORT).show()
            }
        }
        
        authRepo.sendVerificationCode(normalizedPhone, context as Activity, callbacks)
    }

    fun verifyOtp() {
        if (otpCode.isBlank() || verificationId.isBlank()) {
            errorMessage = "Please enter the verification code"
            return
        }
        
        isLoading = true
        errorMessage = null
        scope.launch {
            val result = authRepo.verifyCodeAndSignIn(verificationId, otpCode)
            isLoading = false
            if (result.isSuccess) {
                isPhoneVerified = true
                userPrefs.saveUserCredentials("phone_${phone}", "PHONE_AUTH_TOKEN")
                Toast.makeText(context, "Phone verified successfully!", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Invalid code"
                errorMessage = errorMsg
                Log.e("PhoneLoginScreen", "Verification error: $errorMsg")
            }
        }
    }

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = { Text("Sign in with Phone", color = CosmicTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = CosmicTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = CosmicPrimary
            )
            
            Text(
                "Enter your phone number",
                style = MaterialTheme.typography.headlineSmall,
                color = CosmicTextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "We'll send you a verification code",
                style = MaterialTheme.typography.bodyMedium,
                color = CosmicTextSecondary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!isCodeSent) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (+1...)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicPrimary,
                        unfocusedBorderColor = CosmicTextSecondary
                    )
                )
                
                Button(
                    onClick = { sendOtp() },
                    enabled = phone.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send OTP")
                    }
                }
            } else {
                Text(
                    "Enter the code sent to $phone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CosmicTextSecondary
                )
                
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { otpCode = it },
                    label = { Text("OTP Code") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicPrimary,
                        unfocusedBorderColor = CosmicTextSecondary
                    )
                )
                
                Button(
                    onClick = { verifyOtp() },
                    enabled = otpCode.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Verify")
                    }
                }
            }
            
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Text(
                        errorMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

