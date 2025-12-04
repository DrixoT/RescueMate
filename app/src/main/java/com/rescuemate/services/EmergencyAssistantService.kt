package com.rescuemate.services

import android.util.Log

/**
 * Emergency Assistant Service
 * Provides system prompts and safety checks for the Local Voice LLM.
 * Specialized for emergency situations and medical conditions with a calm, supportive persona.
 */
class EmergencyAssistantService {

    companion object {
        private const val TAG = "EmergencyAssistant"
        private const val GREETING = "Hey I'm Res, How can I help you today?"
    }

    /**
     * Get the initial greeting message
     */
    fun getGreeting(): String {
        return GREETING
    }

    /**
     * Check if the message indicates a life-threatening emergency
     */
    fun isLifeThreatening(message: String): Boolean {
        val msg = message.lowercase()
        return msg.contains("chest pain") || msg.contains("can't breathe") ||
               msg.contains("heart attack") || msg.contains("choking") ||
               msg.contains("unconscious") || msg.contains("not breathing") ||
               msg.contains("severe bleeding") || msg.contains("anaphylaxis")
    }

    /**
     * Get emergency-specific system prompt for LLM
     * Includes real-time vital signs and improved safety protocol
     * 
     * @param heartRate Current heart rate in BPM
     * @param spO2 Current oxygen saturation in %, or null if not available
     */
    fun getSystemPrompt(heartRate: Int? = null, spO2: Int? = null): String {
        val vitalsSection = StringBuilder("Current Vitals:\n")
        vitalsSection.append(if (heartRate != null) "- Heart Rate: $heartRate BPM\n" else "- Heart Rate: Unknown\n")
        vitalsSection.append(if (spO2 != null) "- SpO2: $spO2 %\n" else "- SpO2: Unknown\n")
        
        val safetyProtocol = """
            SAFETY PROTOCOL:
            1. You are Res, a calm and supportive medical assistant.
            2. Your goal is to calm the user down and provide clear, helpful medical guidance.
            3. If the user presents with life-threatening symptoms (e.g., chest pain, severe bleeding, unconsciousness), gently but firmly recommend calling emergency services (911) immediately.
            4. You CAN provide first-aid tips and general medical advice (e.g., "Apply pressure," "Sit upright," "Drink water").
            5. Use calming language: "I'm here with you," "Take a breath," "Let's handle this together."
        """.trimIndent()

        return """You are Res, a calm and supportive medical assistant.
        
$vitalsSection

$safetyProtocol

Your Role:
- Be NICE, CALM, and SUPPORTIVE.
- Use simple, soothing language.
- Provide practical medical advice for non-emergencies.
- For emergencies, guide them to call 911 but remain supportive.
- Keep responses conversational but concise.

Example Responses:
- "I hear that you're in pain. I'm right here. Let's take a moment. If the pain is in your chest, we should call 911 just to be safe."
- "It sounds like you're feeling anxious. Take a deep breath with me. In... and out. Tell me more about what you're feeling."
- "For that scrape, try to clean it with water if you can. I'm here with you."
"""
    }
    
    /**
     * Check if fallback safety message should be triggered
     */
    fun shouldTriggerFallback(message: String): Boolean {
        // If LLM response is empty, unsafe, or hallucinates safety, we might need fallback
        // For now, we use this to flag if the User's input was critically dangerous and LLM might miss it
        // This is a secondary check.
        return isLifeThreatening(message)
    }
}
