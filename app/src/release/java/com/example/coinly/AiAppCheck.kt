package com.example.coinly

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

object AiAppCheck {
    fun showHelp(context: android.content.Context) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle(com.example.coinly.R.string.ai_device_help).setMessage(com.example.coinly.R.string.ai_release_help)
            .setPositiveButton(android.R.string.ok, null).show()
    }
    fun install() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
    }
}
