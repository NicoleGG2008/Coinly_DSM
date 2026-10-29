package com.example.coinly

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.coinly.data.ai.GeminiErrors
import com.example.coinly.data.ai.GeminiService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in remote check. Sends no profile, movements, budgets or chat history. */
@RunWith(AndroidJUnit4::class)
class GeminiConnectionSmokeTest {
    @Test fun receivesARealGeminiResponse() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("allowRemoteAi") == "true")
        assumeTrue(!BuildConfig.FIREBASE_EMULATOR)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        try {
            val answer = GeminiService().generateContent(
                "Responde únicamente: CONEXION_OK",
                "Esta es una prueba técnica de conexión. Responde con el texto solicitado.",
                emptyList()
            )
            assertTrue("Gemini returned text", answer.isNotBlank())
        } catch (error: Exception) {
            // Never put SDK payloads or App Check tokens into test reports.
            val causes = generateSequence<Throwable>(error) { it.cause }.take(8).toList()
            val types = causes.joinToString(",") { it.javaClass.simpleName }
            val markers = listOf("SERVICE_DISABLED", "PERMISSION_DENIED", "API_KEY_INVALID",
                "API_KEY_SERVICE_BLOCKED", "GENAI_CONFIG_NOT_FOUND", "RESOURCE_EXHAUSTED",
                "NOT_FOUND", "403", "404", "App Check").filter { marker ->
                causes.any { it.message?.contains(marker, ignoreCase = true) == true }
            }
            throw AssertionError(context.getString(GeminiErrors.message(error)) + " [types=$types; markers=$markers]")
        }
    }
}
