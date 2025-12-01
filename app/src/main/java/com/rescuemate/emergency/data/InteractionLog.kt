package com.rescuemate.emergency.data

data class InteractionLog(
    val id: String,
    val userId: String,
    val timestamp: Long,
    val summary: String,
    val transcript: String,
    val type: String // "ONLINE" or "OFFLINE"
)
