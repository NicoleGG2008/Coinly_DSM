package com.example.coinly

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.coinly.data.UserPrefs

class CoinlyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AiAppCheck.install()
        S.context = applicationContext
        com.example.coinly.data.AuthSession.context = applicationContext
        com.example.coinly.data.CloudStore.context = applicationContext
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(com.google.firebase.firestore.MemoryCacheSettings.newBuilder().build()).build()
        if (BuildConfig.FIREBASE_EMULATOR) {
            auth.useEmulator("10.0.2.2", 9199)
            db.useEmulator("10.0.2.2", 8180)
        }
        if (!com.example.coinly.data.AuthSession.remember) auth.signOut()
        auth.addAuthStateListener {
            com.example.coinly.data.CloudStore.start()
            if (!BuildConfig.FIREBASE_EMULATOR) com.example.coinly.data.EconomicEventWork.configure(this, it.currentUser?.uid)
        }
        applyTheme(UserPrefs(this).darkMode)
    }

    companion object {
        /** Cambia el tema de toda la app; las pantallas abiertas se recrean solas. */
        fun applyTheme(dark: Boolean) {
            AppCompatDelegate.setDefaultNightMode(
                if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }
}
