package com.example.coinly.data
import com.example.coinly.S
import com.example.coinly.R
import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/** Only profile cache and per-user UI preferences. Passwords belong to Firebase Auth. */
class UserPrefs(context: Context) {
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "signed_out"
    private val prefs = context.applicationContext.getSharedPreferences("coinly_user_$uid", Context.MODE_PRIVATE)
    var name: String
        get() = prefs.getString("name", "").orEmpty()
        set(v) = prefs.edit().putString("name", v).apply()
    var email: String
        get() = FirebaseAuth.getInstance().currentUser?.email ?: prefs.getString("email", "").orEmpty()
        set(v) = prefs.edit().putString("email", v).apply()
    var university: String
        get() = prefs.getString("university", "").orEmpty()
        set(v) = prefs.edit().putString("university", v).apply()
    var career: String
        get() = prefs.getString("career", "").orEmpty()
        set(v) = prefs.edit().putString("career", v).apply()
    var currency: String
        get() = prefs.getString("currency", "USD") ?: "USD"
        set(v) = prefs.edit().putString("currency", v).apply()
    var avatarUri: String?
        get() = prefs.getString("avatarUri", null)
        set(v) = prefs.edit().putString("avatarUri", v).apply()
    var darkMode: Boolean
        get() = prefs.getBoolean("darkMode", false)
        set(v) = prefs.edit().putBoolean("darkMode", v).apply()
    var fontFactor: Float
        get() = prefs.getFloat("fontFactor", 1f)
        set(v) = prefs.edit().putFloat("fontFactor", v).apply()
    val firstName get() = name.trim().substringBefore(' ').ifEmpty { S.text(R.string.text_110) }
    val initial get() = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
    fun cacheProfile(data: Map<String, Any>) {
        val update = prefs.edit()
        listOf("name", "email", "university", "career", "currency").forEach { key ->
            (data[key] as? String)?.let { update.putString(key, it) }
        }
        update.apply()
    }
}
