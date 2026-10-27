package com.example.coinly.data
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
object AuthSession {
    lateinit var context: Context
    var remember: Boolean
        get() = context.getSharedPreferences("coinly_session", Context.MODE_PRIVATE).getBoolean("remember", false)
        set(v) = context.getSharedPreferences("coinly_session", Context.MODE_PRIVATE).edit().putBoolean("remember", v).apply()
    fun logout() { remember = false; FirebaseAuth.getInstance().signOut(); CloudStore.reset(); com.example.coinly.ui.CoinlyAiChatSheet.clearSession() }
}
