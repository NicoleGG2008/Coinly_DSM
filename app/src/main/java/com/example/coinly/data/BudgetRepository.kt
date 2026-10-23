package com.example.coinly.data

import android.content.Context
import com.example.coinly.data.MovementRepository.MovementRepository
import com.example.coinly.model.Budget
import org.json.JSONArray
import org.json.JSONObject

/** Presupuestos y alertas descartadas, compartidos entre Inicio y Presupuestos.
 * Calcula los gastos reales dinámicamente desde MovementRepository.
 */
class BudgetRepository(private val context: Context) {

    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getBudgets(): List<Budget> {
        val rawBudgets = loadRawBudgets()
        val movements = MovementRepository(context).getMovements()
        val spentMap = movements.filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        return rawBudgets.map { b ->
            val realSpent = spentMap[b.category] ?: 0.0
            b.copy(spent = realSpent)
        }
    }

    private fun loadRawBudgets(): List<Budget> {
        val saved = prefs.getString(KEY, null) ?: return sample().also { save(it) }
        return runCatching {
            val array = JSONArray(saved)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Budget(
                    o.getString("id"),
                    o.getString("category"),
                    o.getDouble("limit"),
                    o.optDouble("spent", 0.0),
                    o.getString("period")
                )
            }
        }.getOrElse { sample() }
    }

    fun upsert(budget: Budget) {
        val all = loadRawBudgets()
        val exists = all.any { it.id == budget.id }
        save(if (exists) all.map { if (it.id == budget.id) budget else it } else all + budget)
        restoreAlert(budget.id)
    }

    fun delete(id: String) {
        save(loadRawBudgets().filterNot { it.id == id })
        restoreAlert(id)
    }

    // ----- Alertas -----
    fun dismissedAlerts(): Set<String> = prefs.getStringSet(KEY_DISMISSED, emptySet()) ?: emptySet()

    fun dismissAlert(id: String) {
        prefs.edit().putStringSet(KEY_DISMISSED, dismissedAlerts() + id).apply()
    }

    fun dismissAll(ids: Collection<String>) {
        prefs.edit().putStringSet(KEY_DISMISSED, dismissedAlerts() + ids).apply()
    }

    private fun restoreAlert(id: String) {
        prefs.edit().putStringSet(KEY_DISMISSED, dismissedAlerts() - id).apply()
    }

    private fun save(items: List<Budget>) {
        val array = JSONArray()
        items.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("category", it.category)
                    .put("limit", it.limit)
                    .put("spent", it.spent)
                    .put("period", it.period)
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private fun sample() = listOf(
        Budget("1", "Alimentación", 200.0, 0.0, "Agosto 2026"),
        Budget("2", "Transporte", 80.0, 0.0, "Agosto 2026"),
        Budget("3", "Ocio", 60.0, 0.0, "Agosto 2026"),
        Budget("4", "Educación", 100.0, 0.0, "Agosto 2026"),
        Budget("5", "Servicios", 70.0, 0.0, "Agosto 2026")
    )

    companion object {
        private const val PREFS = "coinly"
        private const val KEY = "budgets_v2"
        private const val KEY_DISMISSED = "dismissed_alerts"
    }
}
