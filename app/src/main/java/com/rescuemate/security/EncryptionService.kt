package com.rescuemate.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encryption Service
 * Handles encryption/decryption of sensitive data using Android Keystore
 */
class EncryptionService(private val context: Context) {
    
    companion object {
        private const val TAG = "EncryptionService"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "RescueMateSecureKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
        load(null)
    }
    
    init {
        // Create key if it doesn't exist
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            createKey()
        }
    }
    
    /**
     * Create encryption key in Android Keystore
     */
    private fun createKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEY_STORE
            )
            
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false) // Can enable for biometric
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
            
            Log.d(TAG, "Encryption key created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create encryption key", e)
            throw SecurityException("Failed to create encryption key", e)
        }
    }
    
    /**
     * Get the secret key from Keystore
     */
    private fun getSecretKey(): SecretKey {
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }
    
    /**
     * Encrypt data
     * @return Base64 encoded encrypted data with IV prepended
     */
    fun encrypt(data: String): Result<String> {
        if (data.isBlank()) {
            return Result.failure(IllegalArgumentException("Data cannot be blank"))
        }
        
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            // Prepend IV to encrypted data
            val combined = iv + encryptedData
            val encoded = Base64.encodeToString(combined, Base64.DEFAULT)
            
            Result.success(encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Decrypt data
     * @param encryptedData Base64 encoded encrypted data with IV prepended
     */
    fun decrypt(encryptedData: String): Result<String> {
        if (encryptedData.isBlank()) {
            return Result.failure(IllegalArgumentException("Encrypted data cannot be blank"))
        }
        
        return try {
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)
            
            // Extract IV (first 12 bytes for GCM)
            val ivSize = 12
            val iv = combined.copyOfRange(0, ivSize)
            val encrypted = combined.copyOfRange(ivSize, combined.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            
            val decrypted = cipher.doFinal(encrypted)
            val result = String(decrypted, Charsets.UTF_8)
            
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Encrypt medical data
     */
    fun encryptMedicalData(medicalData: String): String? {
        return encrypt(medicalData).getOrNull()
    }
    
    /**
     * Decrypt medical data
     */
    fun decryptMedicalData(encryptedData: String): String? {
        return decrypt(encryptedData).getOrNull()
    }
    
    /**
     * Check if key exists
     */
    fun isKeyAvailable(): Boolean {
        return try {
            keyStore.containsAlias(KEY_ALIAS)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking key availability", e)
            false
        }
    }
    
    /**
     * Delete encryption key (use with caution - will lose access to encrypted data)
     */
    fun deleteKey() {
        try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
                Log.d(TAG, "Encryption key deleted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete encryption key", e)
        }
    }
}

