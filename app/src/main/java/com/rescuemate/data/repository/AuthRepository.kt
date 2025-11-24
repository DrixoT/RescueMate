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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.rescuemate.data.UserPreferences
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.InputStream

import android.app.Activity
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class AuthRepository(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val userPrefs = UserPreferences(context)
    private val googleSignInClient: GoogleSignInClient

    init {
        // Try to get web client ID from google-services.json
        val webClientId = getWebClientIdFromGoogleServices()
        
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        
        // Only request ID token if we have a web client ID
        webClientId?.let {
            gsoBuilder.requestIdToken(it)
        }
        
        googleSignInClient = GoogleSignIn.getClient(context, gsoBuilder.build())
    }
    
    private fun getWebClientIdFromGoogleServices(): String? {
        // Try multiple methods to read the Web Client ID
        
        // Method 1: Try reading from assets (if file was manually added to assets)
        try {
            val inputStream: InputStream = context.assets.open("google-services.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            
            val clientArray = json.getJSONArray("client")
            if (clientArray.length() > 0) {
                val client = clientArray.getJSONObject(0)
                val oauthClients = client.optJSONArray("oauth_client")
                
                if (oauthClients != null && oauthClients.length() > 0) {
                    // Find web client (client_type: 3)
                    for (i in 0 until oauthClients.length()) {
                        val oauthClient = oauthClients.getJSONObject(i)
                        if (oauthClient.optInt("client_type") == 3) {
                            val clientId = oauthClient.getString("client_id")
                            Log.d("AuthRepository", "Found Web Client ID from assets: ${clientId.take(20)}...")
                            return clientId
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("AuthRepository", "Could not read from assets, trying file system...", e)
        }
        
        // Method 2: Try reading from the app directory (google-services.json location)
        try {
            val file = java.io.File(context.filesDir.parent, "google-services.json")
            if (!file.exists()) {
                // Try alternative path
                val altFile = java.io.File(context.applicationInfo.dataDir, "google-services.json")
                if (altFile.exists()) {
                    val jsonString = altFile.readText()
                    val json = JSONObject(jsonString)
                    return extractWebClientId(json)
                }
            } else {
                val jsonString = file.readText()
                val json = JSONObject(jsonString)
                return extractWebClientId(json)
            }
        } catch (e: Exception) {
            Log.d("AuthRepository", "Could not read from file system", e)
        }
        
        // Method 3: Fallback to hardcoded value from the actual google-services.json
        // This is the Web Client ID from the project's google-services.json file
        val fallbackClientId = "1085665199694-urhu4004f6lq8bb1hgha5vbqh06ubssp.apps.googleusercontent.com"
        Log.w("AuthRepository", "⚠️ Using fallback Web Client ID")
        Log.w("AuthRepository", "   This may cause Google Sign-In to fail if the fallback doesn't match your Firebase project")
        Log.w("AuthRepository", "   Fallback ID: ${fallbackClientId.take(30)}...")
        Log.w("AuthRepository", "   To fix: Ensure google-services.json is in app/ directory with correct Web Client ID")
        Log.w("AuthRepository", "   Expected location: app/google-services.json")
        return fallbackClientId
    }
    
    private fun extractWebClientId(json: JSONObject): String? {
        try {
            val clientArray = json.getJSONArray("client")
            if (clientArray.length() > 0) {
                val client = clientArray.getJSONObject(0)
                val oauthClients = client.optJSONArray("oauth_client")
                
                if (oauthClients != null && oauthClients.length() > 0) {
                    for (i in 0 until oauthClients.length()) {
                        val oauthClient = oauthClients.getJSONObject(i)
                        if (oauthClient.optInt("client_type") == 3) {
                            val clientId = oauthClient.getString("client_id")
                            Log.d("AuthRepository", "Extracted Web Client ID: ${clientId.take(20)}...")
                            return clientId
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Error extracting Web Client ID", e)
        }
        return null
    }

    // ============================================
    // EMAIL AUTHENTICATION
    // ============================================
    
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            Log.d("AuthRepository", "🔄 Starting email sign-in for: ${email.take(10)}...")
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Firebase User is null")
            
            Log.d("AuthRepository", "✅ Email sign-in successful for user: ${user.uid}")
            
            // Update local preferences
            updateLocalUser(user)
            
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Email Sign-In failed: ${e.javaClass.simpleName}", e)
            Log.e("AuthRepository", "   Error message: ${e.message}")
            
            // Extract Firebase error code if available in message
            val errorCode = e.message?.let { msg ->
                when {
                    msg.contains("ERROR_USER_NOT_FOUND", ignoreCase = true) -> "USER_NOT_FOUND"
                    msg.contains("ERROR_WRONG_PASSWORD", ignoreCase = true) -> "WRONG_PASSWORD"
                    msg.contains("ERROR_INVALID_EMAIL", ignoreCase = true) -> "INVALID_EMAIL"
                    msg.contains("ERROR_TOO_MANY_REQUESTS", ignoreCase = true) -> "TOO_MANY_REQUESTS"
                    msg.contains("ERROR_NETWORK_REQUEST_FAILED", ignoreCase = true) -> "NETWORK_ERROR"
                    msg.contains("ERROR_INVALID_CREDENTIAL", ignoreCase = true) -> "INVALID_CREDENTIAL"
                    else -> null
                }
            }
            
            if (errorCode != null) {
                Log.e("AuthRepository", "   Firebase error code: $errorCode")
            }
            
            Log.e("AuthRepository", "   Stack trace: ${e.stackTraceToString().take(500)}")
            Result.failure(e)
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
        // Check if we have a valid configuration
        if (getWebClientIdFromGoogleServices() == null) {
            Log.e("AuthRepository", "⚠️ MISSING WEB CLIENT ID in google-services.json! Google Sign-In will fail.")
        }
        return googleSignInClient.signInIntent
    }

    suspend fun signInWithGoogle(intent: Intent): Result<FirebaseUser> {
        // First check if we have the necessary configuration
        if (getWebClientIdFromGoogleServices() == null) {
            val errorMsg = "Configuration Error: Missing Web Client ID in google-services.json. Please update your Firebase configuration."
            Log.e("AuthRepository", errorMsg)
            return Result.failure(Exception(errorMsg))
        }

        return try {
            Log.d("AuthRepository", "🔄 Starting Google Sign-In authentication...")
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val account = task.getResult(ApiException::class.java)
            
            val idToken = account.idToken ?: throw Exception("Google ID Token is null - Check Web Client ID configuration")
            Log.d("AuthRepository", "✅ Google account retrieved, ID token present")
            
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Firebase User is null")
            
            Log.d("AuthRepository", "✅ Firebase authentication successful for user: ${user.uid}")
            
            // Update local preferences
            updateLocalUser(user)
            
            Result.success(user)
        } catch (e: ApiException) {
            // Handle ApiException specifically to extract status code
            val statusCode = e.statusCode
            val errorMessage = when (statusCode) {
                10 -> "Configuration error: Wrong Web Client ID or SHA-1 certificate fingerprint. Please verify your Firebase configuration."
                8 -> "Google Sign-In service error. Please try again."
                7 -> "Network error. Please check your internet connection."
                12500 -> "Sign-in was cancelled"
                12501 -> "Another sign-in is already in progress"
                else -> "Google Sign-In failed with error code: $statusCode"
            }
            
            Log.e("AuthRepository", "❌ Google Sign-In ApiException")
            Log.e("AuthRepository", "   Status Code: $statusCode")
            Log.e("AuthRepository", "   Error Message: ${e.message}")
            Log.e("AuthRepository", "   Full Exception:", e)
            
            Result.failure(Exception("$errorMessage (Code: $statusCode)"))
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Google Sign-In failed: ${e.javaClass.simpleName}", e)
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
            Log.d("AuthRepository", "📝 Updating local user data for: ${user.uid}")
            
            val email = user.email ?: run {
                Log.w("AuthRepository", "⚠️ User email is null, using placeholder")
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
            Log.d("AuthRepository", "✅ Credentials saved")
            
            // Save profile info with null safety and defaults
            val displayName = user.displayName?.takeIf { it.isNotBlank() } 
                ?: userPrefs.getUserName()
                ?: "User"
            
            val age = userPrefs.getUserAge() ?: ""
            val gender = userPrefs.getUserGender() ?: ""
            val phone = user.phoneNumber?.takeIf { it.isNotBlank() } 
                ?: userPrefs.getUserPhone() 
                ?: ""
            
            userPrefs.saveUserProfile(
                name = displayName,
                age = age,
                gender = gender,
                phone = phone
            )
            Log.d("AuthRepository", "✅ Profile info updated")
            
            userPrefs.setUserId(user.uid)
            Log.d("AuthRepository", "✅ User ID set: ${user.uid}")
            
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Failed to update local user data", e)
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
                    name = userPrefs.getUserName() ?: "",
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
                    name = userPrefs.getUserName() ?: "",
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
}

