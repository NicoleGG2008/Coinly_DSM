package com.example.coinly.data.ai
import com.example.coinly.S
import com.example.coinly.R

import android.content.Context
import com.example.coinly.data.BudgetRepository
import com.example.coinly.data.MovementRepository.MovementRepository
import com.example.coinly.data.UserPrefs
import com.example.coinly.model.ChatMessage
import com.example.coinly.model.ChatSource
import com.example.coinly.ui.Ui
import kotlinx.coroutines.*

class CoinlyAiAssistant(private val context: Context) {

    private val geminiService = GeminiService()
    private val ruleBasedAssistant = RuleBasedAssistant(context)
    private val movementRepository by lazy { MovementRepository(context) }
    private val budgetRepository by lazy { BudgetRepository(context) }
    private val userPrefs by lazy { UserPrefs(context) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun close() { scope.cancel() }

    fun ask(userPrompt: String, history: List<ChatMessage>, onResult: (ChatMessage) -> Unit) {
        val systemContext = buildFinancialContext()
        scope.launch {
            val response = try {
                val answer = geminiService.generateContent(userPrompt, systemContext, history)
                ChatMessage(text = answer, isUser = false, source = ChatSource.GEMINI)
            } catch (error: Exception) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                ChatMessage(
                    text = ruleBasedAssistant.generateResponse(userPrompt),
                    isUser = false,
                    source = ChatSource.RULE_BASED,
                    noticeRes = GeminiErrors.message(error)
                )
            }
            onResult(response)
        }
    }

    private fun buildFinancialContext(): String {
        val name = userPrefs.firstName

        val movements = movementRepository.getMovements()
        val budgets = budgetRepository.getBudgets()

        val income = movements.filter { it.isIncome }.sumOf { it.amount }
        val expense = movements.filter { !it.isIncome }.sumOf { it.amount }
        val balance = income - expense

        val expenseByCat = movements.filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { m -> m.amount } }

        val sb = StringBuilder()
        sb.append(S.text(R.string.text_165))
        sb.append(S.text(R.string.text_166))

        sb.append(S.text(R.string.text_167))
        sb.append(S.text(R.string.ai_context_user, name))
        sb.append(S.text(R.string.text_169, Ui.money(balance)))
        sb.append(S.text(R.string.text_170, Ui.money(income)))
        sb.append(S.text(R.string.text_171, Ui.money(expense)))

        if (expenseByCat.isNotEmpty()) {
            sb.append(S.text(R.string.text_172))
            expenseByCat.forEach { (cat, amt) ->
                sb.append("  - $cat: ${Ui.money(amt)}\n")
            }
            sb.append("\n")
        } else {
            sb.append(S.text(R.string.text_173))
        }

        if (budgets.isNotEmpty()) {
            sb.append(S.text(R.string.text_174))
            budgets.forEach { b ->
                val statusText = when (b.status) {
                    com.example.coinly.model.BudgetStatus.DANGER -> S.text(R.string.text_175, Ui.money(b.spent - b.limit))
                    com.example.coinly.model.BudgetStatus.WARNING -> S.text(R.string.text_176, b.percent)
                    com.example.coinly.model.BudgetStatus.SUCCESS -> S.text(R.string.text_177, b.percent)
                }
                sb.append(S.text(R.string.text_178, b.category, Ui.money(b.limit), Ui.money(b.spent), statusText))
            }
        } else {
            sb.append(S.text(R.string.text_179))
        }

        sb.append(S.text(R.string.text_180))
        return sb.toString()
    }
}
