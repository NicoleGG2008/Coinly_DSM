package com.example.coinly.model

enum class ChatSource {
    USER,
    ASSISTANT,
    GEMINI,
    RULE_BASED
}

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString() + "_" + (0..999).random(),
    val text: String,
    val isUser: Boolean,
    val timestamp: String = "",
    val noticeRes: Int? = null,
    val source: ChatSource = if (isUser) ChatSource.USER else ChatSource.GEMINI
)
