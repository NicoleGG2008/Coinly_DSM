package com.example.coinly.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.coinly.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class EconomicEditorState(val ready: Boolean = false, val busy: Boolean = false,
    val done: Boolean = false, val data: EconomicEvent? = null, val error: Int? = null)

class EconomicEventEditor : ViewModel() {
    val state = MutableLiveData(EconomicEditorState())
    private var loaded = false
    private var owner: String? = null
    fun load(id: String, existing: Boolean) {
        if (loaded) return
        loaded = true; owner = FirebaseAuth.getInstance().currentUser?.uid
        val uid = owner ?: return
        if (!existing) { state.value = EconomicEditorState(ready = true); return }
        EconomicEvents.collection(uid).document(id).get().addOnCompleteListener { result ->
            val event = if (result.isSuccessful) EconomicEvents.from(result.result) else null
            state.value = EconomicEditorState(ready = event != null, data = event,
                error = if (event == null) R.string.economic_error else null)
        }
    }
    fun save(e: EconomicEvent) {
        val previous = state.value ?: return
        if (!previous.ready || previous.busy || previous.done || previous.data?.posted == true) return
        if (!EconomicEvents.valid(e) || e.date < EconomicEvents.today()) {
            state.value = previous.copy(error = R.string.economic_invalid); return
        }
        val uid = owner ?: return
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) return
        state.value = previous.copy(busy = true, error = null)
        val doc = EconomicEvents.collection(uid).document(e.id)
        FirebaseFirestore.getInstance().runTransaction<Void> { tx ->
            val current = tx.get(doc)
            check(!current.exists() || current.getString("status") == "pending") { "ALREADY_POSTED" }
            tx.set(doc, mapOf("title" to e.title, "amount" to e.amount, "type" to e.type, "category" to e.category,
                "date" to e.date, "dueAt" to EconomicEvents.dueAt(e.date), "status" to "pending", "movementId" to "",
                "createdAt" to (current.getTimestamp("createdAt") ?: FieldValue.serverTimestamp())))
            null
        }.addOnCompleteListener { result ->
            state.value = previous.copy(busy = false, done = result.isSuccessful, data = if (result.isSuccessful) e else previous.data,
                error = if (result.isSuccessful) null else R.string.economic_error)
        }
    }
    fun delete(id: String) {
        val previous = state.value ?: return
        val uid = owner ?: return
        if (!previous.ready || previous.busy || FirebaseAuth.getInstance().currentUser?.uid != uid) return
        state.value = previous.copy(busy = true, error = null)
        EconomicEvents.collection(uid).document(id).delete().addOnCompleteListener { result ->
            state.value = previous.copy(busy = false, done = result.isSuccessful, error = if (result.isSuccessful) null else R.string.economic_error)
        }
    }
}
