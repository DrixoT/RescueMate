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

import com.rescuemate.utils.NetworkMonitor
import com.rescuemate.services.OpenAIService

class InteractionLogManager(private val context: Context) {

    companion object {
        private const val TAG = "InteractionLogManager"
    }

    private val dbHelper = EmergencyDatabaseHelper(context)
    private val firestoreRepository = try { FirestoreRepository() } catch (e: Exception) { null }
    private val tinyLlamaService = TinyLlamaInferenceService.getInstance(context)
    private val networkMonitor = NetworkMonitor(context)
    private val openAIService = OpenAIService()

    fun saveLog(userId: String, transcript: String, type: String) {
        if (transcript.isBlank()) {
            Log.d(TAG, "Transcript empty, skipping log.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val isOnline = networkMonitor.checkConnection()
            var summary = "Processing Summary..." // Temporary title
            val logId = UUID.randomUUID().toString()
            
            // 1. Create initial log object
            val log = InteractionLog(
                id = logId,
                userId = userId,
                timestamp = System.currentTimeMillis(),
                summary = summary,
                transcript = transcript,
                type = type
            )
            
            // 2. Save locally immediately
            try {
                dbHelper.insertInteractionLog(log)
                Log.d(TAG, "Log saved locally: ${log.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving initial log locally", e)
                return@launch
            }

            if (isOnline) {
                Log.d(TAG, "Online: Generating summary with OpenAI...")
                try {
                    val generatedSummary = openAIService.generateSummary(transcript)
                    if (!generatedSummary.isNullOrBlank()) {
                        summary = generatedSummary.trim().removePrefix("\"").removeSuffix("\"")
                    } else {
                         summary = "Voice Interaction"
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "OpenAI summary failed, using default", e)
                    summary = "Voice Interaction"
                }
                
                // Update log object
                val updatedLog = log.copy(summary = summary)
                
                // Save to Firestore and delete local if successful
                firestoreRepository?.let { repo ->
                    val result = repo.saveInteractionLog(updatedLog)
                    if (result.isSuccess) {
                        Log.d(TAG, "Log synced to Firestore, deleting local copy.")
                        dbHelper.deleteInteractionLog(logId)
                    } else {
                         Log.w(TAG, "Firestore sync failed, updating local summary only.")
                         dbHelper.updateInteractionLog(updatedLog)
                    }
                }
                
                // Also try to sync any other pending logs
                syncAndClearLogs(userId)
                
            } else {
                Log.d(TAG, "Offline: Queuing background summarization...")
                // Launch background task for offline summarization
                processOfflineSummary(log)
            }
        }
    }

    /**
     * Process offline summary using TinyLlama when it's free.
     * This runs in background to avoid blocking the conversation thread.
     */
    private fun processOfflineSummary(log: InteractionLog) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Wait a bit to ensure conversation fully releases resources
                // In a real queue system we'd wait for a specific signal, but a delay helps here.
                kotlinx.coroutines.delay(2000)
                
                Log.d(TAG, "Starting background offline summarization for ${log.id}...")
                
                // Ensure service is initialized (it might have been cleaned up)
                // This call blocks if mutex is held by conversation
                tinyLlamaService.initialize()
                
                val safeTranscript = if (log.transcript.length > 2000) {
                     log.transcript.take(2000) + "...[truncated]"
                } else {
                     log.transcript
                }

                val systemPrompt = "You are a medical assistant. Analyze the following conversation and provide a response in exactly this format:\n" +
                        "Title: [Symptom Name Only] (e.g. 'Semi-Handicap', 'Chest Pain'). Max 3-4 words. NO extra text.\n" +
                        "Summary: [Detailed clinical summary of symptoms, actions, and advice. Do not miss any medical details.]\n" +
                        "Do not include transcripts or other text."
                        
                val generatedSummary = tinyLlamaService.generateSimpleResponse(safeTranscript, systemPrompt)
                
                if (!generatedSummary.isNullOrBlank()) {
                    val finalSummary = generatedSummary.trim().removePrefix("\"").removeSuffix("\"")
                    val updatedLog = log.copy(summary = finalSummary)
                    dbHelper.updateInteractionLog(updatedLog)
                    Log.d(TAG, "Offline summary updated locally: $finalSummary")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in offline summarization", e)
            }
        }
    }

    /**
     * Syncs all local logs to Firestore and deletes them upon success.
     */
    private fun syncAndClearLogs(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Starting sync of local logs...")
            val result = dbHelper.getInteractionLogs(userId, limit = 100) // Sync batch
            val logs = result.getOrNull() ?: return@launch
            
            if (logs.isEmpty()) {
                Log.d(TAG, "No local logs to sync.")
                return@launch
            }
            
            logs.forEach { log ->
                try {
                    // Skip logs that are still processing (if any marker exists, or just try to sync what we have)
                    firestoreRepository?.let { repo ->
                        val syncResult = repo.saveInteractionLog(log)
                        if (syncResult.isSuccess) {
                            dbHelper.deleteInteractionLog(log.id)
                            Log.d(TAG, "Synced and deleted local log: ${log.id}")
                        } else {
                             Log.w(TAG, "Failed to sync log: ${log.id}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing log: ${log.id}", e)
                }
            }
        }
    }
}
