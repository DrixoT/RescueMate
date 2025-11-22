package com.rescuemate.emergency.health

import android.content.Context
import android.content.SharedPreferences
import com.rescuemate.ai.TinyLlamaInferenceService
import com.rescuemate.emergency.EmergencyConstants
import com.rescuemate.emergency.data.HealthData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
 * 
 * Priority order:
 * 1. TinyLlama (PRIMARY) - Fast, offline, private
 * 2. OpenAI GPT-4 (OPTIONAL) - Enhancement for critical cases when online
 * 3. Rule-based (FALLBACK) - Always available
 */
class HealthMonitoringService(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        EmergencyConstants.PREF_NAME_EMERGENCY,
        Context.MODE_PRIVATE
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    // TinyLlama - PRIMARY LLM (local, offline, private)
    private val tinyLlamaService = TinyLlamaInferenceService(context)
    private var isTinyLlamaInitialized = false
    
    // Cache for LLM responses to avoid redundant calls
    private val analysisCache = mutableMapOf<String, Pair<HealthAnalysisResult, Long>>()
    private val CACHE_DURATION_MS = 60_000L // 1 minute cache

    private val heartRateHistory = mutableListOf<HeartRateReading>()
    private var baselineHeartRate: Int = prefs.getInt(EmergencyConstants.PREF_KEY_USER_BASELINE_HEART_RATE, 70)
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        // Initialize TinyLlama in background
        scope.launch {
            try {
                isTinyLlamaInitialized = tinyLlamaService.initialize()
                android.util.Log.d("HealthMonitoringService", 
                    "TinyLlama initialization: ${if (isTinyLlamaInitialized) "SUCCESS" else "FAILED"}")
            } catch (e: Exception) {
                android.util.Log.e("HealthMonitoringService", "Failed to initialize TinyLlama", e)
                isTinyLlamaInitialized = false
            }
        }
    }

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
     * Priority: TinyLlama (PRIMARY) -> GPT-4 (OPTIONAL) -> Rule-based (FALLBACK)
     */
    suspend fun analyzeHealthStatus(
        currentHeartRate: Int,
        activityLevel: HealthData.ActivityLevel,
        isExercising: Boolean,
        openAIApiKey: String? = null  // Optional enhancement
    ): HealthAnalysisResult = withContext(Dispatchers.IO) {
        try {
            // Get recent heart rate trend
            val recentReadings = synchronized(heartRateHistory) {
                heartRateHistory.takeLast(20)
            }

            // Basic rule-based analysis (always performed as fallback)
            val basicAnalysis = performBasicAnalysis(currentHeartRate, activityLevel, isExercising)

            // Check cache first
            val cacheKey = generateCacheKey(currentHeartRate, recentReadings, activityLevel, isExercising)
            val cachedResult = getCachedAnalysis(cacheKey)
            if (cachedResult != null) {
                android.util.Log.d("HealthMonitoringService", "Using cached LLM analysis")
                return@withContext cachedResult
            }

            // PRIORITY 1: Try TinyLlama (PRIMARY - local, offline, private)
            if (isTinyLlamaInitialized && recentReadings.size >= 5) {
                val tinyLlamaResult = performTinyLlamaAnalysis(
                    currentHeartRate,
                    recentReadings,
                    activityLevel,
                    isExercising
                )
                
                if (tinyLlamaResult != null) {
                    android.util.Log.d("HealthMonitoringService", "TinyLlama analysis successful")
                    // Cache the result
                    cacheAnalysis(cacheKey, tinyLlamaResult)
                    
                    // PRIORITY 2: Optionally enhance with GPT-4 for critical cases (when online + user consent)
                    if (tinyLlamaResult.riskScore >= 0.8f && 
                        !openAIApiKey.isNullOrEmpty() && 
                        recentReadings.size >= 5) {
                        android.util.Log.d("HealthMonitoringService", 
                            "High risk detected, attempting GPT-4 enhancement")
                        val enhancedResult = performOpenAIAnalysisWithRetry(
                            currentHeartRate,
                            recentReadings,
                            activityLevel,
                            isExercising,
                            openAIApiKey
                        )
                        if (enhancedResult != null) {
                            android.util.Log.d("HealthMonitoringService", "GPT-4 enhancement successful")
                            return@withContext enhancedResult
                        }
                    }
                    
                    return@withContext tinyLlamaResult
                } else {
                    android.util.Log.w("HealthMonitoringService", 
                        "TinyLlama analysis failed, trying fallback")
                }
            }

            // PRIORITY 2: Fallback to GPT-4 if TinyLlama unavailable (optional)
            if (!openAIApiKey.isNullOrEmpty() && recentReadings.size >= 5) {
                val gpt4Result = performOpenAIAnalysisWithRetry(
                    currentHeartRate,
                    recentReadings,
                    activityLevel,
                    isExercising,
                    openAIApiKey
                )
                
                if (gpt4Result != null) {
                    android.util.Log.d("HealthMonitoringService", "GPT-4 analysis successful (fallback)")
                    cacheAnalysis(cacheKey, gpt4Result)
                    return@withContext gpt4Result
                } else {
                    android.util.Log.w("HealthMonitoringService", 
                        "GPT-4 analysis failed, using basic analysis")
                }
            }

            // PRIORITY 3: Fallback to rule-based analysis
            android.util.Log.d("HealthMonitoringService", "Using rule-based analysis (fallback)")
            basicAnalysis

        } catch (e: Exception) {
            android.util.Log.e("HealthMonitoringService", "Error in analyzeHealthStatus", e)
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
     * Perform TinyLlama analysis (PRIMARY - local, offline)
     */
    private suspend fun performTinyLlamaAnalysis(
        currentHeartRate: Int,
        recentReadings: List<HeartRateReading>,
        activityLevel: HealthData.ActivityLevel,
        isExercising: Boolean
    ): HealthAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            val prompt = buildHealthAnalysisPrompt(currentHeartRate, recentReadings, activityLevel, isExercising)
            
            val response = tinyLlamaService.generateHealthAnalysis(prompt, maxTokens = 200)
            
            if (response != null) {
                return@withContext parseHealthAnalysisResult(response)
            }
            
            return@withContext null
        } catch (e: Exception) {
            android.util.Log.e("HealthMonitoringService", "TinyLlama analysis error", e)
            return@withContext null
        }
    }

    /**
     * Perform OpenAI GPT-4 analysis with retry logic (OPTIONAL ENHANCEMENT)
     */
    private suspend fun performOpenAIAnalysisWithRetry(
        currentHeartRate: Int,
        recentReadings: List<HeartRateReading>,
        activityLevel: HealthData.ActivityLevel,
        isExercising: Boolean,
        apiKey: String,
        maxRetries: Int = 2
    ): HealthAnalysisResult? = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        
        for (attempt in 0..maxRetries) {
            try {
                val result = performOpenAIAnalysis(
                    currentHeartRate,
                    recentReadings,
                    activityLevel,
                    isExercising,
                    apiKey
                )
                
                if (result != null) {
                    android.util.Log.d("HealthMonitoringService", 
                        "LLM analysis successful on attempt ${attempt + 1}")
                    return@withContext result
                }
            } catch (e: java.net.SocketTimeoutException) {
                lastException = e
                android.util.Log.w("HealthMonitoringService", 
                    "LLM analysis timeout on attempt ${attempt + 1}")
                if (attempt < maxRetries) {
                    delay(1000L * (attempt + 1)) // Exponential backoff
                }
            } catch (e: Exception) {
                lastException = e
                android.util.Log.e("HealthMonitoringService", 
                    "LLM analysis error on attempt ${attempt + 1}", e)
                if (attempt < maxRetries && isRetryableError(e)) {
                    delay(1000L * (attempt + 1)) // Exponential backoff
                } else {
                    break // Don't retry on non-retryable errors
                }
            }
        }
        
        android.util.Log.e("HealthMonitoringService", 
            "LLM analysis failed after $maxRetries retries", lastException)
        null
    }
    
    /**
     * Check if error is retryable
     */
    private fun isRetryableError(e: Exception): Boolean {
        return when (e) {
            is java.net.SocketTimeoutException,
            is java.net.UnknownHostException,
            is java.io.IOException -> true
            else -> false
        }
    }
    
    /**
     * Generate cache key for analysis
     */
    private fun generateCacheKey(
        currentHeartRate: Int,
        recentReadings: List<HeartRateReading>,
        activityLevel: HealthData.ActivityLevel,
        isExercising: Boolean
    ): String {
        val recentAvg = if (recentReadings.isNotEmpty()) {
            recentReadings.map { it.heartRate }.average().toInt()
        } else {
            currentHeartRate
        }
        return "${currentHeartRate}_${recentAvg}_${activityLevel}_${isExercising}"
    }
    
    /**
     * Get cached analysis if still valid
     */
    private fun getCachedAnalysis(cacheKey: String): HealthAnalysisResult? {
        synchronized(analysisCache) {
            val cached = analysisCache[cacheKey]
            if (cached != null) {
                val (result, timestamp) = cached
                if (System.currentTimeMillis() - timestamp < CACHE_DURATION_MS) {
                    return result
                } else {
                    analysisCache.remove(cacheKey)
                }
            }
        }
        return null
    }
    
    /**
     * Cache analysis result
     */
    private fun cacheAnalysis(cacheKey: String, result: HealthAnalysisResult) {
        synchronized(analysisCache) {
            analysisCache[cacheKey] = Pair(result, System.currentTimeMillis())
            
            // Clean old cache entries
            val now = System.currentTimeMillis()
            analysisCache.entries.removeAll { (now - it.value.second) > CACHE_DURATION_MS }
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
                val errorBody = response.body?.string() ?: "Unknown error"
                android.util.Log.e("HealthMonitoringService", 
                    "OpenAI API error: ${response.code} - $errorBody")
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            
            try {
                val jsonResponse = JSONObject(responseBody)
                
                // Check for errors in response
                if (jsonResponse.has("error")) {
                    val error = jsonResponse.getJSONObject("error")
                    android.util.Log.e("HealthMonitoringService", 
                        "OpenAI API error: ${error.optString("message", "Unknown error")}")
                    return@withContext null
                }
                
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() == 0) {
                    android.util.Log.w("HealthMonitoringService", "No choices in OpenAI response")
                    return@withContext null
                }
                
                val content = choices
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                val result = parseHealthAnalysisResult(content)
                android.util.Log.d("HealthMonitoringService", 
                    "OpenAI analysis: abnormal=${result.isAbnormal}, risk=${result.riskScore}")
                return@withContext result
                
            } catch (e: org.json.JSONException) {
                android.util.Log.e("HealthMonitoringService", 
                    "Failed to parse OpenAI response", e)
                return@withContext null
            }

        } catch (e: java.net.SocketTimeoutException) {
            android.util.Log.e("HealthMonitoringService", 
                "OpenAI API timeout after 30 seconds", e)
            throw e // Re-throw to trigger retry
        } catch (e: java.io.IOException) {
            android.util.Log.e("HealthMonitoringService", 
                "OpenAI API network error", e)
            throw e // Re-throw to trigger retry
        } catch (e: Exception) {
            android.util.Log.e("HealthMonitoringService", 
                "Unexpected error in OpenAI analysis", e)
            null
        }
    }


    /**
     * Build prompt for LLM analysis (optimized for TinyLlama)
     */
    private fun buildHealthAnalysisPrompt(
        currentHeartRate: Int,
        recentReadings: List<HeartRateReading>,
        activityLevel: HealthData.ActivityLevel,
        isExercising: Boolean
    ): String {
        val readingsText = recentReadings.takeLast(10).joinToString(", ") {
            "${it.heartRate} BPM"
        }

        return """You are a health assistant for elderly patients. Analyze these vital signs and provide a brief, clear interpretation.

Current Vitals:
- Heart Rate: $currentHeartRate BPM
- Baseline Heart Rate: $baselineHeartRate BPM
- Activity Level: $activityLevel
- Currently Exercising: $isExercising

Recent Heart Rate Readings: $readingsText

Provide risk assessment in JSON format:
{
  "isAbnormal": true/false,
  "riskScore": 0.0-1.0,
  "alertReason": "Brief explanation",
  "recommendedAction": "What to do",
  "confidence": 0.0-1.0,
  "trendAnalysis": "Trend description"
}

Keep response simple and clear for elderly users.""".trimIndent()
    }

    /**
     * Parse LLM response into HealthAnalysisResult
     */
    private fun parseHealthAnalysisResult(jsonContent: String): HealthAnalysisResult {
        return try {
            // Extract JSON from response (may be wrapped in markdown or code blocks)
            var jsonStr = jsonContent.trim()
            
            // Remove markdown code blocks if present
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.removePrefix("```json").trim()
            }
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.removePrefix("```").trim()
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.removeSuffix("```").trim()
            }
            
            // Extract JSON object
            val jsonStart = jsonStr.indexOf('{')
            val jsonEnd = jsonStr.lastIndexOf('}') + 1
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonStr = jsonStr.substring(jsonStart, jsonEnd)
            }

            val json = JSONObject(jsonStr)
            
            // Validate and parse with defaults
            val isAbnormal = json.optBoolean("isAbnormal", false)
            val riskScore = json.optDouble("riskScore", 0.0).toFloat().coerceIn(0f, 1f)
            val alertReason = json.optString("alertReason", "No specific reason provided")
            val recommendedAction = json.optString("recommendedAction", "Continue monitoring")
            val confidence = json.optDouble("confidence", 0.5).toFloat().coerceIn(0f, 1f)
            val trendAnalysis = json.optString("trendAnalysis", "No trend analysis available")
            
            HealthAnalysisResult(
                isAbnormal = isAbnormal,
                riskScore = riskScore,
                alertReason = alertReason,
                recommendedAction = recommendedAction,
                confidence = confidence,
                trendAnalysis = trendAnalysis
            )
        } catch (e: Exception) {
            android.util.Log.e("HealthMonitoringService", 
                "Failed to parse LLM response: $jsonContent", e)
            HealthAnalysisResult(
                isAbnormal = false,
                riskScore = 0f,
                alertReason = "Failed to parse analysis: ${e.message}",
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

