package com.example.coinly.data

import com.example.coinly.model.CalendarEvent
import com.example.coinly.model.EventType
import com.example.coinly.model.Movement
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class EconomicEvent(val id: String, val title: String, val amount: Double, val type: String,
    val category: String, val date: String, val posted: Boolean = false, val movementId: String = "") {
    fun calendar() = CalendarEvent("scheduled-$id", date,
        "$title · ${if (type == Movement.TYPE_INCOME) "+" else "−"}${String.format(Locale.ROOT, "%.2f", amount)} · ${if (posted) "Registrado" else "Programado"}",
        EventType.FINANCIAL, id, movementId)
}

object EconomicEvents {
    fun dueAt(date: String): Timestamp? = if (!NewsDates.valid(date)) null else runCatching {
        Timestamp(SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            isLenient = false; timeZone = TimeZone.getTimeZone("America/El_Salvador")
        }.parse(date)!!)
    }.getOrNull()
    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("America/El_Salvador")
    }.format(java.util.Date())
    fun valid(e: EconomicEvent) = e.title.isNotBlank() && e.title.length <= 500 &&
        e.amount.isFinite() && e.amount > 0 && e.amount <= 1_000_000_000 &&
        e.type in listOf(Movement.TYPE_INCOME, Movement.TYPE_EXPENSE) &&
        e.category.isNotBlank() && e.category.length <= 100 && dueAt(e.date) != null
    fun collection(uid: String) = FirebaseFirestore.getInstance().collection("usuarios").document(uid).collection("eventos")
    fun from(doc: DocumentSnapshot): EconomicEvent? = runCatching {
        EconomicEvent(doc.id, doc.getString("title").orEmpty(), doc.getDouble("amount") ?: return@runCatching null,
            doc.getString("type").orEmpty(), doc.getString("category").orEmpty(), doc.getString("date").orEmpty(),
            doc.getString("status") == "posted", doc.getString("movementId").orEmpty()).takeIf(::valid)
    }.getOrNull()

    /** The transaction makes event completion and movement creation one atomic operation. */
    fun post(uid: String, id: String): Task<Void> {
        val db = FirebaseFirestore.getInstance()
        val eventRef = collection(uid).document(id)
        val movementId = "prog_$id"
        val movementRef = db.collection("usuarios").document(uid).collection("movimientos").document(movementId)
        val attempt = db.runTransaction<Void> { tx ->
            check(FirebaseAuth.getInstance().currentUser?.uid == uid) { "SESSION_CHANGED" }
            val doc = tx.get(eventRef)
            if (!doc.exists() || doc.getString("status") != "pending") return@runTransaction null
            val e = from(doc) ?: error("INVALID_EVENT")
            check((doc.getTimestamp("dueAt") ?: error("INVALID_DATE")) <= Timestamp.now()) { "NOT_DUE" }
            check(!tx.get(movementRef).exists()) { "MOVEMENT_CONFLICT" }
            tx.set(movementRef, mapOf("id" to movementId, "description" to e.title, "amount" to e.amount,
                "type" to e.type, "category" to e.category, "date" to e.date,
                "paymentMethod" to "Programado", "tags" to listOf("programado")))
            tx.update(eventRef, mapOf("status" to "posted", "movementId" to movementId))
            null
        }
        return attempt.continueWithTask { result ->
            if (result.isSuccessful) com.google.android.gms.tasks.Tasks.forResult<Void>(null)
            else {
                val failure = result.exception ?: IllegalStateException("POST_FAILED")
                if (FirebaseAuth.getInstance().currentUser?.uid != uid) return@continueWithTask com.google.android.gms.tasks.Tasks.forException<Void>(failure)
                // A concurrent device may commit after our read. Verify that completed state;
                // never retry by creating a second movement with a new ID.
                eventRef.get(Source.SERVER).continueWithTask { current ->
                    if (!current.isSuccessful || current.result.getString("status") != "posted" || current.result.getString("movementId") != movementId)
                        com.google.android.gms.tasks.Tasks.forException<Void>(failure)
                    else movementRef.get(Source.SERVER).continueWithTask { movement ->
                        if (movement.isSuccessful && movement.result.exists()) com.google.android.gms.tasks.Tasks.forResult<Void>(null)
                        else com.google.android.gms.tasks.Tasks.forException<Void>(failure)
                    }
                }
            }
        }
    }
    suspend fun processDue(uid: String) {
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) return
        val pending = collection(uid).whereEqualTo("status", "pending").get(Source.SERVER).awaitResult()
        for (doc in pending.documents) {
            if (FirebaseAuth.getInstance().currentUser?.uid != uid) return
            if (doc.getTimestamp("dueAt")?.let { it <= Timestamp.now() } == true) post(uid, doc.id).awaitResult()
        }
    }
}

suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (continuation.isActive) {
            if (task.isSuccessful) continuation.resume(task.result)
            else continuation.resumeWithException(task.exception ?: IllegalStateException("TASK_FAILED"))
        }
    }
}
