package com.rescuemate.emergency.health

import android.util.Log
import com.rescuemate.emergency.EmergencyConstants
import kotlin.random.Random

/**
 * Mock Sensor Data Service
 * Simulates heart rate and sensor data for testing when actual smartwatch is unavailable
 */
class MockSensorDataService {

    companion object {
        private const val TAG = "MockSensorDataService"

        // Heart rate simulation parameters
        private const val DEFAULT_BASELINE_HR = 70
        private const val NORMAL_VARIATION = 5 // ±5 BPM
        private const val EXERCISE_ELEVATION = 40 // +40 BPM during exercise
        private const val STRESS_ELEVATION = 20 // +20 BPM during stress

        // Anomaly simulation
        private const val ANOMALY_PROBABILITY = 0.05f // 5% chance of anomaly per reading
        private const val CRITICAL_ANOMALY_PROBABILITY = 0.01f // 1% chance of critical anomaly

        // Pattern simulation
        private const val PATTERN_DURATION_READINGS = 10 // Readings in a pattern
    }

    data class HeartRateReading(
        val heartRate: Int,
        val timestamp: Long = System.currentTimeMillis(),
        val activityLevel: ActivityLevel = ActivityLevel.UNKNOWN,
        val isExercising: Boolean = false,
        val isAnomaly: Boolean = false,
        val anomalyType: AnomalyType? = null
    )

    enum class ActivityLevel {
        STATIONARY,
        WALKING,
        RUNNING,
        EXERCISING,
        UNKNOWN
    }

    enum class AnomalyType {
        HIGH_HEART_RATE,      // Tachycardia
        LOW_HEART_RATE,        // Bradycardia
        IRREGULAR_RHYTHM,      // Arrhythmia pattern
        SUDDEN_DROP,           // Sudden drop in HR
        CRITICAL_HIGH,         // Very high HR (>180)
        CRITICAL_LOW           // Very low HR (<40)
    }

    private var baselineHeartRate: Int = DEFAULT_BASELINE_HR
    private var currentPattern: Pattern? = null
    private var patternRemainingReadings: Int = 0
    private var lastHeartRate: Int = DEFAULT_BASELINE_HR
    private var consecutiveAnomalies: Int = 0

    /**
     * Set baseline heart rate for simulation
     */
    fun setBaselineHeartRate(bpm: Int) {
        baselineHeartRate = bpm.coerceIn(40, 120)
        lastHeartRate = baselineHeartRate
        Log.d(TAG, "Baseline heart rate set to: $baselineHeartRate BPM")
    }

    /**
     * Generate a realistic heart rate reading
     * @param variationLevel How much variation to introduce (0.0 = normal, 1.0 = maximum variation)
     * @param anomalyProbability Probability of generating an anomaly (0.0 to 1.0)
     * @param simulateExercise Whether to simulate exercise scenario
     * @return HeartRateReading with simulated data
     */
    fun generateHeartRate(
        variationLevel: Float = 0.5f,
        anomalyProbability: Float = ANOMALY_PROBABILITY,
        simulateExercise: Boolean = false
    ): HeartRateReading {
        val currentTime = System.currentTimeMillis()

        // Check if we're in a pattern
        if (patternRemainingReadings > 0 && currentPattern != null) {
            patternRemainingReadings--
            return generatePatternReading(currentPattern!!)
        }

        // Determine activity level
        val activityLevel = if (simulateExercise) {
            ActivityLevel.EXERCISING
        } else {
            ActivityLevel.values().random()
        }

        val isExercising = activityLevel == ActivityLevel.EXERCISING ||
                          activityLevel == ActivityLevel.RUNNING

        // Base heart rate calculation
        var heartRate = baselineHeartRate

        // Add activity-based elevation
        when (activityLevel) {
            ActivityLevel.EXERCISING, ActivityLevel.RUNNING -> {
                heartRate += EXERCISE_ELEVATION + Random.nextInt(-10, 10)
            }
            ActivityLevel.WALKING -> {
                heartRate += 10 + Random.nextInt(-5, 5)
            }
            ActivityLevel.STATIONARY -> {
                // Slight decrease when stationary
                heartRate += Random.nextInt(-3, 3)
            }
            else -> {
                // Normal variation
                heartRate += Random.nextInt(-NORMAL_VARIATION, NORMAL_VARIATION + 1)
            }
        }

        // Check for anomaly
        val shouldGenerateAnomaly = Random.nextFloat() < anomalyProbability
        val anomalyType: AnomalyType? = if (shouldGenerateAnomaly) {
            generateAnomaly(heartRate)
        } else {
            null
        }

        // Apply anomaly if present
        if (anomalyType != null) {
            heartRate = applyAnomaly(heartRate, anomalyType)
            consecutiveAnomalies++
            Log.d(TAG, "⚠️ Anomaly detected: $anomalyType, HR: $heartRate BPM")
        } else {
            consecutiveAnomalies = 0
        }

        // Clamp heart rate to reasonable bounds
        heartRate = heartRate.coerceIn(30, 220)

        // Update last heart rate
        lastHeartRate = heartRate

        return HeartRateReading(
            heartRate = heartRate,
            timestamp = currentTime,
            activityLevel = activityLevel,
            isExercising = isExercising,
            isAnomaly = anomalyType != null,
            anomalyType = anomalyType
        )
    }

    /**
     * Generate an anomaly pattern for testing emergency scenarios
     */
    fun simulateAnomalyPattern(patternType: AnomalyPattern): List<HeartRateReading> {
        val readings = mutableListOf<HeartRateReading>()
        val patternDuration = PATTERN_DURATION_READINGS

        when (patternType) {
            AnomalyPattern.TACHYCARDIA -> {
                // Gradually increasing heart rate
                for (i in 0 until patternDuration) {
                    val hr = baselineHeartRate + 50 + (i * 5) + Random.nextInt(-5, 5)
                    readings.add(HeartRateReading(
                        heartRate = hr.coerceIn(120, 200),
                        activityLevel = ActivityLevel.UNKNOWN,
                        isAnomaly = true,
                        anomalyType = AnomalyType.HIGH_HEART_RATE
                    ))
                }
            }
            AnomalyPattern.BRADYCARDIA -> {
                // Gradually decreasing heart rate
                for (i in 0 until patternDuration) {
                    val hr = baselineHeartRate - 30 - (i * 3) + Random.nextInt(-3, 3)
                    readings.add(HeartRateReading(
                        heartRate = hr.coerceIn(30, 60),
                        activityLevel = ActivityLevel.STATIONARY,
                        isAnomaly = true,
                        anomalyType = AnomalyType.LOW_HEART_RATE
                    ))
                }
            }
            AnomalyPattern.CRITICAL_HIGH -> {
                // Critical high heart rate pattern
                for (i in 0 until patternDuration) {
                    val hr = 180 + Random.nextInt(-10, 10)
                    readings.add(HeartRateReading(
                        heartRate = hr.coerceIn(170, 220),
                        activityLevel = ActivityLevel.UNKNOWN,
                        isAnomaly = true,
                        anomalyType = AnomalyType.CRITICAL_HIGH
                    ))
                }
            }
            AnomalyPattern.CRITICAL_LOW -> {
                // Critical low heart rate pattern
                for (i in 0 until patternDuration) {
                    val hr = 35 + Random.nextInt(-5, 5)
                    readings.add(HeartRateReading(
                        heartRate = hr.coerceIn(25, 45),
                        activityLevel = ActivityLevel.STATIONARY,
                        isAnomaly = true,
                        anomalyType = AnomalyType.CRITICAL_LOW
                    ))
                }
            }
            AnomalyPattern.IRREGULAR -> {
                // Irregular rhythm pattern
                for (i in 0 until patternDuration) {
                    val baseHR = baselineHeartRate + Random.nextInt(-20, 40)
                    val hr = baseHR + Random.nextInt(-15, 15)
                    readings.add(HeartRateReading(
                        heartRate = hr.coerceIn(50, 150),
                        activityLevel = ActivityLevel.UNKNOWN,
                        isAnomaly = true,
                        anomalyType = AnomalyType.IRREGULAR_RHYTHM
                    ))
                }
            }
            AnomalyPattern.SUDDEN_DROP -> {
                // Sudden drop pattern
                readings.addAll((0 until 3).map {
                    HeartRateReading(
                        heartRate = baselineHeartRate + Random.nextInt(-5, 5),
                        activityLevel = ActivityLevel.UNKNOWN
                    )
                })
                readings.addAll((0 until 5).map {
                    HeartRateReading(
                        heartRate = 40 + Random.nextInt(-5, 5),
                        activityLevel = ActivityLevel.STATIONARY,
                        isAnomaly = true,
                        anomalyType = AnomalyType.SUDDEN_DROP
                    )
                })
            }
        }

        return readings
    }

    /**
     * Generate a specific heart rate value for testing
     */
    fun generateSpecificHeartRate(bpm: Int, isAnomaly: Boolean = false): HeartRateReading {
        val anomalyType = if (isAnomaly) {
            when {
                bpm >= EmergencyConstants.HEART_RATE_CRITICAL_HIGH -> AnomalyType.CRITICAL_HIGH
                bpm <= EmergencyConstants.HEART_RATE_LOW_THRESHOLD -> AnomalyType.CRITICAL_LOW
                bpm > baselineHeartRate + 40 -> AnomalyType.HIGH_HEART_RATE
                bpm < baselineHeartRate - 20 -> AnomalyType.LOW_HEART_RATE
                else -> AnomalyType.IRREGULAR_RHYTHM
            }
        } else null

        return HeartRateReading(
            heartRate = bpm,
            activityLevel = ActivityLevel.UNKNOWN,
            isAnomaly = isAnomaly,
            anomalyType = anomalyType
        )
    }

    /**
     * Generate an anomaly type based on current heart rate
     */
    private fun generateAnomaly(currentHR: Int): AnomalyType {
        val criticalHigh = Random.nextFloat() < CRITICAL_ANOMALY_PROBABILITY
        val criticalLow = Random.nextFloat() < CRITICAL_ANOMALY_PROBABILITY

        return when {
            criticalHigh || currentHR > 180 -> AnomalyType.CRITICAL_HIGH
            criticalLow || currentHR < 40 -> AnomalyType.CRITICAL_LOW
            currentHR > baselineHeartRate + 30 -> AnomalyType.HIGH_HEART_RATE
            currentHR < baselineHeartRate - 20 -> AnomalyType.LOW_HEART_RATE
            Random.nextBoolean() -> AnomalyType.IRREGULAR_RHYTHM
            else -> AnomalyType.SUDDEN_DROP
        }
    }

    /**
     * Apply anomaly to heart rate
     */
    private fun applyAnomaly(heartRate: Int, anomalyType: AnomalyType): Int {
        return when (anomalyType) {
            AnomalyType.CRITICAL_HIGH -> 180 + Random.nextInt(0, 40)
            AnomalyType.CRITICAL_LOW -> 30 + Random.nextInt(0, 10)
            AnomalyType.HIGH_HEART_RATE -> heartRate + 40 + Random.nextInt(0, 20)
            AnomalyType.LOW_HEART_RATE -> heartRate - 30 - Random.nextInt(0, 15)
            AnomalyType.IRREGULAR_RHYTHM -> heartRate + Random.nextInt(-25, 30)
            AnomalyType.SUDDEN_DROP -> (heartRate * 0.5).toInt() + Random.nextInt(-5, 5)
        }.coerceIn(25, 220)
    }

    /**
     * Generate reading as part of a pattern
     */
    private fun generatePatternReading(pattern: Pattern): HeartRateReading {
        // Pattern-specific logic would go here
        return generateHeartRate()
    }

    /**
     * Reset simulation state
     */
    fun reset() {
        currentPattern = null
        patternRemainingReadings = 0
        lastHeartRate = baselineHeartRate
        consecutiveAnomalies = 0
        Log.d(TAG, "Mock sensor data service reset")
    }

    /**
     * Get current baseline
     */
    fun getBaselineHeartRate(): Int = baselineHeartRate

    /**
     * Get statistics about generated readings
     */
    fun getStatistics(readings: List<HeartRateReading>): SensorStatistics {
        if (readings.isEmpty()) {
            return SensorStatistics(0, 0, 0, 0.0, 0, 0)
        }

        val heartRates = readings.map { it.heartRate }
        val anomalies = readings.count { it.isAnomaly }

        return SensorStatistics(
            min = heartRates.minOrNull() ?: 0,
            max = heartRates.maxOrNull() ?: 0,
            average = heartRates.average().toInt(),
            standardDeviation = calculateStandardDeviation(heartRates),
            anomalyCount = anomalies,
            totalReadings = readings.size
        )
    }

    private fun calculateStandardDeviation(values: List<Int>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }

    private data class Pattern(val type: AnomalyPattern, val duration: Int)
}

enum class AnomalyPattern {
    TACHYCARDIA,      // High heart rate pattern
    BRADYCARDIA,      // Low heart rate pattern
    CRITICAL_HIGH,    // Critical high pattern
    CRITICAL_LOW,     // Critical low pattern
    IRREGULAR,        // Irregular rhythm pattern
    SUDDEN_DROP       // Sudden drop pattern
}

data class SensorStatistics(
    val min: Int,
    val max: Int,
    val average: Int,
    val standardDeviation: Double,
    val anomalyCount: Int,
    val totalReadings: Int
)


