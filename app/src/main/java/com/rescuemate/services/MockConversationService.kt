package com.rescuemate.services

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay

/**
 * Mock Conversation Service for Emulator Demo
 * 
 * Simulates ElevenLabs Conversational AI behavior with pre-scripted responses
 * for reliable demo on Android emulators where audio I/O may not work properly.
 * 
 * Usage: Toggle demo mode in WellnessAIConversationScreen
 */
class MockConversationService(private val context: Context) {
    
    companion object {
        private const val TAG = "MockConversation"
        private const val RESPONSE_DELAY_MS = 1500L  // Simulate AI thinking time
        private const val TYPING_DELAY_MS = 2000L    // Simulate voice generation time
    }
    
    @Volatile
    private var isActive = false
    
    private var conversationHistory = mutableListOf<Pair<String, String>>() // (role, message)
    
    /**
     * Pre-scripted responses for wellness conversation
     * Maps keywords to appropriate responses
     */
    private val responseDatabase = mapOf(
        // Greetings
        "hello" to listOf(
            "Hello! I'm here to support you. How are you feeling right now?",
            "Hi there! It's good to connect with you. What's on your mind today?",
            "Hello! I'm your wellness companion. How can I help you today?"
        ),
        "hi" to listOf(
            "Hi! I'm here to listen and support you. How are you doing?",
            "Hello! What would you like to talk about today?"
        ),
        
        // Stress-related
        "stress" to listOf(
            "I understand stress can be overwhelming. Would you like to talk about what's causing you stress?",
            "Stress is a natural response, but it's important to address it. Tell me more about what you're experiencing.",
            "I hear that you're feeling stressed. Let's explore some ways to help you manage this feeling."
        ),
        "stressed" to listOf(
            "I can sense the stress in your message. Would you like to share what's been weighing on you?",
            "Feeling stressed is challenging. I'm here to help you work through it. What's been happening?"
        ),
        "overwhelm" to listOf(
            "Feeling overwhelmed is difficult. Let's break things down together. What's the most pressing concern right now?",
            "I understand feeling overwhelmed. Sometimes taking small steps helps. What feels most manageable to address first?"
        ),
        
        // Anxiety-related
        "anxiety" to listOf(
            "Anxiety can be challenging to manage. I'm here to help. Can you tell me more about what triggers your anxiety?",
            "I understand dealing with anxiety is tough. Would you like to try some calming techniques together?",
            "Thank you for sharing that. Anxiety affects many people. What situations make you feel most anxious?"
        ),
        "anxious" to listOf(
            "I hear that you're feeling anxious. That takes courage to share. What's making you feel this way?",
            "Anxiety can be exhausting. I'm here to support you. Would you like to talk about what's worrying you?"
        ),
        "worry" to listOf(
            "Worrying is natural, but when it becomes excessive, it can affect your wellbeing. What are you worried about?",
            "I understand you're dealing with worries. Sometimes talking about them helps. What's on your mind?"
        ),
        "panic" to listOf(
            "If you're experiencing a panic attack, remember to breathe slowly. I'm here with you. Can you tell me what you're feeling?",
            "Panic can feel very intense. Focus on your breathing - in through your nose, out through your mouth. I'm here to support you."
        ),
        
        // Depression/Sadness
        "sad" to listOf(
            "I'm sorry you're feeling sad. Your feelings are valid. Would you like to talk about what's making you feel this way?",
            "Sadness is a difficult emotion to carry. I'm here to listen without judgment. What's been happening?"
        ),
        "depress" to listOf(
            "Thank you for trusting me with this. Depression can feel isolating, but you're not alone. How long have you been feeling this way?",
            "I hear that you're dealing with depression. That's incredibly difficult. Have you been able to talk to a healthcare professional about this?"
        ),
        "lonely" to listOf(
            "Loneliness can be very painful. I'm glad you reached out. Would you like to talk about what's making you feel lonely?",
            "Feeling lonely is more common than you might think. I'm here to keep you company. What would help you feel more connected?"
        ),
        "alone" to listOf(
            "I understand feeling alone can be difficult. You've taken an important step by reaching out. I'm here to listen.",
            "Even when you feel alone, please know that support is available. I'm here with you now. What's going through your mind?"
        ),
        
        // Work/School
        "work" to listOf(
            "Work-related stress is very common. What specific aspects of work are causing you difficulty?",
            "I understand work can be demanding. Would you like to talk about strategies for managing work stress?",
            "Your work situation sounds challenging. Let's explore what might help make it more manageable."
        ),
        "job" to listOf(
            "Job stress can significantly impact your wellbeing. What's the most challenging part of your job right now?",
            "I hear your job is causing difficulties. Are there particular situations or people that make it harder?"
        ),
        "school" to listOf(
            "Academic pressure can be intense. What subjects or situations are causing you the most stress?",
            "School stress affects many students. How are you managing your workload? Do you need support with time management?"
        ),
        "exam" to listOf(
            "Exam stress is very common. Have you been able to maintain a study schedule? Let's talk about preparation strategies.",
            "I understand exams can create a lot of pressure. How are you feeling about your preparation?"
        ),
        
        // Relationships
        "relationship" to listOf(
            "Relationships can be complex and challenging. Would you like to share what's happening in your relationship?",
            "I'm here to listen about your relationship concerns. What's been difficult lately?"
        ),
        "family" to listOf(
            "Family dynamics can be complicated. What's been happening with your family that you'd like to discuss?",
            "I understand family relationships can be both supporting and challenging. Tell me more about your situation."
        ),
        "friend" to listOf(
            "Friendships are important for our wellbeing. What's going on with your friends?",
            "I hear you're dealing with friendship issues. Would you like to talk about what happened?"
        ),
        
        // Health/Physical
        "tired" to listOf(
            "Fatigue can affect both your body and mind. How have you been sleeping lately?",
            "Feeling tired all the time could have many causes. Have you been getting enough rest? How is your sleep quality?"
        ),
        "sleep" to listOf(
            "Sleep is crucial for your wellbeing. Tell me about your sleep patterns. When do you typically go to bed?",
            "Sleep issues can significantly impact your mood and energy. What's been affecting your sleep?"
        ),
        "pain" to listOf(
            "Physical pain can be distressing. If this is severe or persistent, please consult a healthcare professional. Can you describe the pain?",
            "I'm sorry you're experiencing pain. While I can offer emotional support, physical symptoms should be evaluated by a doctor. Have you sought medical attention?"
        ),
        
        // Coping/Help
        "help" to listOf(
            "I'm here to help you. What kind of support would be most helpful right now?",
            "Asking for help is a sign of strength. I'm glad you reached out. What specific area would you like help with?"
        ),
        "cope" to listOf(
            "Developing healthy coping strategies is important. What coping techniques have you tried before? What works best for you?",
            "I'm glad you're thinking about coping strategies. Let's explore some options together. Have you tried mindfulness or breathing exercises?"
        ),
        "breath" to listOf(
            "Breathing exercises can be very effective. Let's try one together: Breathe in slowly for 4 counts, hold for 4, then exhale for 6. How does that feel?",
            "Focused breathing is a powerful tool. Try the 4-7-8 technique: Inhale for 4, hold for 7, exhale for 8. This can help calm your nervous system."
        ),
        
        // Positive emotions
        "better" to listOf(
            "I'm glad to hear you're feeling better! What helped improve your mood?",
            "That's wonderful that you're feeling better. What positive changes have you noticed?"
        ),
        "good" to listOf(
            "I'm happy to hear things are going well! What's been good for you lately?",
            "That's great! It's important to acknowledge the positive moments. What's contributing to you feeling good?"
        ),
        "happy" to listOf(
            "I'm so glad you're feeling happy! What's bringing you joy right now?",
            "Happiness is wonderful to experience. Would you like to share what's making you happy?"
        ),
        
        // Gratitude/Ending
        "thank" to listOf(
            "You're very welcome! I'm here whenever you need support. Is there anything else you'd like to talk about?",
            "I'm glad I could help. Remember, I'm always here when you need someone to talk to. Take care of yourself!"
        ),
        "bye" to listOf(
            "Take care of yourself! Remember, I'm here whenever you need to talk. Stay well!",
            "Goodbye! I hope our conversation was helpful. Please reach out anytime you need support."
        ),
        
        // Default fallback responses
        "default" to listOf(
            "I'm listening. Please tell me more about how you're feeling.",
            "Thank you for sharing that with me. Can you elaborate on what you're experiencing?",
            "I hear you. Would you like to tell me more about that?",
            "That sounds challenging. How is this affecting you?",
            "I understand. What would help you feel better in this situation?",
            "I'm here to support you. What's most important for us to focus on right now?"
        )
    )
    
    /**
     * Start mock conversation session
     */
    fun startSession() {
        isActive = true
        conversationHistory.clear()
        Log.d(TAG, "Mock session started")
    }
    
    /**
     * End mock conversation session
     */
    fun endSession() {
        isActive = false
        conversationHistory.clear()
        Log.d(TAG, "Mock session ended")
    }
    
    /**
     * Simulate conversation with AI
     * Analyzes user input and returns appropriate pre-scripted response
     * 
     * @param userInput User's message text
     * @param onThinking Callback when AI starts "thinking" (before delay)
     * @param onResponse Callback with AI's response text
     */
    suspend fun simulateConversation(
        userInput: String,
        onThinking: () -> Unit = {},
        onResponse: (String) -> Unit
    ) {
        if (!isActive) {
            Log.w(TAG, "Attempted to send message to inactive session")
            return
        }
        
        // Add user message to history
        conversationHistory.add("user" to userInput)
        
        // Notify that AI is "thinking"
        onThinking()
        
        // Simulate processing delay (AI thinking)
        delay(RESPONSE_DELAY_MS)
        
        // Find best matching response
        val response = findBestResponse(userInput)
        
        // Add AI response to history
        conversationHistory.add("assistant" to response)
        
        Log.d(TAG, "User: ${userInput.take(50)}...")
        Log.d(TAG, "AI: ${response.take(50)}...")
        
        // Simulate additional delay for "speaking" the response
        delay(TYPING_DELAY_MS)
        
        // Return response
        onResponse(response)
    }
    
    /**
     * Find best matching response based on user input
     * Uses keyword matching with fallback to default responses
     */
    private fun findBestResponse(userInput: String): String {
        val normalizedInput = userInput.lowercase().trim()
        
        // Check for exact keyword matches
        for ((keyword, responses) in responseDatabase) {
            if (keyword != "default" && normalizedInput.contains(keyword)) {
                // Return random response from matched category
                return responses.random()
            }
        }
        
        // Check for multi-word patterns
        val patterns = mapOf(
            "how are you" to "I'm doing well, thank you for asking! More importantly, how are you doing?",
            "what is your name" to "I'm your AI wellness companion, here to provide support whenever you need it.",
            "who are you" to "I'm an AI designed to offer emotional support and be a caring listener. I'm here to help you work through challenges.",
            "can you help" to "Yes, I'm here to help! Tell me what's on your mind and I'll do my best to support you.",
            "i don't know" to "It's okay not to have all the answers. Sometimes just talking through things can bring clarity. What's confusing you?",
            "i'm fine" to "I'm glad to hear that. Remember, it's okay to not be fine sometimes too. I'm here if you need to talk about anything.",
            "nothing" to "Sometimes it's hard to put feelings into words. Is there anything, even something small, that's been on your mind?"
        )
        
        for ((pattern, response) in patterns) {
            if (normalizedInput.contains(pattern)) {
                return response
            }
        }
        
        // Context-aware responses based on conversation history
        if (conversationHistory.size > 2) {
            val lastAIResponse = conversationHistory.findLast { it.first == "assistant" }?.second ?: ""
            
            // If last response was asking a question and user gave short answer
            if (lastAIResponse.contains("?") && userInput.split(" ").size < 3) {
                return "Thank you for sharing. Can you tell me a bit more about that?"
            }
        }
        
        // Fallback to default responses
        return responseDatabase["default"]?.random() 
            ?: "I'm here to listen and support you. Please continue."
    }
    
    /**
     * Check if session is active
     */
    fun isSessionActive(): Boolean = isActive
    
    /**
     * Get conversation history
     */
    fun getHistory(): List<Pair<String, String>> = conversationHistory.toList()
    
    /**
     * Get session state for debugging
     */
    fun getSessionState(): String {
        return if (isActive) "ACTIVE" else "IDLE"
    }
}

