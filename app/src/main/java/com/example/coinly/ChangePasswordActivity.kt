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

            if (current.isEmpty()) { tilCurrent.error = S.text(R.string.text_020); valid = false }

            if (new.length < 6) { tilNew.error = S.text(R.string.text_021); valid = false }
            if (new != confirm) { tilConfirm.error = S.text(R.string.text_022); valid = false }
            if (!valid) return

            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
            AuthUi.run(this, findViewById(R.id.btnSavePassword), {
                user.reauthenticate(com.google.firebase.auth.EmailAuthProvider.getCredential(user.email.orEmpty(), current))
                    .continueWithTask { result ->
                        if (!result.isSuccessful) throw result.exception ?: IllegalStateException()
                        user.updatePassword(new)
                    }
            }) {
                Ui.toast(findViewById(android.R.id.content), Tone.SUCCESS, S.text(R.string.text_023))
                finish()
            }
        }
        findViewById<MaterialButton>(R.id.btnSavePassword).setOnClickListener { save() }
        etConfirm.setOnEditorActionListener { _, _, _ -> save(); true }
    }
}
