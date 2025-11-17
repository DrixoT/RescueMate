package com.rescuemate.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Unit tests for EncryptionService
 * NOTE: These are instrumented tests that require Android Keystore
 */
@RunWith(AndroidJUnit4::class)
class EncryptionServiceTest {
    
    private lateinit var encryptionService: EncryptionService
    
    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        encryptionService = EncryptionService(context)
    }
    
    @Test
    fun testEncryptDecrypt() {
        val originalData = "Sensitive medical data"
        
        val encryptResult = encryptionService.encrypt(originalData)
        assertTrue(encryptResult.isSuccess)
        
        val encryptedData = encryptResult.getOrNull()
        assertNotNull(encryptedData)
        assertNotEquals(originalData, encryptedData)
        
        val decryptResult = encryptionService.decrypt(encryptedData!!)
        assertTrue(decryptResult.isSuccess)
        assertEquals(originalData, decryptResult.getOrNull())
    }
    
    @Test
    fun testEncryptEmptyString() {
        val result = encryptionService.encrypt("")
        assertTrue(result.isFailure)
    }
    
    @Test
    fun testDecryptInvalidData() {
        val result = encryptionService.decrypt("invalid-encrypted-data")
        assertTrue(result.isFailure)
    }
    
    @Test
    fun testKeyAvailability() {
        assertTrue(encryptionService.isKeyAvailable())
    }
    
    @Test
    fun testEncryptMedicalData() {
        val medicalData = "Blood Type: O+, Allergies: Penicillin"
        val encrypted = encryptionService.encryptMedicalData(medicalData)
        assertNotNull(encrypted)
        assertNotEquals(medicalData, encrypted)
        
        val decrypted = encryptionService.decryptMedicalData(encrypted!!)
        assertEquals(medicalData, decrypted)
    }
}

