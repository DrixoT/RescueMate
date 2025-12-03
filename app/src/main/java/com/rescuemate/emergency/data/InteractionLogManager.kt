package com.rescuemate.emergency.data

import android.content.Context
import android.util.Log
import com.rescuemate.ai.TinyLlamaInferenceService
import com.rescuemate.data.repository.FirestoreRepository
import com.rescuemate.emergency.data.database.EmergencyDatabaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class InteractionLogManager(private val context: Context) {

    companion object {
        private const val TAG = "InteractionLogManager"
    }

    private val dbHelper = EmergencyDatabaseHelper(context)
    private val firestoreRepository = try { FirestoreRepository() } catch (e: Exception) { null }
    private val tinyLlamaService = TinyLlamaInferenceService.getInstance(context)

    fun saveLog(userId: String, transcript: String, type: String) {
        if (transcript.isBlank()) {
            Log.d(TAG, "Transcript empty, skipping log.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            var summary = "Voice Interaction"
            try {
                Log.d(TAG, "Generating summary for log with TinyLlama...")
                
                
                tinyLlamaService.initialize()
                
                val systemPrompt = "You are a medical assistant. Analyze the following conversation and provide a response in exactly this format:\n" +
                        "Title: [Symptom Name Only] (e.g. 'Semi-Handicap', 'Chest Pain'). Max 3-4 words. NO extra text.\n" +
                        "Summary: [Detailed clinical summary of symptoms, actions, and advice. Do not miss any medical details.]\n" +
                        "Do not include transcripts or other text."
                val generatedSummary = tinyLlamaService.generateSimpleResponse(transcript, systemPrompt)
                
                if (!generatedSummary.isNullOrBlank()) {
                    summary = generatedSummary.trim().removePrefix("\"").removeSuffix("\"")
                    Log.d(TAG, "Generated summary: $summary")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Summary generation failed, using default title", e)
            }

            try {
                val log = InteractionLog(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    timestamp = System.currentTimeMillis(),
                    summary = summary,
                    transcript = transcript,
                    type = type
                )

                // Save locally
                dbHelper.insertInteractionLog(log)
                Log.d(TAG, "Log saved locally: ${log.id}")

                // Sync to Firestore
                firestoreRepository?.saveInteractionLog(log)
                Log.d(TAG, "Log synced to Firestore: ${log.id}")

            } catch (e: Exception) {
                Log.e(TAG, "Error saving interaction log", e)
            }
        }
    }
}
