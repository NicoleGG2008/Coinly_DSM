package com.example.coinly.data.MovementRepository

import android.content.Context
import com.example.coinly.model.Movement
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistencia local de movimientos (SharedPreferences + JSON).
 * La primera vez se siembran los datos de ejemplo del diseño; desde ahí todo es editable.
 */
class MovementRepository(private val context: Context? = null) {

    private val prefs get() = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getMovements(): List<Movement> {
        val saved = prefs?.getString(KEY, null)
        val list = if (saved == null) {
            sampleMovements().also { save(it) }
        } else {
            runCatching { parse(saved) }.getOrElse { sampleMovements() }
        }
        return list.sortedWith(compareByDescending<Movement> { it.date }.thenByDescending { it.id })
    }

    fun getById(id: String): Movement? = getMovements().firstOrNull { it.id == id }

    fun addMovement(movement: Movement) = save(getMovements() + movement)

    fun updateMovement(movement: Movement) =
        save(getMovements().map { if (it.id == movement.id) movement else it })

    fun deleteMovement(id: String) = save(getMovements().filterNot { it.id == id })

    private fun parse(json: String): List<Movement> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            val tagsArray = o.optJSONArray("tags") ?: JSONArray()
            Movement(
                id = o.getString("id"),
                description = o.getString("description"),
                amount = o.getDouble("amount"),
                type = o.getString("type"),
                category = o.getString("category"),
                date = o.getString("date"),
                paymentMethod = o.optString("paymentMethod", ""),
                tags = (0 until tagsArray.length()).map { tagsArray.getString(it) }
            )
        }
    }

    private fun save(items: List<Movement>) {
        val array = JSONArray()
        items.forEach { m ->
            array.put(
                JSONObject()
                    .put("id", m.id)
                    .put("description", m.description)
                    .put("amount", m.amount)
                    .put("type", m.type)
                    .put("category", m.category)
                    .put("date", m.date)
                    .put("paymentMethod", m.paymentMethod)
                    .put("tags", JSONArray(m.tags))
            )
        }
        prefs?.edit()?.putString(KEY, array.toString())?.apply()
    }

    private fun sampleMovements(): List<Movement> = listOf(
        Movement("1", "Pago mensual trabajo part-time", 500.0, "income", "Salario", "2026-08-10", "Transferencia", listOf("trabajo")),
        Movement("2", "Beca universitaria agosto", 300.0, "income", "Beca", "2026-08-05", "Transferencia", listOf("beca")),
        Movement("3", "Almuerzo semana", 45.0, "expense", "Alimentación", "2026-08-10", "Efectivo"),
        Movement("4", "Pasajes bus mensual", 25.0, "expense", "Transporte", "2026-08-09", "Tarjeta", listOf("transporte")),
        Movement("5", "Libros semestre", 80.0, "expense", "Educación", "2026-08-07", "Tarjeta", listOf("libros")),
        Movement("6", "Internet y teléfono", 60.0, "expense", "Servicios", "2026-08-06", "Transferencia"),
        Movement("7", "Cine con amigos", 35.0, "expense", "Ocio", "2026-08-03", "Efectivo", listOf("social")),
        Movement("8", "Ropa de temporada", 129.5, "expense", "Ropa", "2026-08-01", "Tarjeta"),
        Movement("9", "Diseño logo cliente", 75.0, "income", "Freelance", "2026-07-28", "Transferencia", listOf("freelance")),
        Movement("10", "Cafetería universidad", 20.0, "expense", "Alimentación", "2026-07-25", "Efectivo")
    )

    companion object {
        private const val PREFS = "coinly"
        private const val KEY = "movements_v2"

        val INCOME_CATEGORIES = listOf("Salario", "Beca", "Freelance", "Regalo", "Inversión", "Otros")
        val EXPENSE_CATEGORIES = listOf("Alimentación", "Transporte", "Ocio", "Servicios", "Educación", "Salud", "Ropa", "Otros")
        val PAYMENT_METHODS = listOf("Efectivo", "Tarjeta", "Transferencia", "Otro")
    }
}
