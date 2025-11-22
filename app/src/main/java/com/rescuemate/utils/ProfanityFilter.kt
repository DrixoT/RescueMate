package com.rescuemate.utils

import java.util.Locale

/**
 * Profanity Filter Utility
 * Detects and filters profane or inappropriate content from user input
 */
object ProfanityFilter {
    
    // List of common profane words (keeping it minimal and PG-13)
    // In production, this would be a more comprehensive list or use an external API
    private val profanityList = setOf(
        // Common profanity (abbreviated to be less explicit)
        "fuck", "fck", "f*ck", "fuk",
        "shit", "sh*t", "sht",
        "bitch", "b*tch", "btch",
        "ass", "a**", "arse",
        "damn", "dmn",
        "hell", "hll",
        "bastard", "bstrd",
        "crap", "cr*p",
        "piss", "p*ss",
        "dick", "d*ck",
        "cock", "c*ck",
        "pussy", "p*ssy",
        "slut", "sl*t",
        "whore", "wh*re",
        "fag", "f*g",
        "nigger", "n*gger", "n***a",
        "retard", "ret*rd",
        // Add more as needed
    )
    
    /**
     * Check if text contains profanity
     */
    fun containsProfanity(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        
        val lowerText = text.lowercase(Locale.getDefault())
        
        // Check for exact matches
        for (word in profanityList) {
            // Check if word appears as standalone or with word boundaries
            if (lowerText.contains(Regex("\\b$word\\b")) || 
                lowerText.contains(word)) {
                return true
            }
        }
        
        // Check for leetspeak variations (1 for i, 3 for e, 0 for o, etc.)
        val normalized = normalizeLeetSpeak(lowerText)
        for (word in profanityList) {
            if (normalized.contains(Regex("\\b$word\\b")) || 
                normalized.contains(word)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Validate text for profanity and return validation result
     */
    fun validateText(text: String?, fieldName: String = "Text"): ValidationUtils.ValidationResult {
        return if (containsProfanity(text)) {
            ValidationUtils.ValidationResult.error("$fieldName contains inappropriate language")
        } else {
            ValidationUtils.ValidationResult.success()
        }
    }
    
    /**
     * Filter/censor profanity from text by replacing with asterisks
     */
    fun filterProfanity(text: String?): String {
        if (text.isNullOrBlank()) return ""
        
        var filtered: String = text
        val lowerText = text.lowercase(Locale.getDefault())
        
        for (word in profanityList) {
            val regex = Regex("\\b$word\\b", RegexOption.IGNORE_CASE)
            filtered = regex.replace(filtered) { matchResult ->
                "*".repeat(matchResult.value.length)
            }
        }
        
        return filtered
    }
    
    /**
     * Normalize leetspeak to regular characters
     */
    private fun normalizeLeetSpeak(text: String): String {
        return text
            .replace("1", "i")
            .replace("3", "e")
            .replace("0", "o")
            .replace("4", "a")
            .replace("5", "s")
            .replace("7", "t")
            .replace("8", "b")
            .replace("@", "a")
            .replace("$", "s")
    }
    
    /**
     * Check if name is appropriate
     */
    fun isNameAppropriate(name: String?): Boolean {
        return !containsProfanity(name)
    }
    
    /**
     * Get sanitized/clean version of text
     */
    fun getSanitizedText(text: String?): String {
        return filterProfanity(text)
    }
}

