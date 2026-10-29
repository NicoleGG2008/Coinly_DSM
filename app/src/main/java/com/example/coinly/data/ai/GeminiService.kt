package com.example.coinly.data.ai

import com.example.coinly.BuildConfig
import com.example.coinly.R
import com.example.coinly.model.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.*
import kotlinx.coroutines.withTimeout
import java.io.IOException

class GeminiService {
    suspend fun generateContent(prompt: String, context: String, history: List<ChatMessage>): String {
        check(!BuildConfig.FIREBASE_EMULATOR) { "AI is disabled in local Firebase tests" }
        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = BuildConfig.GEMINI_MODEL,
            systemInstruction = content { text(context) },
            generationConfig = generationConfig {
                temperature = 0.5f
                maxOutputTokens = 2048
            }
        )
        val previous = GeminiConversation.previousTurns(prompt, history).map { message ->
            content(role = if (message.isUser) "user" else "model") { text(message.text) }
        }
        val answer = withTimeout(45_000) { model.startChat(previous).sendMessage(prompt).text }
        return answer?.trim()?.takeIf { it.isNotEmpty() } ?: throw EmptyGeminiResponse()
    }
}

private class EmptyGeminiResponse : Exception()

/** User-facing messages never include server payloads, tokens or financial context. */
object GeminiErrors {
    fun message(error: Throwable): Int {
        val causes = generateSequence(error) { it.cause }.take(8).toList()
        return when {
            causes.any { it is APINotConfiguredException || it is ServiceDisabledException } -> R.string.ai_error_setup
            causes.any { it is InvalidAPIKeyException || it is PermissionMissingException } -> R.string.ai_error_access
            causes.any { it is QuotaExceededException } -> R.string.ai_error_quota
            causes.any { it is PromptBlockedException || it is ContentBlockedException || it is ResponseStoppedException } -> R.string.ai_error_blocked
            causes.any { it is UnsupportedUserLocationException } -> R.string.ai_error_region
            causes.any { it is RequestTimeoutException || it is kotlinx.coroutines.TimeoutCancellationException || it is IOException } -> R.string.ai_error_network
            causes.any { it is EmptyGeminiResponse } -> R.string.ai_error_empty
            else -> R.string.ai_error_unavailable
        }
    }
}
