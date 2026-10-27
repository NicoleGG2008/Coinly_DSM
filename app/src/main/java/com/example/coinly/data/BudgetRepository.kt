package com.example.coinly.data
import android.content.Context
import com.example.coinly.model.Budget
import com.google.android.gms.tasks.Task
import java.text.SimpleDateFormat
import java.util.Locale
class BudgetRepository(context: Context) {
    private val uid = CloudStore.auth.currentUser?.uid ?: "signed_out"
    private val prefs = context.getSharedPreferences("coinly_alerts_$uid", Context.MODE_PRIVATE)
    fun getBudgets(): List<Budget> = CloudStore.budgets.map { b ->
        val period = b.period
        val spent = CloudStore.movements.filter { m ->
            !m.isIncome && m.category == b.category && m.date.take(7) == period
        }.sumOf { it.amount }
        b.copy(spent = spent)
    }
    fun upsert(b: Budget): Task<Void> = CloudStore.write {
        CloudStore.userRef().collection("presupuestos").document(b.id).set(mapOf(
            "id" to b.id, "category" to b.category, "limit" to b.limit, "period" to b.period
        ))
    }.addOnSuccessListener { restoreAlert(b.id) }
    fun delete(id: String): Task<Void> = CloudStore.write {
        CloudStore.userRef().collection("presupuestos").document(id).delete()
    }.addOnSuccessListener { restoreAlert(id) }
    fun dismissedAlerts(): Set<String> = prefs.getStringSet("dismissed", emptySet()).orEmpty()
    fun dismissAlert(id: String) = dismissAll(listOf(id))
    fun dismissAll(ids: Collection<String>) { prefs.edit().putStringSet("dismissed", dismissedAlerts() + ids).apply() }
    private fun restoreAlert(id: String) { prefs.edit().putStringSet("dismissed", dismissedAlerts() - id).apply() }
}
