package com.rescuemate.emergency

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rescuemate.emergency.data.EmergencyContact
import com.rescuemate.emergency.data.HealthData
import com.rescuemate.emergency.data.LocationData
import com.rescuemate.emergency.data.MedicalInfo
import com.rescuemate.emergency.data.UserInfo
import com.rescuemate.emergency.data.database.EmergencyDatabaseHelper
import com.rescuemate.emergency.health.HealthMonitoringService
import com.rescuemate.emergency.location.EmergencyLocationService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emergency Module Integration Tests
 */
@RunWith(AndroidJUnit4::class)
class EmergencyModuleTest {

    private lateinit var context: Context
    private lateinit var emergencyManager: EmergencyManager
    private lateinit var dbHelper: EmergencyDatabaseHelper
    private lateinit var healthService: HealthMonitoringService
    private lateinit var locationService: EmergencyLocationService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        emergencyManager = EmergencyManager(context)
        dbHelper = EmergencyDatabaseHelper(context)
        healthService = HealthMonitoringService(context)
        locationService = EmergencyLocationService(context)

        // Clear database
        context.deleteDatabase(EmergencyConstants.DATABASE_NAME)
    }

    @Test
    fun testEmergencyContactCreation() {
        val contact = EmergencyContact(
            name = "Test Contact",
            phoneNumber = "+1234567890",
            relationship = "Friend",
            priority = 1,
            isVerified = true
        )

        val result = dbHelper.insertContact(contact)
        assertTrue("Contact insertion should succeed", result > 0)

        val contacts = dbHelper.getAllContacts()
        assertEquals("Should have 1 contact", 1, contacts.size)
        assertEquals("Contact name should match", "Test Contact", contacts[0].name)
    }

    @Test
    fun testEmergencyContactPriority() {
        // Add multiple contacts
        dbHelper.insertContact(EmergencyContact(
            name = "Contact 1",
            phoneNumber = "+1111111111",
            relationship = "Family",
            priority = 1
        ))

        dbHelper.insertContact(EmergencyContact(
            name = "Contact 2",
            phoneNumber = "+2222222222",
            relationship = "Friend",
            priority = 2
        ))

        val contacts = dbHelper.getAllContacts()
        assertEquals("Should have 2 contacts", 2, contacts.size)
        assertEquals("First contact should have priority 1", 1, contacts[0].priority)
        assertTrue("Contacts should be ordered by priority",
            contacts[0].priority < contacts[1].priority)
    }

    @Test
    fun testHealthMonitoringBaseline() {
        healthService.setBaselineHeartRate(70)
        assertEquals("Baseline should be 70", 70, healthService.getBaselineHeartRate())
    }

    @Test
    fun testHealthMonitoringRecording() {
        healthService.recordHeartRate(75)
        healthService.recordHeartRate(80)
        healthService.recordHeartRate(85)

        val stats = healthService.getHeartRateStats()
        assertEquals("Should have 3 samples", 3, stats.sampleSize)
        assertEquals("Min should be 75", 75, stats.min)
        assertEquals("Max should be 85", 85, stats.max)
    }

    @Test
    fun testHealthAnalysisBasicRules() = runBlocking {
        healthService.setBaselineHeartRate(70)

        // Test normal heart rate
        val normalAnalysis = healthService.analyzeHealthStatus(
            currentHeartRate = 75,
            activityLevel = HealthData.ActivityLevel.WALKING,
            isExercising = false,
            llmApiKey = null // Use rule-based only
        )

        assertFalse("Normal heart rate should not be abnormal", normalAnalysis.isAbnormal)
        assertTrue("Risk score should be low", normalAnalysis.riskScore < 0.5f)
    }

    @Test
    fun testHealthAnalysisCriticalHigh() = runBlocking {
        healthService.setBaselineHeartRate(70)

        // Test critical high heart rate
        val criticalAnalysis = healthService.analyzeHealthStatus(
            currentHeartRate = 185,
            activityLevel = HealthData.ActivityLevel.STATIONARY,
            isExercising = false,
            llmApiKey = null
        )

        assertTrue("Critical heart rate should be abnormal", criticalAnalysis.isAbnormal)
        assertTrue("Risk score should be high", criticalAnalysis.riskScore >= 0.9f)
    }

    @Test
    fun testMedicalInfoStorage() {
        val medicalInfo = MedicalInfo(
            userId = "test-user",
            bloodType = "O+",
            knownConditions = listOf("Hypertension"),
            currentMedications = listOf(
                com.rescuemate.emergency.data.Medication(
                    name = "Test Med",
                    dosage = "10mg",
                    frequency = "Daily"
                )
            ),
            baselineHeartRate = 70
        )

        dbHelper.insertOrUpdateMedicalInfo(medicalInfo)

        val retrieved = dbHelper.getMedicalInfo("test-user")
        assertNotNull("Medical info should be retrieved", retrieved)
        assertEquals("Blood type should match", "O+", retrieved?.bloodType)
        assertEquals("Should have 1 condition", 1, retrieved?.knownConditions?.size)
    }

    @Test
    fun testEmergencyManagerNotNull() {
        assertNotNull("Emergency manager should be initialized", emergencyManager)
        assertFalse("No emergency should be active initially", emergencyManager.isEmergencyActive())
    }

    @Test
    fun testEmergencyTriggerWithoutContacts() = runBlocking {
        val userInfo = UserInfo(
            userId = "test-user",
            name = "Test User",
            age = 30,
            phoneNumber = "+1234567890",
            medicalInfo = MedicalInfo(userId = "test-user")
        )

        val result = emergencyManager.triggerManualEmergency("test-user", userInfo)

        assertTrue("Emergency should fail without contacts", result.isFailure)
        assertEquals("Should have no contacts error",
            EmergencyConstants.ERROR_NO_EMERGENCY_CONTACTS,
            result.exceptionOrNull()?.message)
    }

    @Test
    fun testLocationDataCreation() {
        val location = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            address = "San Francisco, CA",
            accuracy = 10f
        )

        assertEquals("Latitude should match", 37.7749, location.latitude, 0.0001)
        assertTrue("Location should be accurate", location.isAccurate())
        assertTrue("Google Maps link should contain coordinates",
            location.getGoogleMapsLink().contains("37.7749"))
    }

    @Test
    fun testEmergencyEventCreation() {
        val healthData = HealthData(
            currentHeartRate = 120,
            normalHeartRate = 70,
            alertReason = "Test alert",
            riskScore = 0.8f
        )

        val locationData = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 10f
        )

        val userInfo = UserInfo(
            userId = "test-user",
            name = "Test User",
            age = 30,
            phoneNumber = "+1234567890",
            medicalInfo = MedicalInfo(userId = "test-user")
        )

        val event = com.rescuemate.emergency.data.EmergencyEvent(
            userId = "test-user",
            emergencyType = EmergencyConstants.EmergencyType.CARDIAC_ALERT,
            status = EmergencyConstants.EmergencyStatus.INITIATED,
            healthData = healthData,
            locationData = locationData,
            userInfo = userInfo,
            emergencyContacts = emptyList()
        )

        assertTrue("Event should be active", event.isActive())
        assertFalse("Should not escalate to phase 2 immediately", event.shouldEscalateToPhase2())
    }

    @Test
    fun testNotificationPreferences() {
        val contact = EmergencyContact(
            name = "Test Contact",
            phoneNumber = "+1234567890",
            relationship = "Friend",
            priority = 1,
            notificationPreference = EmergencyContact.NotificationPreference.VOICE_ONLY
        )

        assertTrue("Should receive voice call", contact.canReceiveVoiceCall())
        assertFalse("Should not receive SMS", contact.canReceiveSMS())
    }
}

