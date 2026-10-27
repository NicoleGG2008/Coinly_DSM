package com.example.coinly.data
import com.example.coinly.R

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import com.example.coinly.S
import com.example.coinly.model.Movement
import com.example.coinly.model.Budget
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

/** One in-memory snapshot per authenticated user. No financial data in SharedPreferences. */
object CloudStore {
    lateinit var context: Context
    val auth get() = FirebaseAuth.getInstance()
    val db get() = FirebaseFirestore.getInstance()
    var movements: List<Movement> = emptyList(); private set
    var budgets: List<Budget> = emptyList(); private set
    var loading = false; private set
    var error: String? = null; private set
    var cached = false; private set
    var pending = 0; private set
    private var owner: String? = null
    private var generation = 0
    private val registrations = mutableListOf<ListenerRegistration>()
    private val observers = linkedSetOf<() -> Unit>()
    private val waiting = mutableSetOf<String>()
    private val readErrors = mutableMapOf<String, String>()
    private val cacheStates = mutableMapOf<String, Boolean>()
    fun observe(listener: () -> Unit): () -> Unit {
        observers.add(listener)
        listener()
        return { observers.remove(listener) }
    }
    private fun emit() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { emit() }; return
        }
        observers.toList().forEach { it() }
    }
    fun online(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    fun reset() {
        generation++
        registrations.forEach { it.remove() }; registrations.clear()
        owner = null; movements = emptyList(); budgets = emptyList()
        loading = false; error = null; pending = 0; cached = false
        waiting.clear(); readErrors.clear(); cacheStates.clear()
        emit()
    }
    fun start(force: Boolean = false) {
        val uid = auth.currentUser?.uid ?: return reset()
        if (!force && owner == uid && registrations.isNotEmpty()) return
        reset(); owner = uid; loading = true
        val run = generation
        waiting.addAll(listOf("profile", "movements", "budgets")); emit()
        val user = db.collection("usuarios").document(uid)
        fun complete(key: String, fromCache: Boolean, exception: Exception?) {
            if (owner != uid || generation != run) return
            waiting.remove(key); loading = waiting.isNotEmpty()
            if (exception != null) readErrors[key] = message(exception) else readErrors.remove(key)
            error = readErrors.values.firstOrNull()
            cacheStates[key] = fromCache
            cached = cacheStates.values.any { it }
            emit()
        }
        registrations += user.addSnapshotListener(MetadataChanges.INCLUDE) { snap, e ->
            if (owner != uid || generation != run) return@addSnapshotListener
            if (snap != null && snap.exists()) UserPrefs(context).cacheProfile(snap.data.orEmpty())
            complete("profile", snap?.metadata?.isFromCache ?: false, e)
        }
        registrations += user.collection("movimientos").addSnapshotListener(MetadataChanges.INCLUDE) { snap, e ->
            if (owner != uid || generation != run) return@addSnapshotListener
            if (snap != null) movements = snap.documents.mapNotNull { doc ->
                val amount = doc.getDouble("amount") ?: return@mapNotNull null
                Movement(doc.id, doc.getString("description").orEmpty(), amount,
                    doc.getString("type").orEmpty(), doc.getString("category").orEmpty(),
                    doc.getString("date").orEmpty(), doc.getString("paymentMethod").orEmpty(),
                    (doc.get("tags") as? List<*>)?.filterIsInstance<String>().orEmpty())
            }.sortedWith(compareByDescending<Movement> { it.date }.thenByDescending { it.id })
            complete("movements", snap?.metadata?.isFromCache ?: false, e)
        }
        registrations += user.collection("presupuestos").addSnapshotListener(MetadataChanges.INCLUDE) { snap, e ->
            if (owner != uid || generation != run) return@addSnapshotListener
            if (snap != null) budgets = snap.documents.mapNotNull { doc ->
                val limit = doc.getDouble("limit") ?: return@mapNotNull null
                Budget(doc.id, doc.getString("category").orEmpty(), limit, 0.0, doc.getString("period").orEmpty())
            }
            complete("budgets", snap?.metadata?.isFromCache ?: false, e)
        }
        Handler(Looper.getMainLooper()).postDelayed({
            if (owner == uid && generation == run && loading) {
                loading = false; error = S.text(R.string.text_078); emit()
            }
        }, 15000)
    }
    fun ensureProfile(): Task<Void> {
        val user = auth.currentUser ?: return Tasks.forException(IllegalStateException(S.text(R.string.login_required)))
        return userRef().get(Source.SERVER).continueWithTask { result ->
            if (!result.isSuccessful) throw result.exception ?: IllegalStateException()
            if (result.result.exists()) {
                if (result.result.getString("email") != user.email)
                    userRef().update("email", user.email.orEmpty())
                else Tasks.forResult<Void>(null)
            } else {
                val prefs = UserPrefs(context)
                userRef().set(mapOf("name" to prefs.name.ifBlank { user.email.orEmpty().substringBefore("@") }.take(100),
                    "email" to user.email.orEmpty(), "currency" to prefs.currency,
                    "university" to prefs.university, "career" to prefs.career))
            }
        }
    }
    fun userRef(): DocumentReference {
        val uid = auth.currentUser?.uid ?: error(S.text(R.string.text_079))
        return db.collection("usuarios").document(uid)
    }
    fun write(operation: () -> Task<Void>): Task<Void> {
        if (auth.currentUser == null) return Tasks.forException(IllegalStateException(S.text(R.string.text_079)))
        if (!online()) return Tasks.forException(IllegalStateException(S.text(R.string.text_001)))
        val run = generation
        pending++; error = null; emit()
        val task = try { operation() } catch (e: Exception) { Tasks.forException<Void>(e) }
        task.addOnCompleteListener {
            if (generation == run) {
                pending = (pending - 1).coerceAtLeast(0)
                error = it.exception?.let(::message)
                emit()
            }
        }
        return task
    }
    fun message(e: Exception): String {
        val code = (e as? FirebaseFirestoreException)?.code
        return when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> S.text(R.string.text_080)
            FirebaseFirestoreException.Code.UNAVAILABLE, FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> S.text(R.string.text_081)
            else -> e.localizedMessage ?: S.text(R.string.text_082)
        }
    }
}
