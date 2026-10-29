package com.example.coinly.data.ai

import com.example.coinly.model.ChatMessage
import com.example.coinly.model.ChatSource

/** Only completed Gemini exchanges belong in the remote conversation. */
object GeminiConversation {
    fun previousTurns(prompt: String, history: List<ChatMessage>): List<ChatMessage> {
        val previous = if (history.lastOrNull()?.let { it.isUser && it.text == prompt } == true)
            history.dropLast(1) else history
        val pairs = mutableListOf<List<ChatMessage>>()
        var user: ChatMessage? = null
        for (message in previous) {
            if (message.isUser) user = message
            else {
                if (message.source == ChatSource.GEMINI && user != null) pairs.add(listOf(user, message))
                user = null
            }
        }
        return pairs.takeLast(4).flatten()
    }
}
