package com.example.coinly

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

object AiAppCheck {
    fun showHelp(context: android.content.Context) {
        FirebaseAppCheck.getInstance().getAppCheckToken(false).addOnCompleteListener {
            val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
            executor.execute {
                val secret = runCatching {
                    val process = ProcessBuilder("logcat", "-d", "--pid=" + android.os.Process.myPid(), "-t", "1500").start()
                    val log = process.inputStream.bufferedReader().use { it.readText() }
                    Regex("Firebase App Check debug token: ([a-fA-F0-9-]{36})").findAll(log).lastOrNull()?.groupValues?.get(1)
                }.getOrNull()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (context is android.app.Activity && !context.isFinishing && !context.isDestroyed) {
                        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                            .setTitle(com.example.coinly.R.string.ai_device_help)
                            .setMessage(if (secret == null) com.example.coinly.R.string.ai_device_no_code else com.example.coinly.R.string.ai_device_instructions)
                            .setNegativeButton(android.R.string.cancel, null)
                        if (secret != null) builder.setPositiveButton(com.example.coinly.R.string.ai_copy_code) { _, _ ->
                            (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                                .setPrimaryClip(android.content.ClipData.newPlainText("App Check privado", secret))
                        }
                        builder.show()
                    }
                }
                executor.shutdown()
            }
        }
    }
    fun install() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}
