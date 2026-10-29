package com.example.coinly

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.example.coinly.ui.Ui
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : BaseActivity() {
    companion object { const val EXTRA_EMAIL = "reset_email" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setScreen(R.layout.activity_forgot_password)
        val model = ViewModelProvider(this)[PasswordActionsViewModel::class.java]
        val til = findViewById<TextInputLayout>(R.id.tilEmailForgot)
        val email = findViewById<TextInputEditText>(R.id.etEmailForgot)
        val button = findViewById<MaterialButton>(R.id.btnSendReset)
        val error = findViewById<TextView>(R.id.tvResetError)
        val message = findViewById<TextView>(R.id.tvResetMessage)
        Ui.clearErrorOnType(til, email)
        if (savedInstanceState == null) email.setText(intent.getStringExtra(EXTRA_EMAIL)
            ?: FirebaseAuth.getInstance().currentUser?.email.orEmpty())
        fun send() {
            if (model.state.value?.busy == true) return
            val value = email.text.toString().trim()
            if (!Ui.isEmail(value)) { til.error = getString(R.string.text_005); return }
            til.error = null
            error.visibility = View.GONE
            model.requestReset(value)
        }
        button.setOnClickListener { send() }
        email.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_UP)) {
                send(); true
            } else false
        }
        findViewById<View>(R.id.btnBackForgot).setOnClickListener { finish() }
        findViewById<View>(R.id.btnBackToLogin).setOnClickListener { finish() }
        findViewById<View>(R.id.btnResetAgain).setOnClickListener { model.editEmail(); email.requestFocus() }
        model.state.observe(this) { state ->
            button.isEnabled = !state.busy
            email.isEnabled = !state.busy
            button.setText(if (state.busy) R.string.password_sending else R.string.text_291)
            findViewById<View>(R.id.layoutForgotForm).visibility = if (state.resetRequested) View.GONE else View.VISIBLE
            findViewById<View>(R.id.layoutForgotSent).visibility = if (state.resetRequested) View.VISIBLE else View.GONE
            if (state.resetRequested) message.text = getString(R.string.password_reset_explanation, state.requestedEmail)
            val reason = state.errorRes
            if (reason != null && state.errorField == PasswordErrorField.EMAIL) {
                til.error = getString(reason); error.visibility = View.GONE
            } else {
                error.text = reason?.let { getString(it) }.orEmpty()
                error.visibility = if (reason == null) View.GONE else View.VISIBLE
            }
        }
    }
}
