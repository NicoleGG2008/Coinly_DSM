package com.example.coinly.data.ai

import com.example.coinly.model.ChatMessage
import com.example.coinly.model.ChatSource
import org.junit.Assert.*
import org.junit.Test

class GeminiConversationTest {
    private fun user(text: String) = ChatMessage(text = text, isUser = true)
    private fun model(text: String) = ChatMessage(text = text, isUser = false, source = ChatSource.GEMINI)

    @Test fun repeatedQuestionKeepsTheEarlierExchangeAndOnlyRemovesPendingTurn() {
        val first = user("¿Cuánto gasté?")
        val reply = model("Gastaste 10 dólares.")
        val turns = GeminiConversation.previousTurns(first.text, listOf(first, reply, user(first.text)))
        assertEquals(listOf(first, reply), turns)
    }

    @Test fun localRepliesWelcomeAndUnansweredMessagesDoNotBecomeGeminiHistory() {
        val welcome = ChatMessage(text = "Hola", isUser = false, source = ChatSource.ASSISTANT)
        val local = ChatMessage(text = "Consejo local", isUser = false, source = ChatSource.RULE_BASED)
        val question = user("Presupuesto")
        val answer = model("Respuesta remota")
        val history = listOf(welcome, user("Sin red"), local, user("Abandonada"), question, answer, user("Nueva"))
        assertEquals(listOf(question, answer), GeminiConversation.previousTurns("Nueva", history))
    }

    @Test fun historyIsBoundedToFourCompleteExchanges() {
        val history = (1..10).flatMap { listOf(user("Pregunta $it"), model("Respuesta $it")) }
        assertEquals(history.takeLast(8), GeminiConversation.previousTurns("Actual", history))
    }

    @Test fun currentQuestionIsKeptEvenWhenItAppearedEarlier() {
        val history = listOf(user("Otra"), model("Respuesta"))
        assertEquals(history, GeminiConversation.previousTurns("Otra", history))
    }
}
