package com.example.coinly.data.MovementRepository
import com.example.coinly.S
import com.example.coinly.R
import android.content.Context
import com.example.coinly.data.CloudStore
import com.example.coinly.model.Movement
import com.google.android.gms.tasks.Task
class MovementRepository(context: Context? = null) {
    fun getMovements(): List<Movement> = CloudStore.movements
    fun getById(id: String) = getMovements().firstOrNull { it.id == id }
    fun addMovement(movement: Movement): Task<Void> = save(movement)
    fun updateMovement(movement: Movement): Task<Void> = save(movement)
    fun deleteMovement(id: String): Task<Void> = CloudStore.write {
        CloudStore.userRef().collection("movimientos").document(id).delete()
    }
    private fun save(m: Movement): Task<Void> = CloudStore.write {
        CloudStore.userRef().collection("movimientos").document(m.id).set(mapOf(
            "id" to m.id, "description" to m.description, "amount" to m.amount,
            "type" to m.type, "category" to m.category, "date" to m.date,
            "paymentMethod" to m.paymentMethod, "tags" to m.tags
        ))
    }
    companion object {

        val INCOME_CATEGORIES = listOf(S.text(R.string.text_246), S.text(R.string.text_247), S.text(R.string.text_248), S.text(R.string.text_249), S.text(R.string.text_084), S.text(R.string.text_250))
        val EXPENSE_CATEGORIES = listOf(S.text(R.string.text_251), S.text(R.string.text_252), S.text(R.string.text_253), S.text(R.string.text_254), S.text(R.string.text_255), S.text(R.string.text_256), S.text(R.string.text_257), S.text(R.string.text_250))
        val PAYMENT_METHODS = listOf(S.text(R.string.text_258), S.text(R.string.text_259), S.text(R.string.text_260), S.text(R.string.text_261))
    }
}
