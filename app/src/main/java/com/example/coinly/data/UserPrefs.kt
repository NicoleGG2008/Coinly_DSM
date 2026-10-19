package com.example.coinly.data

import android.content.Context
import java.security.MessageDigest

/** Perfil, sesión local y preferencias de accesibilidad. Todo se guarda solo en el dispositivo. */
class UserPrefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var name: String
        get() = prefs.getString("p_name", "Usuario") ?: "Usuario"
        set(v) = prefs.edit().putString("p_name", v).apply()
    var email: String
        get() = prefs.getString("p_email", "usuario@estudiante.udb") ?: ""
        set(v) = prefs.edit().putString("p_email", v).apply()
    var university: String
        get() = prefs.getString("p_university", "Universidad Don Bosco") ?: ""
        set(v) = prefs.edit().putString("p_university", v).apply()
    var career: String
        get() = prefs.getString("p_career", "Ingeniería en Ciencias de la Computación") ?: ""
        set(v) = prefs.edit().putString("p_career", v).apply()
    var currency: String
        get() = prefs.getString("p_currency", "USD") ?: "USD"
        set(v) = prefs.edit().putString("p_currency", v).apply()
    var avatarUri: String?
        get() = prefs.getString("p_avatar", null)
        set(v) = prefs.edit().putString("p_avatar", v).apply()

    val firstName: String get() = name.trim().substringBefore(' ').ifEmpty { "Usuario" }
    val initial: String get() = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"

    // ----- Cuenta local (demo sin backend) -----
    val hasAccount: Boolean get() = prefs.getString("acc_hash", null) != null

    fun register(name: String, email: String, password: String, currency: String) {
        prefs.edit()
            .putString("p_name", name).putString("p_email", email).putString("p_currency", currency)
            .putString("acc_hash", hash(password))
            .apply()
    }

    /** Si hay una cuenta registrada se valida; si no, se acepta cualquier credencial con formato válido. */
    fun login(email: String, password: String): Boolean {
        val stored = prefs.getString("acc_hash", null) ?: return true
        return stored == hash(password) && email.equals(this.email, ignoreCase = true)
    }

    fun checkPassword(password: String): Boolean {
        val stored = prefs.getString("acc_hash", null) ?: return true
        return stored == hash(password)
    }

    fun setPassword(password: String) = prefs.edit().putString("acc_hash", hash(password)).apply()

    private fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        private const val PREFS = "coinly"
    }
}
