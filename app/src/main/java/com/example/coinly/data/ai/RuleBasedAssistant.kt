package com.example.coinly.data.ai
import com.example.coinly.S
import com.example.coinly.R

import android.content.Context
import com.example.coinly.data.BudgetRepository
import com.example.coinly.data.MovementRepository.MovementRepository
import com.example.coinly.model.Budget
import com.example.coinly.model.BudgetStatus
import com.example.coinly.model.Movement
import com.example.coinly.ui.Ui

class RuleBasedAssistant(private val context: Context) {

    private val movementRepository by lazy { MovementRepository(context) }
    private val budgetRepository by lazy { BudgetRepository(context) }

    fun generateResponse(userPrompt: String): String {
        val query = userPrompt.lowercase().trim()
        val movements = movementRepository.getMovements()
        val budgets = budgetRepository.getBudgets()

        // Caso límite: Usuario sin datos
        if (movements.isEmpty() && budgets.isEmpty()) {
            return S.text(R.string.text_190) +
                S.text(R.string.text_191) +
                S.text(R.string.text_192) +
                S.text(R.string.text_193)
        }

        val incomeMovements = movements.filter { it.isIncome }
        val expenseMovements = movements.filter { !it.isIncome }

        val totalIncome = incomeMovements.sumOf { it.amount }
        val totalExpense = expenseMovements.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        val expensesByCategory = expenseMovements
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        val topCategory = expensesByCategory.firstOrNull()

        // Determinar categoría por intención de búsqueda
        return when {
            isSavingsQuery(query) -> buildSavingsResponse(totalIncome, totalExpense, balance, topCategory)
            isBudgetQuery(query) -> buildBudgetResponse(budgets)
            isExpenseQuery(query) -> buildExpenseResponse(query, totalExpense, expensesByCategory, budgets)
            else -> buildGeneralResponse(totalIncome, totalExpense, balance, topCategory, budgets)
        }
    }

    private fun isSavingsQuery(q: String) =
        q.contains("ahorr") || q.contains("guardar") || q.contains("meta") || q.contains("fondo") || q.contains("inver") || q.contains(S.text(R.string.text_194))

    private fun isBudgetQuery(q: String) =
        q.contains("presupuesto") || q.contains("limite") || q.contains(S.text(R.string.text_195)) || q.contains("alerta") || q.contains("exced") || q.contains("disponible")

    private fun isExpenseQuery(q: String) =
        q.contains("gasto") || q.contains("compr") || q.contains("pag") || q.contains(S.text(R.string.text_196)) || q.contains("salida") || q.contains("comida") || q.contains(S.text(R.string.text_197)) || q.contains(S.text(R.string.text_198))

    private fun buildSavingsResponse(
        income: Double,
        expense: Double,
        balance: Double,
        topCategory: Pair<String, Double>?
    ): String {
        val recommendedSavings = if (income > 0) income * 0.20 else 0.0
        val sb = StringBuilder()

        sb.append(S.text(R.string.text_199))

        if (balance > 0) {
            val savingsRate = if (income > 0) (balance / income * 100).toInt() else 0
            sb.append(S.text(R.string.text_200, Ui.money(balance)))
            sb.append(S.text(R.string.text_201, savingsRate))
            if (income > 0) {
                sb.append(S.text(R.string.text_202, Ui.money(income), Ui.money(recommendedSavings)))
            }
            sb.append(S.text(R.string.text_203, Ui.money(balance * 0.5)))
        } else if (balance == 0.0 && income > 0) {
            sb.append(S.text(R.string.text_204, Ui.money(income)))
            sb.append(S.text(R.string.text_205))
        } else {
            sb.append(S.text(R.string.text_206, Ui.money(expense), Ui.money(income)))
            sb.append(S.text(R.string.text_207, Ui.money(-balance)))
            if (topCategory != null) {
                sb.append(S.text(R.string.text_208, topCategory.first, Ui.money(topCategory.second)))
                sb.append(S.text(R.string.text_209, Ui.money(topCategory.second * 0.15)))
            }
        }
        return sb.toString()
    }

    private fun buildBudgetResponse(budgets: List<Budget>): String {
        if (budgets.isEmpty()) {
            return S.text(R.string.text_210) +
                S.text(R.string.text_211)
        }

        val exceeded = budgets.filter { it.status == BudgetStatus.DANGER }
        val warning = budgets.filter { it.status == BudgetStatus.WARNING }
        val success = budgets.filter { it.status == BudgetStatus.SUCCESS }

        val sb = StringBuilder(S.text(R.string.text_212))

        if (exceeded.isNotEmpty()) {
            sb.append(S.text(R.string.text_213, exceeded.size))
            exceeded.forEach { b ->
                val over = b.spent - b.limit
                sb.append(S.text(R.string.text_214, b.category, Ui.money(b.spent), Ui.money(b.limit), Ui.money(over)))
            }
            sb.append("\n")
        }

        if (warning.isNotEmpty()) {
            sb.append(S.text(R.string.text_215, warning.size))
            warning.forEach { b ->
                sb.append(S.text(R.string.text_216, b.category, Ui.money(b.spent), Ui.money(b.limit), b.percent, Ui.money(b.available)))
            }
            sb.append("\n")
        }

        if (success.isNotEmpty()) {
            sb.append(S.text(R.string.text_217, success.size))
            success.forEach { b ->
                sb.append(S.text(R.string.text_218, b.category, Ui.money(b.spent), Ui.money(b.limit), b.percent))
            }
            sb.append("\n")
        }

        if (exceeded.isNotEmpty()) {
            sb.append(S.text(R.string.text_219))
        } else {
            sb.append(S.text(R.string.text_220))
        }

        return sb.toString()
    }

    private fun buildExpenseResponse(
        query: String,
        totalExpense: Double,
        expensesByCategory: List<Pair<String, Double>>,
        budgets: List<Budget>
    ): String {
        val sb = StringBuilder()

        // Caso de consulta de gasto específico (ej: "¿puedo gastar 50 en ocio?")
        val matchAmount = Regex("(\\d+([.,]\\d+)?)").find(query)?.value?.replace(",", ".")?.toDoubleOrNull()
        if (matchAmount != null && matchAmount > 0) {
            val matchingBudget = budgets.firstOrNull { query.contains(it.category.lowercase()) }
            sb.append(S.text(R.string.text_221, Ui.money(matchAmount)))

            if (matchingBudget != null) {
                val available = matchingBudget.available
                if (available >= matchAmount) {
                    sb.append(S.text(R.string.text_222, matchingBudget.category, Ui.money(available)))
                    sb.append(S.text(R.string.text_223, Ui.money(available - matchAmount)))
                } else if (available > 0) {
                    sb.append(S.text(R.string.text_224, matchingBudget.category, Ui.money(available)))
                    sb.append(S.text(R.string.text_225, Ui.money(matchAmount), Ui.money(matchAmount - available)))
                } else {
                    sb.append(S.text(R.string.text_226, matchingBudget.category))
                    sb.append(S.text(R.string.text_227, Ui.money(-available)))
                }
            } else {
                sb.append(S.text(R.string.text_228, Ui.money(totalExpense)))
                sb.append(S.text(R.string.text_229, Ui.money(matchAmount)))
            }
            return sb.toString()
        }

        // Resumen general de gastos por categoría
        sb.append(S.text(R.string.text_230))
        sb.append(S.text(R.string.text_231, Ui.money(totalExpense)))

        if (expensesByCategory.isNotEmpty()) {
            sb.append(S.text(R.string.text_232))
            expensesByCategory.take(4).forEach { (cat, amount) ->
                val pct = if (totalExpense > 0) (amount / totalExpense * 100).toInt() else 0
                sb.append(S.text(R.string.text_233, cat, Ui.money(amount), pct))
            }
            sb.append("\n")
            val top = expensesByCategory.first()
            sb.append(S.text(R.string.text_234, top.first))
            sb.append(S.text(R.string.text_235, (top.second / totalExpense * 100).toInt()))
        } else {
            sb.append(S.text(R.string.text_236))
        }

        return sb.toString()
    }

    private fun buildGeneralResponse(
        income: Double,
        expense: Double,
        balance: Double,
        topCategory: Pair<String, Double>?,
        budgets: List<Budget>
    ): String {
        val exceededCount = budgets.count { it.status == BudgetStatus.DANGER }
        val warningCount = budgets.count { it.status == BudgetStatus.WARNING }

        val sb = StringBuilder(S.text(R.string.text_237))
        sb.append(S.text(R.string.text_238, if (balance >= 0) "+" else "", Ui.money(balance)))
        sb.append(S.text(R.string.text_239, Ui.money(income)))
        sb.append(S.text(R.string.text_240, Ui.money(expense)))

        if (topCategory != null) {
            sb.append(S.text(R.string.text_241, topCategory.first, Ui.money(topCategory.second)))
        }

        if (budgets.isNotEmpty()) {
            sb.append(S.text(R.string.text_242, budgets.size))
            if (exceededCount > 0 || warningCount > 0) {
                sb.append(S.text(R.string.text_243, exceededCount, warningCount))
            } else {
                sb.append(S.text(R.string.text_244))
            }
            sb.append("\n")
        }

        sb.append(S.text(R.string.text_245))
        return sb.toString()
    }
}
