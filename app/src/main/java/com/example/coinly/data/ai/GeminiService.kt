package com.example.coinly.data.ai
import com.example.coinly.S
import com.example.coinly.R

import com.example.coinly.BuildConfig
import com.example.coinly.model.ChatMessage
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GeminiException(message: String, cause: Throwable? = null) : Exception(message, cause)

class GeminiService {

    fun generateContent(
        userPrompt: String,
        systemContext: String,
        history: List<ChatMessage>
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "your_gemini_api_key_here") {
            throw GeminiException(S.text(R.string.text_181))
        }

        val endpoints = listOf(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=$apiKey"
        )

        var lastException: Exception? = null

        for (endpointUrl in endpoints) {
            try {
                return executeApiCall(endpointUrl, userPrompt, systemContext, history)
            } catch (e: Exception) {
                lastException = e
            }
        }

        throw lastException ?: GeminiException(S.text(R.string.text_182))
    }

    private fun executeApiCall(
        endpointUrl: String,
        userPrompt: String,
        systemContext: String,
        history: List<ChatMessage>
    ): String {
        val url = URL(endpointUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.connectTimeout = 10000
        conn.readTimeout = 12000
        conn.doOutput = true

        val requestJson = JSONObject()

        // Instrucción de sistema con contexto financiero
        if (systemContext.isNotBlank()) {
            val systemInstruction = JSONObject()
            val systemParts = JSONArray()
            systemParts.put(JSONObject().put("text", systemContext))
            systemInstruction.put("parts", systemParts)
            requestJson.put("systemInstruction", systemInstruction)
        }

        // Historial corto + mensaje actual
        val contentsArray = JSONArray()

        // Incluimos hasta los últimos 6 mensajes del historial
        val recentHistory = history.takeLast(6)
        for (msg in recentHistory) {
            val role = if (msg.isUser) "user" else "model"
            val turnObj = JSONObject()
            turnObj.put("role", role)
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", msg.text))
            turnObj.put("parts", partsArray)
            contentsArray.put(turnObj)
        }

        // Mensaje actual si no estaba ya en el historial
        if (recentHistory.none { it.text == userPrompt && it.isUser }) {
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()
            currentParts.put(JSONObject().put("text", userPrompt))
            currentTurn.put("parts", currentParts)
            contentsArray.put(currentTurn)
        }

        requestJson.put("contents", contentsArray)

        // Enviar payload
        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(requestJson.toString())
            writer.flush()
        }

        val statusCode = conn.responseCode
        if (statusCode != HttpURLConnection.HTTP_OK) {
            val errorStream = conn.errorStream
            val errorMsg = if (errorStream != null) {
                BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
            } else S.text(R.string.text_183, statusCode)
            throw GeminiException(S.text(R.string.text_184, statusCode, errorMsg))
        }

        val responseText = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
        val responseJson = JSONObject(responseText)

        val candidates = responseJson.optJSONArray("candidates")
            ?: throw GeminiException(S.text(R.string.text_185))

        if (candidates.length() == 0) {
            throw GeminiException(S.text(R.string.text_186))
        }

        val candidate = candidates.getJSONObject(0)
        val content = candidate.optJSONObject("content")
            ?: throw GeminiException(S.text(R.string.text_187))

        val parts = content.optJSONArray("parts")
            ?: throw GeminiException(S.text(R.string.text_188))

        val textBuilder = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            textBuilder.append(part.optString("text", ""))
        }

        val result = textBuilder.toString().trim()
        if (result.isBlank()) {
            throw GeminiException(S.text(R.string.text_189))
        }

        return result
    }
}
