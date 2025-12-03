package com.rescuemate.services

import android.util.Log
import com.rescuemate.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAIService {

    companion object {
        private const val TAG = "OpenAIService"
        private const val API_URL = "https://api.openai.com/v1/chat/completions"
        private const val MODEL = "gpt-3.5-turbo"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey = BuildConfig.OPENAI_API_KEY

    suspend fun generateSummary(transcript: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.e(TAG, "OpenAI API Key is missing")
            return@withContext "Summary unavailable (API Key missing)"
        }

        if (transcript.isBlank()) {
            return@withContext "No conversation recorded."
        }

        try {
            val systemPrompt = "You are a medical assistant. Analyze the following conversation and provide a response in exactly this format:\n" +
                    "Title: [Short, precise medical condition or main symptom, max 5 words]\n" +
                    "Summary: [Detailed clinical summary of symptoms, actions, and advice. Do not miss any medical details.]"
            
            val jsonBody = JSONObject().apply {
                put("model", MODEL)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", transcript)
                    })
                })
                put("max_tokens", 250)
                put("temperature", 0.5)
            }

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "API call failed: ${response.code} ${response.message}")
                    return@withContext "Summary generation failed."
                }

                val responseBody = response.body?.string() ?: return@withContext "Empty response"
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.optJSONArray("choices")
                val message = choices?.optJSONObject(0)?.optJSONObject("message")
                val content = message?.optString("content")

                return@withContext content?.trim() ?: "Could not parse summary."
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error generating summary", e)
            return@withContext "Error generating summary."
        }
    }
}
