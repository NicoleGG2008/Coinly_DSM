package com.example.coinly.model

enum class ChatSource {
    USER,
    GEMINI,
    RULE_BASED
}

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString() + "_" + (0..999).random(),
    val text: String,
    val isUser: Boolean,
    val timestamp: String = "",
    val source: ChatSource = if (isUser) ChatSource.USER else ChatSource.GEMINI
)
