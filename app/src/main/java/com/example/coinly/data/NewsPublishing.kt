package com.example.coinly.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.coinly.R
import com.example.coinly.ui.Ui
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class PublishState(val busy: Boolean = false, val done: Boolean = false, val error: Int? = null)

class NewsPublishing : ViewModel() {
    val state = MutableLiveData(PublishState())
    fun publish(title: String, summary: String, content: String, category: String, eventDate: String) {
        if (state.value?.busy == true || state.value?.done == true) return
        if (title.length !in 1..140 || summary.length !in 1..500 || content.length !in 1..10000 ||
            category !in NewsRepository.categories.drop(1) || (eventDate.isNotEmpty() && !NewsDates.valid(eventDate))) {
            state.value = PublishState(error = R.string.news_invalid); return
        }
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            state.value = PublishState(error = R.string.news_admin_required); return
        }
        state.value = PublishState(busy = true)
        user.getIdToken(true).addOnCompleteListener { token ->
            if (!token.isSuccessful) {
                state.value = PublishState(error = R.string.news_role_failed); return@addOnCompleteListener
            }
            if (token.result.claims["newsAdmin"] != true || FirebaseAuth.getInstance().currentUser?.uid != user.uid) {
                state.value = PublishState(error = R.string.news_admin_required); return@addOnCompleteListener
            }
            val imageKey = listOf("ahorro", "presupuesto", "inversion", "becas").getOrElse(NewsRepository.categories.indexOf(category) - 1) { "ahorro" }
            FirebaseFirestore.getInstance().collection("noticias").document().set(mapOf(
                "title" to title, "summary" to summary, "content" to content, "category" to category,
                "date" to Ui.todayIso(), "published" to true, "imageKey" to imageKey,
                "authorUid" to user.uid, "createdAt" to FieldValue.serverTimestamp(),
                "eventDate" to eventDate, "eventType" to "FINANCIAL"
            )).addOnCompleteListener { result ->
                val denied = (result.exception as? com.google.firebase.firestore.FirebaseFirestoreException)?.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
                state.value = if (result.isSuccessful) PublishState(done = true)
                    else PublishState(error = if (denied) R.string.news_write_denied else R.string.news_network_error)
            }
        }
    }
}
