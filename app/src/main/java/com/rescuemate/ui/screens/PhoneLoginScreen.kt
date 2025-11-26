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
        
        // Normalize phone number to E.164 format (remove spaces, dashes, parentheses)
        val normalizedPhone = phone.replace(Regex("[\\s\\-\\(\\)]"), "")
        
        // Validate phone number format (E.164: +[country code][number])
        if (!normalizedPhone.matches(Regex("^\\+[1-9]\\d{1,14}$"))) {
            errorMessage = "Please enter a valid phone number with country code (e.g., +1234567890)"
            Log.w("PhoneLoginScreen", "Invalid phone number format: $phone (normalized: $normalizedPhone)")
            return
        }
        
        Log.d("PhoneLoginScreen", "Sending OTP to: ${normalizedPhone.take(5)}...")
        isLoading = true
        errorMessage = null
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // For test numbers, Firebase auto-verifies
                isLoading = false
                Log.d("PhoneLoginScreen", "Phone auto-verification completed")
                scope.launch {
                    val result = authRepo.verifyWithCredential(credential)
                    if (result.isSuccess) {
                        isPhoneVerified = true
                        isCodeSent = true
                        userPrefs.saveUserCredentials("phone_${phone}", "PHONE_AUTH_TOKEN")
                        Log.d("PhoneLoginScreen", "Phone verified automatically")
                        Toast.makeText(context, "Phone verified automatically!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        errorMessage = "Auto-verification failed: $error"
                        Log.e("PhoneLoginScreen", "Auto-verification failed: $error")
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                isLoading = false
                val errorMsg = when {
                    e.message?.contains("quota", ignoreCase = true) == true ||
                    e.message?.contains("exceeded", ignoreCase = true) == true ->
                        "SMS quota exceeded. Please try again later."
                    e.message?.contains("invalid", ignoreCase = true) == true ||
                    e.message?.contains("format", ignoreCase = true) == true ->
                        "Invalid phone number format. Please check and try again."
                    e.message?.contains("network", ignoreCase = true) == true ||
                    e.message?.contains("connection", ignoreCase = true) == true ->
                        "Network error. Please check your connection."
                    e.message?.contains("too many", ignoreCase = true) == true ->
                        "Too many attempts. Please try again later."
                    else -> "Verification failed: ${e.message ?: "Unknown error"}"
                }
                errorMessage = errorMsg
                Log.e("PhoneLoginScreen", "Phone verification failed: ${e.javaClass.simpleName}", e)
                Log.e("PhoneLoginScreen", "   Error message: ${e.message}")
                Log.e("PhoneLoginScreen", "   User message: $errorMsg")
            }

            override fun onCodeSent(vId: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = vId
                isCodeSent = true
                isLoading = false
                Log.d("PhoneLoginScreen", "Verification code sent successfully")
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
        
        Log.d("PhoneLoginScreen", "Verifying OTP code...")
        isLoading = true
        errorMessage = null
        scope.launch {
            val result = authRepo.verifyCodeAndSignIn(verificationId, otpCode)
            isLoading = false
            if (result.isSuccess) {
                isPhoneVerified = true
                userPrefs.saveUserCredentials("phone_${phone}", "PHONE_AUTH_TOKEN")
                Log.d("PhoneLoginScreen", "Phone verified successfully")
                Toast.makeText(context, "Phone verified successfully!", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
            } else {
                val exception = result.exceptionOrNull()
                val errorMsg = when {
                    exception?.message?.contains("invalid", ignoreCase = true) == true ||
                    exception?.message?.contains("code", ignoreCase = true) == true ->
                        "Invalid verification code. Please check and try again."
                    exception?.message?.contains("expired", ignoreCase = true) == true ->
                        "Verification code expired. Please request a new code."
                    exception?.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Please check your connection."
                    else -> exception?.message ?: "Invalid code"
                }
                errorMessage = errorMsg
                Log.e("PhoneLoginScreen", "OTP verification error: ${exception?.javaClass?.simpleName}", exception)
                Log.e("PhoneLoginScreen", "   Error message: ${exception?.message}")
                Log.e("PhoneLoginScreen", "   User message: $errorMsg")
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

