package com.rescuemate.utils

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {
    private const val ALGORITHM = "AES"
    private const val SECRET_KEY = "RescueMateSecureKey2024" // Hardcoded key for simplicity in this scope

    private val secretKeySpec: SecretKeySpec by lazy {
        val key = SECRET_KEY.toByteArray(Charsets.UTF_8)
        val sha = MessageDigest.getInstance("SHA-256")
        val keyBytes = sha.digest(key).copyOf(16) // Use first 128 bits
        SecretKeySpec(keyBytes, ALGORITHM)
    }

    fun encrypt(data: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            data // Fallback to original data if encryption fails (or handle differently)
        }
    }

    fun decrypt(encryptedData: String): String? {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
            val decodedBytes = Base64.decode(encryptedData, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
