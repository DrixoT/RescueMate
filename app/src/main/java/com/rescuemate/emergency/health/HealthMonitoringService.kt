package com.rescuemate.emergency.health

import android.content.Context
import android.content.SharedPreferences
import com.rescuemate.emergency.EmergencyConstants
import com.rescuemate.emergency.data.HealthData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Health Monitoring Service with LLM Integration
 * Analyzes heart rate patterns and predicts potential emergencies
 */
class HealthMonitoringService(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        EmergencyConstants.PREF_NAME_EMERGENCY,
        Context.MODE_PRIVATE
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val heartRateHistory = mutableListOf<HeartRateReading>()
    private var baselineHeartRate: Int = prefs.getInt(EmergencyConstants.PREF_KEY_USER_BASELINE_HEART_RATE, 70)

    companion object {
        private const val MAX_HISTORY_SIZE = 100 // Keep last 100 readings
        private const val ANOMALY_THRESHOLD = 0.7f // 70% confidence for anomaly

        // LLM Configuration (OpenAI)
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
    }

    data class HeartRateReading(
        val timestamp: Long,
        val heartRate: Int,
        val activityLevel: HealthData.ActivityLevel = HealthData.ActivityLevel.UNKNOWN,
        val isExercising: Boolean = false
    )

    data class HealthAnalysisResult(
        val isAbnormal: Boolean,
        val riskScore: Float,
        val alertReason: String,
        val recommendedAction: String,
        val confidence: Float,
        val trendAnalysis: String
    )

    /**
     * Record a new heart rate reading
     */
    fun recordHeartRate(
        heartRate: Int,
        activityLevel: HealthData.ActivityLevel = HealthData.ActivityLevel.UNKNOWN,
        isExercising: Boolean = false
    ) {
        val reading = HeartRateReading(
            timestamp = System.currentTimeMillis(),
            heartRate = heartRate,
            activityLevel = activityLevel,
            isExercising = isExercising
        )

        synchronized(heartRateHistory) {
            heartRateHistory.add(reading)
            if (heartRateHistory.size > MAX_HISTORY_SIZE) {
                heartRateHistory.removeAt(0)
            }
        }
    }

    /**
     * Set user's baseline heart rate
     */
    fun setBaselineHeartRate(bpm: Int) {
        baselineHeartRate = bpm
        prefs.edit().putInt(EmergencyConstants.PREF_KEY_USER_BASELINE_HEART_RATE, bpm).apply()
    }

    /**
     * Get baseline heart rate
     */
    fun getBaselineHeartRate(): Int = baselineHeartRate

    /**
     * Analyze current health status using LLM
     */
    suspend fun analyzeHealthStatus(
        currentHeartRate: Int,
        activityLevel: HealthData.ActivityLevel,
        isExercising: Boolean,
        llmApiKey: String? = null
    ): HealthAnalysisResult = withContext(Dispatchers.IO) {
        try {
            // Get recent heart rate trend
            val recentReadings = synchronized(heartRateHistory) {
                heartRateHistory.takeLast(20)
            }

            // Basic rule-based analysis
            val basicAnalysis = performBasicAnalysis(currentHeartRate, activityLevel, isExercising)

            // If we have LLM API key, enhance with AI analysis
            if (!llmApiKey.isNullOrEmpty() && recentReadings.size >= 5) {
                return@withContext performLLMAnalysis(
                    currentHeartRate,
                    recentReadings,
                    activityLevel,
                    isExercising,
                    llmApiKey
                ) ?: basicAnalysis
            }

            basicAnalysis

        } catch (e: Exception) {
            e.printStackTrace()
            HealthAnalysisResult(
                isAbnormal = false,
                riskScore = 0f,
                alertReason = "Analysis failed: ${e.message}",
                recommendedAction = "Monitor manually",
                confidence = 0f,
                trendAnalysis = "Error analyzing trend"
            )
        }
    }

    /**
     * Basic rule-based health analysis (fallback when LLM unavailable)
     */
    private fun performBasicAnalysis(
        currentHeartRate: Int,
        activityLevel: HealthData.ActivityLevel,
        isExercising: Boolean
    ): HealthAnalysisResult {
        val deviation = (currentHeartRate - baselineHeartRate).toFloat() / baselineHeartRate

        // Critical high heart rate
        if (currentHeartRate >= EmergencyConstants.HEART_RATE_CRITICAL_HIGH && !isExercising) {
            return HealthAnalysisResult(
                isAbnormal = true,
                riskScore = 0.95f,
                alertReason = "Critical high heart rate: $currentHeartRate BPM (Normal: $baselineHeartRate BPM)",
                recommendedAction = "Immediate emergency contact notification recommended",
                confidence = 0.9f,
                trendAnalysis = "Heart rate critically elevated"
            )
        }

        // Critical low heart rate
        if (currentHeartRate <= EmergencyConstants.HEART_RATE_LOW_THRESHOLD &&
            activityLevel != HealthData.ActivityLevel.STATIONARY) {
            return HealthAnalysisResult(
                isAbnormal = true,
                riskScore = 0.9f,
                alertReason = "Critical low heart rate: $currentHeartRate BPM (Normal: $baselineHeartRate BPM)",
                recommendedAction = "Immediate emergency contact notification recommended",
                confidence = 0.85f,
                trendAnalysis = "Heart rate critically low"
            )
        }

        // High heart rate when not exercising
        if (deviation > 0.5f && !isExercising && activityLevel != HealthData.ActivityLevel.EXERCISING) {
            return HealthAnalysisResult(
                isAbnormal = true,
                riskScore = 0.75f,
                alertReason = "Elevated heart rate: $currentHeartRate BPM (${(deviation * 100).toInt()}% above normal)",
                recommendedAction = "Monitor closely, prepare for emergency notification",
                confidence = 0.7f,
                trendAnalysis = "Heart rate elevated without physical activity"
            )
        }

        // Normal range
        return HealthAnalysisResult(
            isAbnormal = false,
            riskScore = 0.1f,
            alertReason = "Heart rate within normal range",
            recommendedAction = "Continue monitoring",
            confidence = 0.8f,
            trendAnalysis = "Stable heart rate"
        )
    }

    /**
     * Perform LLM-enhanced health analysis using OpenAI
     */
    private suspend fun performLLMAnalysis(
        currentHeartRate: Int,
        recentReadings: List<HeartRateReading>,
        activityLevel: HealthData.ActivityLevel,
        isExercising: Boolean,
        apiKey: String
    ): HealthAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            performOpenAIAnalysis(currentHeartRate, recentReadings, activityLevel, isExercising, apiKey)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * OpenAI GPT-4 Health Analysis
     */
    private suspend fun performOpenAIAnalysis(
        currentHeartRate: Int,
        recentReadings: List<HeartRateReading>,
        activityLevel: HealthData.ActivityLevel,
        isExercising: Boolean,
        apiKey: String
    ): HealthAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            val prompt = buildHealthAnalysisPrompt(currentHeartRate, recentReadings, activityLevel, isExercising)

            val requestBody = JSONObject().apply {
                put("model", "gpt-4")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", """You are a medical AI assistant specialized in cardiac health monitoring.
                            |Analyze heart rate patterns and provide risk assessment.
                            |Respond in JSON format with: {
                            |  "isAbnormal": boolean,
                            |  "riskScore": float (0-1),
                            |  "alertReason": string,
                            |  "recommendedAction": string,
                            |  "confidence": float (0-1),
                            |  "trendAnalysis": string
                            |}""".trimMargin())
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("temperature", 0.3)
                put("max_tokens", 500)
            }

            val request = Request.Builder()
                .url(OPENAI_API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            val jsonResponse = JSONObject(responseBody)
            val content = jsonResponse
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            parseHealthAnalysisResult(content)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    /**
     * Build prompt for LLM analysis
     */
    private fun buildHealthAnalysisPrompt(
        currentHeartRate: Int,
        recentReadings: List<HeartRateReading>,
        activityLevel: HealthData.ActivityLevel,
        isExercising: Boolean
    ): String {
        val readingsText = recentReadings.takeLast(10).joinToString("\n") {
            "Time: ${(System.currentTimeMillis() - it.timestamp) / 1000}s ago, HR: ${it.heartRate} BPM, Activity: ${it.activityLevel}"
        }

        return """
            Analyze this cardiac health data for potential emergency:
            
            Current Status:
            - Heart Rate: $currentHeartRate BPM
            - Baseline Heart Rate: $baselineHeartRate BPM
            - Activity Level: $activityLevel
            - Currently Exercising: $isExercising
            
            Recent Readings (last 10):
            $readingsText
            
            Provide risk assessment in JSON format:
            {
              "isAbnormal": true/false,
              "riskScore": 0.0-1.0,
              "alertReason": "Brief explanation",
              "recommendedAction": "What to do",
              "confidence": 0.0-1.0,
              "trendAnalysis": "Trend description"
            }
        """.trimIndent()
    }

    /**
     * Parse LLM response into HealthAnalysisResult
     */
    private fun parseHealthAnalysisResult(jsonContent: String): HealthAnalysisResult {
        return try {
            // Extract JSON from response (may be wrapped in markdown)
            val jsonStart = jsonContent.indexOf('{')
            val jsonEnd = jsonContent.lastIndexOf('}') + 1
            val jsonStr = if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonContent.substring(jsonStart, jsonEnd)
            } else {
                jsonContent
            }

            val json = JSONObject(jsonStr)
            HealthAnalysisResult(
                isAbnormal = json.getBoolean("isAbnormal"),
                riskScore = json.getDouble("riskScore").toFloat(),
                alertReason = json.getString("alertReason"),
                recommendedAction = json.getString("recommendedAction"),
                confidence = json.getDouble("confidence").toFloat(),
                trendAnalysis = json.getString("trendAnalysis")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            HealthAnalysisResult(
                isAbnormal = false,
                riskScore = 0f,
                alertReason = "Failed to parse analysis",
                recommendedAction = "Use manual monitoring",
                confidence = 0f,
                trendAnalysis = "Parse error"
            )
        }
    }

    /**
     * Check if emergency should be triggered based on health data
     */
    fun shouldTriggerEmergency(analysisResult: HealthAnalysisResult): Boolean {
        return analysisResult.isAbnormal &&
               analysisResult.riskScore >= ANOMALY_THRESHOLD &&
               analysisResult.confidence >= 0.6f
    }

    /**
     * Get current health data snapshot
     */
    fun getCurrentHealthData(
        currentHeartRate: Int,
        analysisResult: HealthAnalysisResult,
        activityLevel: HealthData.ActivityLevel = HealthData.ActivityLevel.UNKNOWN,
        isExercising: Boolean = false
    ): HealthData {
        val recentReadings = synchronized(heartRateHistory) {
            heartRateHistory.takeLast(20).map { it.heartRate }
        }

        return HealthData(
            timestamp = System.currentTimeMillis(),
            currentHeartRate = currentHeartRate,
            normalHeartRate = baselineHeartRate,
            heartRateTrend = recentReadings,
            riskScore = analysisResult.riskScore,
            alertReason = analysisResult.alertReason,
            activityLevel = activityLevel,
            isExercising = isExercising,
            stressLevel = calculateStressLevel(analysisResult.riskScore)
        )
    }

    /**
     * Calculate stress level from risk score
     */
    private fun calculateStressLevel(riskScore: Float): HealthData.StressLevel {
        return when {
            riskScore >= 0.9f -> HealthData.StressLevel.CRITICAL
            riskScore >= 0.7f -> HealthData.StressLevel.HIGH
            riskScore >= 0.5f -> HealthData.StressLevel.ELEVATED
            riskScore >= 0.3f -> HealthData.StressLevel.NORMAL
            else -> HealthData.StressLevel.LOW
        }
    }

    /**
     * Clear heart rate history
     */
    fun clearHistory() {
        synchronized(heartRateHistory) {
            heartRateHistory.clear()
        }
    }

    /**
     * Get heart rate statistics
     */
    fun getHeartRateStats(): HeartRateStats {
        val readings = synchronized(heartRateHistory) {
            heartRateHistory.map { it.heartRate }
        }

        if (readings.isEmpty()) {
            return HeartRateStats(0, 0, 0, 0.0, 0)
        }

        return HeartRateStats(
            min = readings.minOrNull() ?: 0,
            max = readings.maxOrNull() ?: 0,
            average = readings.average().toInt(),
            standardDeviation = calculateStandardDeviation(readings),
            sampleSize = readings.size
        )
    }

    data class HeartRateStats(
        val min: Int,
        val max: Int,
        val average: Int,
        val standardDeviation: Double,
        val sampleSize: Int
    )

    private fun calculateStandardDeviation(values: List<Int>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}

