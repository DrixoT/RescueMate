package com.rescuemate.utils

/**
 * Health Monitoring Constants
 * Centralized health-related configuration values
 */
object HealthConstants {
    
    // Default Heart Rate Values
    const val DEFAULT_RESTING_HEART_RATE = 70 // BPM
    const val DEFAULT_MAX_HEART_RATE = 180 // BPM
    const val DEFAULT_MIN_HEART_RATE = 40 // BPM
    
    // Heart Rate Thresholds
    const val HEART_RATE_VERY_LOW_THRESHOLD = 40 // BPM
    const val HEART_RATE_LOW_THRESHOLD = 50 // BPM
    const val HEART_RATE_HIGH_THRESHOLD = 100 // BPM
    const val HEART_RATE_VERY_HIGH_THRESHOLD = 120 // BPM
    const val HEART_RATE_CRITICAL_THRESHOLD = 180 // BPM
    
    // Heart Rate Variability
    const val HEART_RATE_DEVIATION_WARNING = 0.3f // 30% deviation
    const val HEART_RATE_DEVIATION_ALERT = 0.5f // 50% deviation
    const val HEART_RATE_DEVIATION_CRITICAL = 0.7f // 70% deviation
    
    // Blood Pressure
    const val NORMAL_SYSTOLIC_BP = 120 // mmHg
    const val NORMAL_DIASTOLIC_BP = 80 // mmHg
    const val HIGH_SYSTOLIC_BP = 140 // mmHg
    const val HIGH_DIASTOLIC_BP = 90 // mmHg
    const val CRITICAL_SYSTOLIC_BP = 180 // mmHg
    const val CRITICAL_DIASTOLIC_BP = 120 // mmHg
    
    // Oxygen Saturation (SpO2)
    const val NORMAL_SPO2 = 95 // percentage
    const val LOW_SPO2_WARNING = 92 // percentage
    const val LOW_SPO2_CRITICAL = 88 // percentage
    
    // Temperature (Celsius)
    const val NORMAL_BODY_TEMP = 37.0f // °C
    const val LOW_TEMP_THRESHOLD = 35.0f // °C (Hypothermia)
    const val HIGH_TEMP_THRESHOLD = 38.0f // °C (Fever)
    const val CRITICAL_TEMP_THRESHOLD = 39.5f // °C (High Fever)
    
    // Monitoring Intervals
    const val HEALTH_CHECK_INTERVAL_MS = 5_000L // 5 seconds
    const val HEART_RATE_SAMPLE_WINDOW = 10 // Number of samples to average
    const val ANOMALY_DETECTION_WINDOW_MS = 60_000L // 1 minute
    const val CONTINUOUS_ANOMALY_THRESHOLD = 3 // Number of consecutive anomalies
    
    // Risk Score Thresholds
    const val RISK_SCORE_LOW = 0.3f
    const val RISK_SCORE_MEDIUM = 0.5f
    const val RISK_SCORE_HIGH = 0.7f
    const val RISK_SCORE_CRITICAL = 0.9f
    
    // Activity Level Thresholds (based on accelerometer magnitude)
    const val ACTIVITY_STATIONARY_THRESHOLD = 1.5f // m/s²
    const val ACTIVITY_WALKING_THRESHOLD = 3.0f // m/s²
    const val ACTIVITY_RUNNING_THRESHOLD = 6.0f // m/s²
    
    // Fall Detection
    const val FALL_ACCELERATION_THRESHOLD = 15.0f // m/s²
    const val FALL_CONFIRMATION_DELAY_MS = 2_000L // 2 seconds
    const val FALL_IMPACT_DURATION_MS = 500L // 500ms
    
    // Stress Level Detection
    const val STRESS_HEART_RATE_MULTIPLIER = 1.2f // 20% above normal
    const val STRESS_HRV_THRESHOLD = 50f // milliseconds
    
    // Data Quality
    const val MIN_HEART_RATE_CONFIDENCE = 0.7f // 70% confidence
    const val MAX_SENSOR_READ_ERROR_RATE = 0.2f // 20% error tolerance
    const val SENSOR_CALIBRATION_SAMPLES = 10
    
    // Age-Based Heart Rate Calculations
    fun getMaxHeartRateForAge(age: Int): Int {
        return 220 - age
    }
    
    fun getTargetHeartRateZone(age: Int): Pair<Int, Int> {
        val maxHR = getMaxHeartRateForAge(age)
        val lowerBound = (maxHR * 0.5).toInt() // 50% of max
        val upperBound = (maxHR * 0.85).toInt() // 85% of max
        return Pair(lowerBound, upperBound)
    }
    
    fun getRestingHeartRateRange(age: Int): Pair<Int, Int> {
        return when (age) {
            in 18..25 -> Pair(60, 100)
            in 26..35 -> Pair(60, 100)
            in 36..45 -> Pair(60, 100)
            in 46..55 -> Pair(60, 100)
            in 56..65 -> Pair(60, 100)
            else -> Pair(60, 100)
        }
    }
    
    // Body Mass Index (BMI) Categories
    const val BMI_UNDERWEIGHT = 18.5f
    const val BMI_NORMAL = 25.0f
    const val BMI_OVERWEIGHT = 30.0f
    const val BMI_OBESE = 35.0f
    
    fun calculateBMI(weightKg: Float, heightMeters: Float): Float {
        if (heightMeters <= 0) return 0f
        return weightKg / (heightMeters * heightMeters)
    }
    
    fun getBMICategory(bmi: Float): String {
        return when {
            bmi < BMI_UNDERWEIGHT -> "Underweight"
            bmi < BMI_NORMAL -> "Normal"
            bmi < BMI_OVERWEIGHT -> "Overweight"
            bmi < BMI_OBESE -> "Obese"
            else -> "Severely Obese"
        }
    }
}

