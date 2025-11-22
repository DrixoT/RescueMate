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
        Log.d("AuthRepository", "Using fallback Web Client ID")
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
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Firebase User is null")
            
            // Update local preferences
            updateLocalUser(user)
            
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Email Sign-In failed", e)
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
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken ?: throw Exception("Google ID Token is null - Check Web Client ID configuration")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Firebase User is null")
            
            // Update local preferences
            updateLocalUser(user)
            
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google Sign-In failed", e)
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
        val email = user.email ?: ""
        // We don't have the password hash for Google Auth, using a placeholder or handling it in UserPreferences logic
        // For now, we just ensure the session is marked active
        userPrefs.saveUserCredentials(email, "GOOGLE_AUTH_TOKEN_PLACEHOLDER")
        
        // Save profile info if available
        userPrefs.saveUserProfile(
            name = user.displayName ?: "",
            age = userPrefs.getUserAge() ?: "", // Keep existing if present
            gender = userPrefs.getUserGender() ?: "", // Keep existing
            phone = user.phoneNumber ?: userPrefs.getUserPhone() ?: ""
        )
        
        userPrefs.setUserId(user.uid)
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

