package com.example.coinly

import android.os.Bundle
import android.view.View
import com.example.coinly.data.UserPrefs
import com.example.coinly.ui.Tone
import com.example.coinly.ui.Ui
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ChangePasswordActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setScreen(R.layout.activity_change_password)

        val prefs = UserPrefs(this)
        val tilCurrent = findViewById<TextInputLayout>(R.id.tilCurrentPassword)
        val tilNew = findViewById<TextInputLayout>(R.id.tilNewPassword)
        val tilConfirm = findViewById<TextInputLayout>(R.id.tilConfirmNewPassword)
        val etCurrent = findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val etNew = findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirm = findViewById<TextInputEditText>(R.id.etConfirmNewPassword)
        Ui.clearErrorOnType(tilCurrent, etCurrent)
        Ui.clearErrorOnType(tilNew, etNew)
        Ui.clearErrorOnType(tilConfirm, etConfirm)

        findViewById<View>(R.id.btnBackChangePassword).setOnClickListener { finish() }

        fun save() {
            val current = etCurrent.text.toString()
            val new = etNew.text.toString()
            val confirm = etConfirm.text.toString()
            var valid = true

            if (current.isEmpty()) { tilCurrent.error = "Completa este campo."; valid = false }
            else if (!prefs.checkPassword(current)) { tilCurrent.error = "La contraseña actual no es correcta."; valid = false }
            if (new.length < 6) { tilNew.error = "Mínimo 6 caracteres."; valid = false }
            if (new != confirm) { tilConfirm.error = "Las contraseñas no coinciden."; valid = false }
            if (!valid) return

            prefs.setPassword(new)
            Ui.toast(findViewById(android.R.id.content), Tone.SUCCESS, "Contraseña actualizada correctamente.")
            findViewById<View>(R.id.btnSavePassword).postDelayed({ finish() }, 900)
        }
        findViewById<MaterialButton>(R.id.btnSavePassword).setOnClickListener { save() }
        etConfirm.setOnEditorActionListener { _, _, _ -> save(); true }
    }
}
