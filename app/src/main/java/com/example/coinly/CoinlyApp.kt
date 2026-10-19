package com.example.coinly

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.coinly.data.UserPrefs

class CoinlyApp : Application() {
    override fun onCreate() {
        super.onCreate()
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
