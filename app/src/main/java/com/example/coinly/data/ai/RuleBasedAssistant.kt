package com.example.coinly.data.ai

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
            return "¡Hola! 👋 Aún no tienes movimientos ni presupuestos registrados en Coinly.\n\n" +
                " Para ayudarte mejor, empieza por registrar tu primer ingreso o gasto desde la pantalla de Inicio o Movimientos (+). " +
                "También puedes configurar límites en la sección de Presupuestos.\n\n" +
                "💡 **Consejo rápido:** Aplica la regla 50/30/20: 50% para necesidades básicas, 30% para gustos personales y 20% directo a tu ahorro."
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
        q.contains("ahorr") || q.contains("guardar") || q.contains("meta") || q.contains("fondo") || q.contains("inver") || q.contains("cuanto guardar")

    private fun isBudgetQuery(q: String) =
        q.contains("presupuesto") || q.contains("limite") || q.contains("límite") || q.contains("alerta") || q.contains("exced") || q.contains("disponible")

    private fun isExpenseQuery(q: String) =
        q.contains("gasto") || q.contains("compr") || q.contains("pag") || q.contains("puedo gastar") || q.contains("salida") || q.contains("comida") || q.contains("en que") || q.contains("en qué")

    private fun buildSavingsResponse(
        income: Double,
        expense: Double,
        balance: Double,
        topCategory: Pair<String, Double>?
    ): String {
        val recommendedSavings = if (income > 0) income * 0.20 else 0.0
        val sb = StringBuilder()

        sb.append("💡 **Análisis de Ahorro Coinly:**\n\n")

        if (balance > 0) {
            val savingsRate = if (income > 0) (balance / income * 100).toInt() else 0
            sb.append("¡Vas por excelente camino! Tu balance actual es positivo: **+${Ui.money(balance)}** ")
            sb.append("(tienes un margen del **$savingsRate%** de tus ingresos).\n\n")
            if (income > 0) {
                sb.append("• **Recomendación 50/30/20:** Para tus ingresos de ${Ui.money(income)}, se recomienda ahorrar un 20% (**${Ui.money(recommendedSavings)}**).\n")
            }
            sb.append("• Si separas **${Ui.money(balance * 0.5)}** hoy mismo a tu fondo de ahorro, mantendrás tus finanzas muy estables.")
        } else if (balance == 0.0 && income > 0) {
            sb.append("Tus gastos igualan exactamente tus ingresos (${Ui.money(income)}). ")
            sb.append("Para lograr ahorrar, necesitas reducir gastos no esenciales o generar ingresos adicionales.")
        } else {
            sb.append("⚠️ **Atención:** Actualmente tus gastos (${Ui.money(expense)}) superan tus ingresos (${Ui.money(income)}), ")
            sb.append("generando un déficit de **${Ui.money(-balance)}**.\n\n")
            if (topCategory != null) {
                sb.append("• Tu principal área de gasto es **${topCategory.first}** con **${Ui.money(topCategory.second)}**. ")
                sb.append("Reducir un 15% en este rubro liberaría **${Ui.money(topCategory.second * 0.15)}** para estabilizar tu balance.")
            }
        }
        return sb.toString()
    }

    private fun buildBudgetResponse(budgets: List<Budget>): String {
        if (budgets.isEmpty()) {
            return "📋 **Presupuestos:**\n\nNo tienes presupuestos configurados en este momento. " +
                "Puedes crear metas mensuales en la pestaña **Presupuestos** para evitar gastos excesivos."
        }

        val exceeded = budgets.filter { it.status == BudgetStatus.DANGER }
        val warning = budgets.filter { it.status == BudgetStatus.WARNING }
        val success = budgets.filter { it.status == BudgetStatus.SUCCESS }

        val sb = StringBuilder("📋 **Estado de tus Presupuestos:**\n\n")

        if (exceeded.isNotEmpty()) {
            sb.append("🚨 **Presupuestos excedidos (${exceeded.size}):**\n")
            exceeded.forEach { b ->
                val over = b.spent - b.limit
                sb.append("• **${b.category}**: gastaste ${Ui.money(b.spent)} de un límite de ${Ui.money(b.limit)} (exceso de ${Ui.money(over)}).\n")
            }
            sb.append("\n")
        }

        if (warning.isNotEmpty()) {
            sb.append("⚠️ **Cercanos al límite (${warning.size}):**\n")
            warning.forEach { b ->
                sb.append("• **${b.category}**: ${Ui.money(b.spent)} de ${Ui.money(b.limit)} (${b.percent}% usado, te quedan ${Ui.money(b.available)}).\n")
            }
            sb.append("\n")
        }

        if (success.isNotEmpty()) {
            sb.append("✅ **Presupuestos bajo control (${success.size}):**\n")
            success.forEach { b ->
                sb.append("• **${b.category}**: ${Ui.money(b.spent)} de ${Ui.money(b.limit)} (${b.percent}% usado).\n")
            }
            sb.append("\n")
        }

        if (exceeded.isNotEmpty()) {
            sb.append("💡 **Sugerencia:** Reasigna fondos de las categorías en verde para cubrir los excesos.")
        } else {
            sb.append("👍 ¡Excelente disciplina financiera! Todos tus presupuestos están dentro del límite planeado.")
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
            sb.append("💸 **Evaluación de Gasto (${Ui.money(matchAmount)}):**\n\n")

            if (matchingBudget != null) {
                val available = matchingBudget.available
                if (available >= matchAmount) {
                    sb.append("✅ **¡Sí es viable!** En la categoría **${matchingBudget.category}** tienes un disponible de **${Ui.money(available)}**. ")
                    sb.append("Después de este gasto te quedarán **${Ui.money(available - matchAmount)}**.")
                } else if (available > 0) {
                    sb.append("⚠️ **Atención:** En **${matchingBudget.category}** solo te quedan **${Ui.money(available)}**. ")
                    sb.append("Gastar ${Ui.money(matchAmount)} excedería tu presupuesto por **${Ui.money(matchAmount - available)}**.")
                } else {
                    sb.append("🚨 **Gasto no recomendado:** Ya has superado tu presupuesto de **${matchingBudget.category}** ")
                    sb.append("(excedido en ${Ui.money(-available)}). Realizar este gasto aumentará tu déficit.")
                }
            } else {
                sb.append("Has registrado un total de **${Ui.money(totalExpense)}** en gastos este mes. ")
                sb.append("Evalúa si este gasto de **${Ui.money(matchAmount)}** encaja en tus prioridades antes de realizarlo.")
            }
            return sb.toString()
        }

        // Resumen general de gastos por categoría
        sb.append("📊 **Desglose de Gastos Acumulados:**\n\n")
        sb.append("Has gastado un total de **${Ui.money(totalExpense)}**.\n\n")

        if (expensesByCategory.isNotEmpty()) {
            sb.append("**Tus principales categorías de gasto son:**\n")
            expensesByCategory.take(4).forEach { (cat, amount) ->
                val pct = if (totalExpense > 0) (amount / totalExpense * 100).toInt() else 0
                sb.append("• **$cat**: ${Ui.money(amount)} ($pct% del total)\n")
            }
            sb.append("\n")
            val top = expensesByCategory.first()
            sb.append("💡 **Tip de optimización:** Revisa tus gastos en **${top.first}**, ya que representan casi el ")
            sb.append("${(top.second / totalExpense * 100).toInt()}% de todas tus salidas.")
        } else {
            sb.append("No hay gastos registrados en este período.")
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

        val sb = StringBuilder("📊 **Resumen Financiero Coinly:**\n\n")
        sb.append("• **Balance neto:** ${if (balance >= 0) "+" else ""}${Ui.money(balance)}\n")
        sb.append("• **Ingresos acumulados:** +${Ui.money(income)}\n")
        sb.append("• **Gastos acumulados:** −${Ui.money(expense)}\n")

        if (topCategory != null) {
            sb.append("• **Mayor gasto en:** ${topCategory.first} (${Ui.money(topCategory.second)})\n")
        }

        if (budgets.isNotEmpty()) {
            sb.append("• **Presupuestos:** ${budgets.size} activos ")
            if (exceededCount > 0 || warningCount > 0) {
                sb.append("($exceededCount en alerta, $warningCount cercanos al límite)")
            } else {
                sb.append("(todos al día)")
            }
            sb.append("\n")
        }

        sb.append("\n💬 *¿En qué puedo ayudarte hoy? Puedes preguntarme sobre tus presupuestos, hábitos de ahorro o análisis de gastos.*")
        return sb.toString()
    }
}
