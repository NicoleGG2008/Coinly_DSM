package com.example.coinly.data.ai

import com.example.coinly.model.Movement
import java.util.Locale

/** Read-only facts from the authenticated user's existing repository snapshot. */
object FinancialSnapshot {
    fun describe(movements: List<Movement>, today: String, currency: String): String {
        val month = today.take(7)
        val current = movements.filter { it.date.startsWith("$month-") }
        fun amount(value: Double) = String.format(Locale.ROOT, "%.2f", value)
        fun safe(value: String) = value.replace('\n', ' ').replace('\r', ' ').take(80)
        return buildString {
            appendLine("Fecha de consulta: $today. Moneda registrada: ${safe(currency)}; no conviertas monedas.")
            appendLine("El saldo y los totales anteriores corresponden a todos los registros disponibles, no solo a este mes.")
            appendLine("Mes $month: ingresos=${amount(current.filter { it.isIncome }.sumOf { it.amount })}; gastos=${amount(current.filter { !it.isIncome }.sumOf { it.amount })}.")
            appendLine("Gastos de este mes por categoría:")
            current.filter { !it.isIncome }.groupBy { it.category }.entries
                .sortedByDescending { entry -> entry.value.sumOf { it.amount } }.take(20).forEach { (category, items) ->
                    appendLine("${safe(category)}: ${amount(items.sumOf { it.amount })}")
                }
            appendLine("Muestra de ${minOf(30, movements.size)} movimientos más recientes de ${movements.size} registros. Es una muestra, no un historial completo por fechas:")
            movements.sortedByDescending { it.date }.take(30).forEach { m ->
                appendLine("fecha=${safe(m.date)}; tipo=${if (m.isIncome) "ingreso" else "gasto"}; categoría=${safe(m.category)}; importe=${amount(m.amount)}; etiquetas=${m.tags.take(5).joinToString(",") { safe(it) }}")
            }
        }
    }
}
