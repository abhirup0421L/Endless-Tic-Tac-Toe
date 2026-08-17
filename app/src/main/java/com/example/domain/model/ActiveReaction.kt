package com.example.domain.model

data class ActiveReaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val emoji: String,
    val senderName: String,
    val isLocal: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
