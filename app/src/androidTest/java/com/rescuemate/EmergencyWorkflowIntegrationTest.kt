package com.rescuemate

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rescuemate.emergency.EmergencyManager
import com.rescuemate.emergency.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Integration test for Emergency Workflow
 * Tests end-to-end emergency trigger flow
 */
@RunWith(AndroidJUnit4::class)
class EmergencyWorkflowIntegrationTest {
    
    private lateinit var emergencyManager: EmergencyManager
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        emergencyManager = EmergencyManager(context)
    }
    
    @Test
    fun testEmergencyTriggerFlow() = runBlocking {
        // Setup test data
        val userId = "test-user-id"
        val testContact = EmergencyContact(
            name = "Test Emergency Contact",
            phoneNumber = "+1234567890",
            relationship = "Friend",
            priority = 1
        )
        
        // Insert test contact
        emergencyManager.database.insertContact(testContact)
        
        // Create user info
        val userInfo = UserInfo(
            userId = userId,
            name = "Test User",
            age = 30,
            phoneNumber = "+1111111111",
            medicalInfo = MedicalInfo(userId = userId)
        )
        
        // Create health data (emergency trigger)
        val healthData = HealthData(
            currentHeartRate = 180,
            normalHeartRate = 70,
            alertReason = "Heart rate critically high",
            riskScore = 0.9f
        )
        
        // Trigger emergency
        val result = emergencyManager.triggerHealthEmergency(userId, healthData, userInfo)
        
        // Verify emergency was triggered
        assertTrue(result.isSuccess)
        val emergencyEvent = result.getOrNull()
        assertNotNull(emergencyEvent)
        assertEquals(userId, emergencyEvent?.userId)
        assertTrue(emergencyManager.isEmergencyActive())
        
        // Test cancellation
        emergencyManager.userConfirmSafe()
        assertFalse(emergencyManager.isEmergencyActive())
    }
    
    @Test
    fun testManualEmergencyTrigger() = runBlocking {
        val userId = "test-user-2"
        
        // Add contact
        emergencyManager.database.insertContact(
            EmergencyContact(
                name = "Contact 2",
                phoneNumber = "+1987654321",
                relationship = "Family",
                priority = 1
            )
        )
        
        val userInfo = UserInfo(
            userId = userId,
            name = "Test User 2",
            age = 25,
            phoneNumber = "+2222222222",
            medicalInfo = MedicalInfo(userId = userId)
        )
        
        val result = emergencyManager.triggerManualEmergency(userId, userInfo)
        
        assertTrue(result.isSuccess)
        assertTrue(emergencyManager.isEmergencyActive())
    }
}

