package com.example.coinly.data.ai

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.coinly.data.BudgetRepository
import com.example.coinly.data.MovementRepository.MovementRepository
import com.example.coinly.data.UserPrefs
import com.example.coinly.model.ChatMessage
import com.example.coinly.model.ChatSource
import com.example.coinly.ui.Ui
import java.util.concurrent.Executors

class CoinlyAiAssistant(private val context: Context) {

    private val geminiService = GeminiService()
    private val ruleBasedAssistant = RuleBasedAssistant(context)
    private val movementRepository by lazy { MovementRepository(context) }
    private val budgetRepository by lazy { BudgetRepository(context) }
    private val userPrefs by lazy { UserPrefs(context) }

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun ask(
        userPrompt: String,
        history: List<ChatMessage>,
        onResult: (ChatMessage) -> Unit
    ) {
        val systemContext = buildFinancialContext()

        executor.execute {
            var responseText: String? = null
            var source = ChatSource.GEMINI

            // 1. Intentar con la API real de Gemini
            try {
                responseText = geminiService.generateContent(userPrompt, systemContext, history)
            } catch (e: Exception) {
                // Si falla (sin internet, sin key o error de red), pasa al motor de reglas
                responseText = null
            }

            // 2. Si Gemini falló o no dio respuesta, usar el respaldo por reglas
            if (responseText.isNullOrBlank()) {
                responseText = ruleBasedAssistant.generateResponse(userPrompt)
                source = ChatSource.RULE_BASED
            }

            val resultMsg = ChatMessage(
                text = responseText,
                isUser = false,
                source = source
            )

            mainHandler.post {
                onResult(resultMsg)
            }
        }
    }

    private fun buildFinancialContext(): String {
        val name = userPrefs.name.ifBlank { "Usuario" }
        val career = userPrefs.career.ifBlank { "Estudiante" }
        val university = userPrefs.university.ifBlank { "Universidad" }

        val movements = movementRepository.getMovements()
        val budgets = budgetRepository.getBudgets()

        val income = movements.filter { it.isIncome }.sumOf { it.amount }
        val expense = movements.filter { !it.isIncome }.sumOf { it.amount }
        val balance = income - expense

        val expenseByCat = movements.filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { m -> m.amount } }

        val sb = StringBuilder()
        sb.append("Eres Coinly AI, el asistente virtual inteligente de finanzas personales dentro de la app Coinly. ")
        sb.append("Responde siempre en español, de forma amable, empática, clara y muy concisa (máximo 2 a 3 párrafos o puntos clave).\n\n")

        sb.append("DATOS FINANCIEROS REALES DEL USUARIO DENTRO DE COINLY:\n")
        sb.append("• Nombre: $name ($career en $university)\n")
        sb.append("• Balance Neto Actual: ${Ui.money(balance)}\n")
        sb.append("• Ingresos Totales: +${Ui.money(income)}\n")
        sb.append("• Gastos Totales: −${Ui.money(expense)}\n\n")

        if (expenseByCat.isNotEmpty()) {
            sb.append("• Desglose de Gastos por Categoría:\n")
            expenseByCat.forEach { (cat, amt) ->
                sb.append("  - $cat: ${Ui.money(amt)}\n")
            }
            sb.append("\n")
        } else {
            sb.append("• No registra gastos en este período.\n\n")
        }

        if (budgets.isNotEmpty()) {
            sb.append("• Presupuestos Configurados:\n")
            budgets.forEach { b ->
                val statusText = when (b.status) {
                    com.example.coinly.model.BudgetStatus.DANGER -> "EXCEDIDO (${Ui.money(b.spent - b.limit)} sobre el límite)"
                    com.example.coinly.model.BudgetStatus.WARNING -> "CERCA DEL LÍMITE (${b.percent}% usado)"
                    com.example.coinly.model.BudgetStatus.SUCCESS -> "EN RANGO (${b.percent}% usado)"
                }
                sb.append("  - ${b.category}: Límite ${Ui.money(b.limit)} | Gastado ${Ui.money(b.spent)} | Estado: $statusText\n")
            }
        } else {
            sb.append("• No tiene presupuestos creados aún.\n")
        }

        sb.append("\nInstrucciones adicionales: Da consejos personalizados usando exactamente los valores numéricos anteriores cuando corresponda. Si te preguntan si pueden gastar x cantidad en x categoría, evalúa con sus presupuestos y saldo disponible.")
        return sb.toString()
    }
}
