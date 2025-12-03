package com.rescuemate.emergency.detection

import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import com.rescuemate.emergency.EmergencyManager
import com.rescuemate.emergency.health.HealthMonitoringService
import com.rescuemate.emergency.health.MockSensorDataService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Demo Simulation Manager
 * Orchestrates the anomaly simulation demo.
 * 
 * Logic:
 * 1. Starts a background loop.
 * 2. Generates sensor readings every 3 seconds.
 * 3. Randomly injects anomalies (HR spike or Fall) with 20% probability.
 * 4. Passes data to HealthMonitoringService (TinyLlama).
 * 5. If risk > threshold for 3 consecutive readings -> Trigger Emergency.
 */
class DemoSimulationManager(private val context: Context) {

    companion object {
        private const val TAG = "DemoSimulationManager"
        private const val READING_INTERVAL_MS = 3000L
        private const val ANOMALY_CHANCE = 0.2 // 20% chance
        private const val CONFIRMATION_COUNT = 3 // Need 3 consecutive bad readings
    }

    private val mockSensorService = MockSensorDataService()
    private val healthService = HealthMonitoringService(context)
    private val emergencyManager = EmergencyManager(context)
    
    private var simulationJob: Job? = null
    private val isRunning = AtomicBoolean(false)
    
    // State
    private var consecutiveHighRiskCount = 0
    private var lastReadings = mutableListOf<MockSensorDataService.HeartRateReading>()

    fun startDemo() {
        if (isRunning.get()) return
        isRunning.set(true)
        Log.d(TAG, "Starting Demo Simulation...")

        simulationJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && isRunning.get()) {
                try {
                    // 1. Generate Data (Randomly inject anomaly)
                    if (Math.random() < ANOMALY_CHANCE) {
                        val type = if (Math.random() > 0.5) 
                            MockSensorDataService.AnomalyType.CRITICAL_HIGH 
                        else 
                            MockSensorDataService.AnomalyType.SUDDEN_DROP // Mimic fall/shock
                        
                        mockSensorService.forceAnomaly(type)
                        Log.d(TAG, "🔥 DEMO: Forcing anomaly -> $type")
                    }

                    val reading = mockSensorService.generateHeartRate(simulateExercise = false)
                    lastReadings.add(reading)
                    if (lastReadings.size > 10) lastReadings.removeAt(0)

                    // 2. Analyze with TinyLlama
                    // Note: This uses the TinyLlama service we just fixed to be thread-safe
                    Log.d(TAG, "Analyzing reading: ${reading.heartRate} BPM...")
                    
                    val analysis = healthService.analyzeHealthStatus(
                        currentHeartRate = reading.heartRate,
                        activityLevel = com.rescuemate.emergency.data.HealthData.ActivityLevel.STATIONARY,
                        isExercising = false
                    )

                    Log.d(TAG, "Analysis Result: Abnormal=${analysis.isAbnormal}, Risk=${analysis.riskScore}, Reason=${analysis.alertReason}")

                    // 3. Emergency Trigger Logic
                    if (analysis.isAbnormal && analysis.riskScore > 0.75f) {
                        consecutiveHighRiskCount++
                        Log.w(TAG, "⚠️ High risk detected! Count: $consecutiveHighRiskCount/$CONFIRMATION_COUNT")
                    } else {
                        if (consecutiveHighRiskCount > 0) {
                            Log.d(TAG, "Risk subsided. Resetting count.")
                        }
                        consecutiveHighRiskCount = 0
                    }

                    // 4. Trigger Protocol
                    if (consecutiveHighRiskCount >= CONFIRMATION_COUNT) {
                        Log.e(TAG, "🚨 EMERGENCY THRESHOLD REACHED! Triggering protocol...")
                        
                        withContext(Dispatchers.Main) {
                            // Create dummy MedicalInfo
                            val dummyMedicalInfo = com.rescuemate.emergency.data.MedicalInfo(
                                userId = "demo_user",
                                knownConditions = emptyList(),
                                currentMedications = emptyList(),
                                allergies = emptyList()
                            )

                            // Create dummy UserInfo for demo purposes since we don't have direct access to user prefs here
                            val dummyUserInfo = com.rescuemate.emergency.data.UserInfo(
                                userId = "demo_user",
                                name = "Demo User",
                                age = 30,
                                phoneNumber = "0000000000",
                                medicalInfo = dummyMedicalInfo
                            )
                            
                            // Use the public trigger method with dummy user info
                            emergencyManager.triggerManualEmergency(
                                userId = "demo_user",
                                userInfo = dummyUserInfo,
                                emergencyType = com.rescuemate.emergency.EmergencyConstants.EmergencyType.MANUAL_TRIGGER // Using manual trigger for immediate effect in demo
                            )
                        }
                        
                        // Stop simulation after trigger to prevent loop
                        stopDemo()
                        break
                    }

                    delay(READING_INTERVAL_MS)

                } catch (e: Exception) {
                    Log.e(TAG, "Error in demo loop", e)
                    delay(5000) // Backoff on error
                }
            }
        }
    }

    fun stopDemo() {
        isRunning.set(false)
        simulationJob?.cancel()
        mockSensorService.clearForcedAnomaly()
        consecutiveHighRiskCount = 0
        Log.d(TAG, "Demo Simulation Stopped")
    }
    
    fun isRunning(): Boolean = isRunning.get()
}
