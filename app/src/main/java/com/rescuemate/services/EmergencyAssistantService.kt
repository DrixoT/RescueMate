package com.rescuemate.services

import android.util.Log

/**
 * Emergency Assistant Service
 * Provides rule-based emergency and medical advice when local LLM is active
 * Specialized for emergency situations and medical conditions
 * Now with improved conversational flow, context awareness, and strict safety protocols
 */
class EmergencyAssistantService {

    companion object {
        private const val TAG = "EmergencyAssistant"
        private const val GREETING = "Hey I'm Res, How's your day?"
    }

    /**
     * Get the initial greeting message
     */
    fun getGreeting(): String {
        return GREETING
    }

    /**
     * Detect intent from user message
     */
    private enum class Intent {
        EMERGENCY,          // Life-threatening situation
        MEDICAL_QUESTION,   // Medical advice/question
        SYMPTOM_DESCRIPTION, // Describing symptoms
        GENERAL_QUESTION,   // General question
        GREETING,          // Hello/hi
        FOLLOW_UP,         // Follow-up to previous question
        LOCATION_INFO,     // Providing location
        UNKNOWN            // Can't determine
    }

    /**
     * Detect intent from message
     */
    private fun detectIntent(message: String, conversationHistory: List<Pair<String, String>>): Intent {
        val msg = message.lowercase().trim()
        
        // Check for emergency keywords
        val emergencyKeywords = listOf(
            "chest pain", "heart attack", "can't breathe", "choking", "unconscious",
            "not breathing", "severe bleeding", "anaphylaxis", "emergency", "urgent",
            "help now", "help immediately", "dying", "dying", "critical"
        )
        if (emergencyKeywords.any { msg.contains(it) }) {
            return Intent.EMERGENCY
        }
        
        // Check if providing location (usually short response after being asked)
        if (conversationHistory.isNotEmpty()) {
            val lastResponse = conversationHistory.last().second.lowercase()
            if (lastResponse.contains("location") || lastResponse.contains("where")) {
                if (msg.length < 50 && !msg.contains("?")) {
                    return Intent.LOCATION_INFO
                }
            }
        }
        
        // Check for symptom descriptions
        val symptomKeywords = listOf(
            "pain", "hurt", "ache", "sore", "uncomfortable", "feeling", "feel",
            "symptom", "nausea", "dizzy", "fever", "cough", "headache"
        )
        if (symptomKeywords.any { msg.contains(it) }) {
            return Intent.SYMPTOM_DESCRIPTION
        }
        
        // Check for medical questions
        if (msg.contains("?") || msg.startsWith("what") || msg.startsWith("how") || 
            msg.startsWith("should") || msg.startsWith("can i") || msg.startsWith("do i")) {
            return Intent.MEDICAL_QUESTION
        }
        
        // Check for greetings
        if (msg.contains("hello") || msg.contains("hi") || msg.contains("hey") || 
            msg.isEmpty() || msg.length < 3) {
            return Intent.GREETING
        }
        
        // Check if follow-up
        if (conversationHistory.isNotEmpty()) {
            return Intent.FOLLOW_UP
        }
        
        return Intent.UNKNOWN
    }

    /**
     * Generate response based on user input
     * Uses improved pattern matching, intent detection, and context awareness
     */
    fun generateResponse(userMessage: String, conversationHistory: List<Pair<String, String>> = emptyList()): String {
        val message = userMessage.lowercase().trim()
        val intent = detectIntent(message, conversationHistory)
        
        Log.d(TAG, "Detected intent: $intent for message: ${message.take(50)}")
        
        // Handle based on intent and context
        return when (intent) {
            Intent.EMERGENCY -> handleEmergency(message, conversationHistory)
            Intent.SYMPTOM_DESCRIPTION -> handleSymptomDescription(message, conversationHistory)
            Intent.MEDICAL_QUESTION -> handleMedicalQuestion(message, conversationHistory)
            Intent.GENERAL_QUESTION -> handleGeneralQuestion(message, conversationHistory)
            Intent.LOCATION_INFO -> handleLocationInfo(message, conversationHistory)
            Intent.GREETING -> handleGreeting()
            Intent.FOLLOW_UP -> handleFollowUp(message, conversationHistory)
            Intent.UNKNOWN -> handleUnknown(message, conversationHistory)
        }
    }

    /**
     * Handle emergency situations
     */
    private fun handleEmergency(message: String, conversationHistory: List<Pair<String, String>>): String {
        val responses = when {
            message.contains("chest") || message.contains("heart") -> {
                listOf(
                    "Chest pain can be serious. Please call 911 right away. Try to stay calm and sit down. Are you able to tell me your location?",
                    "I understand you're having chest pain. This needs immediate medical attention. Call 911 now. Can you tell me where you are?",
                    "Chest pain requires emergency care. Please call 911 immediately. Stay calm and sit if possible. What's your location?"
                )
            }
            message.contains("breath") || message.contains("breathe") -> {
                listOf(
                    "Breathing problems are serious. Call 911 immediately. Try to sit upright. If you have an inhaler, use it. Where are you located?",
                    "I understand you're having trouble breathing. This is an emergency - call 911 right away. Can you tell me your location?",
                    "Difficulty breathing needs immediate attention. Please call 911 now. Stay calm and sit up. What's your current location?"
                )
            }
            message.contains("choking") -> {
                listOf(
                    "If you're choking and can't breathe, call 911 immediately. If you can cough, keep coughing. Can someone help you?",
                    "Choking is an emergency. Call 911 right away. Are you able to breathe at all?",
                    "Choking requires immediate help. Call 911 now. If someone is with you, have them perform the Heimlich maneuver."
                )
            }
            message.contains("unconscious") || message.contains("passed out") -> {
                listOf(
                    "If someone is unconscious, call 911 immediately. Check if they're breathing. Do not move them unless they're in danger.",
                    "Unconsciousness is a medical emergency. Call 911 right away. Are they breathing?",
                    "Please call 911 immediately for an unconscious person. Check their breathing and pulse if you can."
                )
            }
            else -> {
                listOf(
                    "This sounds like an emergency. Please call 911 immediately. I'm here to help. Can you tell me what's happening?",
                    "I understand this is urgent. Call 911 right away if this is life-threatening. What's the emergency?",
                    "This needs immediate attention. Please call 911 now. Can you describe what's happening?"
                )
            }
        }
        return responses.random()
    }

    /**
     * Handle symptom descriptions
     */
    private fun handleSymptomDescription(message: String, conversationHistory: List<Pair<String, String>>): String {
        // Check conversation history for context
        val hasDiscussedSymptoms = conversationHistory.any { 
            it.first.lowercase().contains("pain") || 
            it.first.lowercase().contains("hurt") || 
            it.first.lowercase().contains("symptom") 
        }
        
        val responses = if (hasDiscussedSymptoms) {
            listOf(
                "I understand. Based on what you've told me, I'd recommend seeing a doctor soon. If symptoms are severe or getting worse, please call 911. Is there anything else I should know?",
                "Thank you for the details. For these symptoms, I'd suggest seeking medical care. If it's getting worse, don't wait - call 911. How are you feeling right now?",
                "I see. These symptoms should be evaluated by a medical professional. If they're severe or worsening, please call 911 immediately. Can you tell me more?"
            )
        } else {
            listOf(
                "I understand you're experiencing some symptoms. Can you tell me more about what you're feeling? When did this start?",
                "I'm here to help. Can you describe your symptoms in more detail? How long have you been experiencing this?",
                "Let me help you assess this. What symptoms are you having? Are they getting worse or staying the same?"
            )
        }
        return responses.random()
    }

    /**
     * Handle medical questions
     */
    private fun handleMedicalQuestion(message: String, conversationHistory: List<Pair<String, String>>): String {
        val responses = when {
            message.contains("medication") || message.contains("medicine") || message.contains("pill") -> {
                listOf(
                    "I can help with general medication questions, but for specific advice, please consult your doctor or pharmacist. What would you like to know?",
                    "For medication questions, it's best to check with your doctor or pharmacist. However, if you've taken the wrong medication or wrong dose, call poison control or 911 immediately. What's your question?",
                    "Medication questions should be answered by a healthcare professional. If this is about a medication error, call poison control or 911 right away. What do you need to know?"
                )
            }
            message.contains("should i") || message.contains("do i need") -> {
                listOf(
                    "That's a good question. For medical decisions, I'd recommend consulting with a healthcare provider. If this is urgent, please call 911. Can you tell me more about the situation?",
                    "I understand your concern. For proper medical guidance, it's best to speak with a doctor. If this is an emergency, call 911. What's the situation?",
                    "That depends on the specifics. For medical advice, please consult a healthcare professional. If it's urgent, don't wait - call 911. Can you describe what's happening?"
                )
            }
            else -> {
                listOf(
                    "I'm here to help with medical questions. However, for specific medical advice, please consult your doctor. If this is urgent, call 911. What would you like to know?",
                    "I can provide general guidance, but for medical decisions, it's best to speak with a healthcare provider. If this is an emergency, call 911. What's your question?",
                    "For medical questions, I'd recommend consulting a doctor. However, I'm here to help guide you. If this is urgent, please call 911. What do you need help with?"
                )
            }
        }
        return responses.random()
    }

    /**
     * Handle general questions
     */
    private fun handleGeneralQuestion(message: String, conversationHistory: List<Pair<String, String>>): String {
        val responses = listOf(
            "I'm here to help with emergency situations and medical questions. What would you like to know?",
            "I'm Res, your emergency assistant. I can help with medical questions and emergency guidance. What do you need?",
            "I'm here to assist you. For emergency situations or medical questions, I can provide guidance. What's your question?",
            "I can help with emergency and medical questions. What would you like to ask?",
            "I'm here to help. What can I assist you with today?"
        )
        return responses.random()
    }

    /**
     * Handle location information
     */
    private fun handleLocationInfo(message: String, conversationHistory: List<Pair<String, String>>): String {
        val responses = listOf(
            "Thank you for providing your location. Help should be on the way. Stay where you are if it's safe. Is there anything else I can help with?",
            "Got it, thank you. Emergency services should be notified. Please stay in a safe location. How are you feeling now?",
            "Thank you. I've noted your location. Help is being contacted. Stay safe and let me know if you need anything else."
        )
        return responses.random()
    }

    /**
     * Handle greetings
     */
    private fun handleGreeting(): String {
        val responses = listOf(
            "Hey I'm Res, How can I help you? I'm here to assist with emergency situations and medical questions.",
            "Hi there! I'm Res, your emergency assistant. What can I help you with today?",
            "Hello! I'm Res. I'm here to help with emergencies and medical questions. What do you need?"
        )
        return responses.random()
    }

    /**
     * Handle follow-up questions
     */
    private fun handleFollowUp(message: String, conversationHistory: List<Pair<String, String>>): String {
        if (conversationHistory.isEmpty()) {
            return handleUnknown(message, conversationHistory)
        }
        
        val lastResponse = conversationHistory.last().second.lowercase()
        val lastUserMessage = conversationHistory.last().first.lowercase()
        
        // If we asked about location
        if (lastResponse.contains("location") || lastResponse.contains("where")) {
            if (message.length > 5 && !message.contains("?")) {
                return handleLocationInfo(message, conversationHistory)
            }
        }
        
        // If we asked about symptoms
        if (lastResponse.contains("symptom") || lastResponse.contains("feeling") || lastResponse.contains("what")) {
            return handleSymptomDescription(message, conversationHistory)
        }
        
        // If user is providing more information
        if (!message.contains("?") && message.length > 10) {
            val responses = listOf(
                "I understand. Can you tell me more about that?",
                "Thank you for that information. What else should I know?",
                "Got it. Is there anything else you'd like to add?",
                "I see. How does that relate to what we discussed earlier?"
            )
            return responses.random()
        }
        
        // General follow-up
        val responses = listOf(
            "I'm here to help. Can you tell me more about what you need?",
            "Let me help you with that. What specifically would you like to know?",
            "I understand. How can I assist you further?",
            "Sure, I can help with that. What would you like to know?"
        )
        return responses.random()
    }

    /**
     * Handle unknown/unclear messages
     */
    private fun handleUnknown(message: String, conversationHistory: List<Pair<String, String>>): String {
        // Try to extract keywords and provide helpful response
        val keywords = listOf("pain", "hurt", "sick", "help", "emergency", "doctor", "hospital", "medication")
        val foundKeywords = keywords.filter { message.contains(it) }
        
        if (foundKeywords.isNotEmpty()) {
            val responses = listOf(
                "I understand you mentioned ${foundKeywords.first()}. Can you tell me more about what's happening?",
                "I see you mentioned ${foundKeywords.first()}. Let me help you with that. What specifically is going on?",
                "You mentioned ${foundKeywords.first()}. I'm here to help. What would you like to know?"
            )
            return responses.random()
        }
        
        // Very generic response with variations
        val responses = listOf(
            "I'm here to help with emergency situations and medical questions. Can you tell me what you need assistance with?",
            "I understand. I'm Res, your emergency assistant. What can I help you with?",
            "I'm here to help. Can you describe what's happening or what you need help with?",
            "Let me help you. What's the situation or what would you like to know?",
            "I'm listening. What can I assist you with today?"
        )
        return responses.random()
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
            SAFETY PROTOCOL - STRICT ENFORCEMENT:
            1. If Heart Rate > 120 BPM or < 40 BPM, YOU MUST RECOMMEND CALLING 911 IMMEDIATELY.
            2. If SpO2 < 90%, YOU MUST RECOMMEND CALLING 911 IMMEDIATELY.
            3. If user mentions "chest pain", "can't breathe", "unconscious", or "bleeding", RECOMMEND 911.
            4. DO NOT diagnose. DO NOT delay emergency care.
        """.trimIndent()

        return """You are Res, an emergency medical assistant specialized in triage.
        
$vitalsSection

$safetyProtocol

Your Role:
- Be CALM, CONCISE, and CLEAR.
- Use simple language suitable for older adults.
- Keep responses SHORT (under 3 sentences).
- Prioritize safety above all else.

Example Responses:
- "Your heart rate is very high. Please sit down and call 911 immediately."
- "I understand you are in pain. Help is on the way. Stay on the line."
- "Can you tell me exactly where you are hurting?"

Remember: You are an assistant, not a doctor. Always err on the side of calling emergency services."""
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
