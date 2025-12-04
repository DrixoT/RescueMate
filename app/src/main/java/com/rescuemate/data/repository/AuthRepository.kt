package com.rescuemate.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.rescuemate.R
import com.rescuemate.data.UserPreferences
import com.rescuemate.data.repository.FCMRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import android.app.Activity
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class AuthRepository(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val userPrefs = UserPreferences(context)
    private val fcmRepository = FCMRepository(context)
    private val googleSignInClient: GoogleSignInClient

    init {
        // Get Web Client ID from auto-generated resources (recommended approach)
        // The Google Services plugin automatically generates R.string.default_web_client_id
        // from google-services.json during build time
        val webClientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            // Fallback to hardcoded value if resource not found
            Log.w("AuthRepository", "Could not find default_web_client_id resource, using fallback")
            "1085665199694-urhu4004f6lq8bb1hgha5vbqh06ubssp.apps.googleusercontent.com"
        }

        // IMPORTANT: SHA-1 fingerprint verification for developers
        // The google-services.json contains SHA-1: 06396e57da76d407f5a86936e0dddd4dacf35885
        // Verify this matches your keystore:
        // Debug: keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
        // Release: keytool -list -v -keystore your-release-key.keystore -alias your-alias
        // If SHA-1 doesn't match, add the correct fingerprint in Firebase Console:
        // Project Settings > Your apps > Android app > SHA certificate fingerprints

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
        Log.d("AuthRepository", "Google Sign-In client initialized")
    }
    
    

    // ============================================
    // EMAIL AUTHENTICATION
    // ============================================
    
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            Log.d("AuthRepository", "Starting email sign-in for: ${email.take(10)}...")
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Firebase User is null")
            
            Log.d("AuthRepository", "Email sign-in successful for user: ${user.uid}")
            
            // Update local preferences
            updateLocalUser(user)
            
            Result.success(user)
        } catch (e: FirebaseAuthException) {
            // Handle Firebase-specific authentication errors
            val errorCode = e.errorCode
            Log.e("AuthRepository", "Email Sign-In FirebaseAuthException")
            Log.e("AuthRepository", "   Error code: $errorCode")
            Log.e("AuthRepository", "   Error message: ${e.message}")
            
            // Create a more descriptive error message based on Firebase error code
            val errorMessage = when (errorCode) {
                "ERROR_USER_NOT_FOUND" -> "Account not found. Please create an account."
                "ERROR_WRONG_PASSWORD" -> "Incorrect password. Please try again."
                "ERROR_INVALID_EMAIL" -> "Invalid email format. Please check your email."
                "ERROR_TOO_MANY_REQUESTS" -> "Too many failed attempts. Please try again later."
                "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your connection."
                "ERROR_INVALID_CREDENTIAL" -> "Invalid credentials. Please check your email and password."
                "ERROR_OPERATION_NOT_ALLOWED" -> "Email/password sign-in is not enabled. Please contact support."
                "ERROR_USER_DISABLED" -> "This account has been disabled. Please contact support."
                else -> e.message ?: "Authentication failed. Please try again."
            }
            
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Email Sign-In failed: ${e.javaClass.simpleName}", e)
            Log.e("AuthRepository", "   Error message: ${e.message}")
            
            // Extract Firebase error code if available in message (fallback for non-FirebaseAuthException)
            val errorCode = e.message?.let { msg ->
                when {
                    msg.contains("ERROR_USER_NOT_FOUND", ignoreCase = true) ||
                    msg.contains("no user record", ignoreCase = true) ||
                    msg.contains("user not found", ignoreCase = true) -> "USER_NOT_FOUND"
                    msg.contains("ERROR_WRONG_PASSWORD", ignoreCase = true) ||
                    msg.contains("wrong password", ignoreCase = true) ||
                    msg.contains("invalid-credential", ignoreCase = true) -> "WRONG_PASSWORD"
                    msg.contains("ERROR_INVALID_EMAIL", ignoreCase = true) ||
                    msg.contains("invalid-email", ignoreCase = true) -> "INVALID_EMAIL"
                    msg.contains("ERROR_TOO_MANY_REQUESTS", ignoreCase = true) ||
                    msg.contains("too many requests", ignoreCase = true) ||
                    msg.contains("blocked", ignoreCase = true) ||
                    msg.contains("unusual activity", ignoreCase = true) -> "TOO_MANY_REQUESTS"
                    msg.contains("ERROR_NETWORK_REQUEST_FAILED", ignoreCase = true) ||
                    msg.contains("network", ignoreCase = true) -> "NETWORK_ERROR"
                    msg.contains("ERROR_INVALID_CREDENTIAL", ignoreCase = true) -> "INVALID_CREDENTIAL"
                    else -> null
                }
            }
            
            if (errorCode != null) {
                Log.e("AuthRepository", "   Detected error code: $errorCode")
            }
            
            // Create user-friendly error message
            val errorMessage = when (errorCode) {
                "TOO_MANY_REQUESTS" -> "Too many failed attempts. Please try again later."
                "USER_NOT_FOUND" -> "Account not found. Please create an account."
                "WRONG_PASSWORD" -> "Incorrect password. Please try again."
                "INVALID_EMAIL" -> "Invalid email format. Please check your email."
                "NETWORK_ERROR" -> "Network error. Please check your connection."
                "INVALID_CREDENTIAL" -> "Invalid credentials. Please check your email and password."
                else -> e.message ?: "Authentication failed. Please try again."
            }
            
            Log.e("AuthRepository", "   Stack trace: ${e.stackTraceToString().take(500)}")
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Firebase User is null")
            
            // Update local preferences
            updateLocalUser(user)
            
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Email Sign-Up failed", e)
            Result.failure(e)
        }
    }

    // ============================================
    // GOOGLE AUTHENTICATION
    // ============================================

    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun signInWithGoogle(intent: Intent): Result<FirebaseUser> {
        return try {
            Log.d("AuthRepository", "Starting Google Sign-In authentication...")

            // Get the task from the intent
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            Log.d("AuthRepository", "Google Sign-In task created, checking if successful...")

            // Check if task was successful and get the account
            val account = if (task.isSuccessful) {
                Log.d("AuthRepository", "Google Sign-In task successful")
                task.result
            } else {
                Log.w("AuthRepository", "Google Sign-In task failed, getting result...")
                task.getResult(ApiException::class.java)
            }

            // Null check for account
            if (account == null) {
                Log.e("AuthRepository", "Google Sign-In account is null")
                throw Exception("Google Sign-In returned null account")
            }

            Log.d("AuthRepository", "Google account retrieved: ${account.email}")

            // Null check for ID token
            val idToken = account.idToken ?: run {
                Log.e("AuthRepository", "Google account ID token is null - check Web Client ID configuration")
                throw Exception("Google ID Token is null - Check Web Client ID configuration")
            }

            Log.d("AuthRepository", "ID token retrieved successfully")

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            Log.d("AuthRepository", "Firebase credential created, signing in...")

            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Firebase User is null")

            Log.d("AuthRepository", "Firebase authentication successful for user: ${user.uid}")

            // Update local preferences
            updateLocalUser(user)

            Result.success(user)
        } catch (e: ApiException) {
            // Handle ApiException specifically to extract status code
            val statusCode = e.statusCode
            val errorMessage = when (statusCode) {
                10 -> "Please reinstall the app or contact support"
                8 -> "Google Sign-In service error. Please try again."
                7 -> "Network error. Please check your internet connection."
                12500 -> "Sign-in cancelled"
                12501 -> "Another sign-in is already in progress"
                else -> "Google Sign-In failed with error code: $statusCode"
            }

            Log.e("AuthRepository", "Google Sign-In ApiException")
            Log.e("AuthRepository", "   Status Code: $statusCode")
            Log.e("AuthRepository", "   Error Message: ${e.message}")
            Log.e("AuthRepository", "   Full Exception:", e)

            Result.failure(Exception("$errorMessage (Code: $statusCode)"))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google Sign-In failed: ${e.javaClass.simpleName}", e)
            Log.e("AuthRepository", "   Error message: ${e.message}")
            Log.e("AuthRepository", "   Stack trace: ${e.stackTraceToString()}")
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
        userPrefs.logout()
    }
    
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
    
    private fun updateLocalUser(user: FirebaseUser) {
        try {
            Log.d("AuthRepository", "Updating local user data for: ${user.uid}")
            Log.d("AuthRepository", "User display name: ${user.displayName}")
            Log.d("AuthRepository", "User email: ${user.email}")
            Log.d("AuthRepository", "User provider data count: ${user.providerData.size}")

            val email = user.email ?: run {
                Log.w("AuthRepository", "User email is null, using placeholder")
                "user@rescuemate.local"
            }
            
            // Save credentials with null safety
            val authToken = when {
                user.providerData.any { it.providerId == "google.com" } -> "GOOGLE_AUTH_TOKEN"
                user.providerData.any { it.providerId == "phone" } -> "PHONE_AUTH_TOKEN"
                user.providerData.any { it.providerId == "apple.com" } -> "APPLE_AUTH_TOKEN"
                else -> "AUTH_TOKEN_PLACEHOLDER"
            }
            
            userPrefs.saveUserCredentials(email, authToken)
            Log.d("AuthRepository", "Credentials saved")
            
            // Save profile info with null safety and defaults
            var displayName = user.displayName
            if (displayName.isNullOrBlank()) {
                // Try to get name from email if available
                displayName = if (email.contains("@")) {
                    email.substringBefore("@").replace(".", " ").capitalize()
                } else {
                    "RescueMate User"
                }
            }
            
            // Double check to ensure we never pass a blank string if UserPreferences requires it
            if (displayName.isBlank()) displayName = "User"
            
            val age = userPrefs.getUserAge() ?: ""
            val gender = userPrefs.getUserGender() ?: ""
            val phone = user.phoneNumber?.takeIf { it.isNotBlank() } 
                ?: userPrefs.getUserPhone() 
                ?: ""
            
            // Safe save - catching specific exceptions from UserPreferences
            try {
                userPrefs.saveUserProfile(
                    name = displayName!!, // guaranteed not null/blank by above check
                    age = age,
                    gender = gender,
                    phone = phone
                )
                Log.d("AuthRepository", "Profile info updated")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error saving user profile details: ${e.message}")
            }
            
            userPrefs.setUserId(user.uid)
            Log.d("AuthRepository", "User ID set: ${user.uid}")
            
            // Load profile photo from Firestore
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val firestoreRepo = com.rescuemate.data.repository.FirestoreRepository()
                    val userProfile = firestoreRepo.getUserProfile()
                    userProfile?.get("photoUrl")?.let { photoUrl ->
                        if (photoUrl is String && photoUrl.isNotBlank()) {
                            userPrefs.saveProfilePhotoUrl(photoUrl)
                            Log.d("AuthRepository", "Profile photo URL loaded: $photoUrl")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Failed to load profile photo from Firestore: ${e.message}")
                    // Don't fail login if photo fetch fails
                }
            }
            
            // Register FCM token after successful login with retry logic
            CoroutineScope(Dispatchers.IO).launch {
                var retryCount = 0
                val maxRetries = 3
                var success = false
                
                while (retryCount < maxRetries && !success) {
                    try {
                        success = fcmRepository.registerToken()
                        if (!success && retryCount < maxRetries - 1) {
                            retryCount++
                            Log.d("AuthRepository", "FCM token registration failed, retrying ($retryCount/$maxRetries)...")
                            delay(2000L * retryCount.toLong()) // Exponential backoff: 2s, 4s, 6s
                        }
                    } catch (e: Exception) {
                        retryCount++
                        Log.e("AuthRepository", "FCM token registration error (attempt $retryCount/$maxRetries)", e)
                        if (retryCount < maxRetries) {
                            delay(2000L * retryCount.toLong())
                        }
                    }
                }
                
                if (!success) {
                    Log.w("AuthRepository", "FCM token registration failed after $maxRetries attempts")
                } else {
                    Log.d("AuthRepository", "FCM token registered successfully")
                }
            }
            
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to update local user data", e)
            // Don't throw - authentication succeeded, local data update is secondary
            Log.w("AuthRepository", "Continuing despite local data update failure")
        }
    }

    fun sendVerificationCode(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun verifyCodeAndLink(verificationId: String, code: String): Result<Unit> {
        return try {
            if (verificationId.isBlank()) {
                throw Exception("Verification ID is empty")
            }
            if (code.isBlank()) {
                throw Exception("Verification code is empty")
            }
            
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val user = auth.currentUser
            
            if (user != null) {
                // User is logged in, update phone number
                user.updatePhoneNumber(credential).await()
                userPrefs.saveUserProfile(
                    name = userPrefs.getUserName() ?: "User",
                    age = userPrefs.getUserAge() ?: "",
                    gender = userPrefs.getUserGender() ?: "",
                    phone = user.phoneNumber ?: ""
                )
            } else {
                // No user logged in - sign in with phone
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user ?: throw Exception("Sign in failed")
                updateLocalUser(firebaseUser)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Phone verification failed: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun verifyWithCredential(credential: PhoneAuthCredential): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                // User is logged in, update their phone number
                user.updatePhoneNumber(credential).await()
                userPrefs.saveUserProfile(
                    name = userPrefs.getUserName() ?: "User",
                    age = userPrefs.getUserAge() ?: "",
                    gender = userPrefs.getUserGender() ?: "",
                    phone = user.phoneNumber ?: ""
                )
            } else {
                // User not logged in yet - sign in with phone credential
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user ?: throw Exception("Sign in failed")
                updateLocalUser(firebaseUser)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Phone verification with credential failed", e)
            Result.failure(e)
        }
    }

    suspend fun verifyCodeAndSignIn(verificationId: String, code: String): Result<Unit> {
        return try {
            if (verificationId.isBlank()) {
                throw Exception("Verification ID is empty")
            }
            if (code.isBlank()) {
                throw Exception("Verification code is empty")
            }
            
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Sign in failed")
            
            updateLocalUser(firebaseUser)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Phone sign-in failed: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Helper extension for capitalization
    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
