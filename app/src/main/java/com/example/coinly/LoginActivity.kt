package com.example.coinly

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.example.coinly.data.UserPrefs
import com.example.coinly.data.AuthSession
import com.example.coinly.data.CloudStore
import com.google.firebase.auth.FirebaseAuth
import android.widget.CheckBox
import com.example.coinly.ui.Ui
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setScreen(R.layout.activity_login)

        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmailLogin)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPasswordLogin)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmailLogin)
        val etPassword = findViewById<TextInputEditText>(R.id.etPasswordLogin)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        findViewById<CheckBox>(R.id.cbRemember).isChecked = AuthSession.remember

        Ui.clearErrorOnType(tilEmail, etEmail)
        Ui.clearErrorOnType(tilPassword, etPassword)

        fun submit() {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            var valid = true

            if (email.isEmpty()) { tilEmail.error = S.text(R.string.text_020); valid = false }
            else if (!Ui.isEmail(email)) { tilEmail.error = S.text(R.string.text_005); valid = false }
            if (password.isEmpty()) { tilPassword.error = S.text(R.string.text_020); valid = false }
            if (!valid) return

            AuthUi.run(this, btnLogin, {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).continueWithTask { result ->
                    if (!result.isSuccessful) throw result.exception ?: IllegalStateException()
                    CloudStore.ensureProfile()
                }
            }) {
                AuthSession.remember = findViewById<CheckBox>(R.id.cbRemember).isChecked
                CloudStore.start()
                CoinlyApp.applyTheme(UserPrefs(this).darkMode)
                startActivity(Intent(this, AccountNavigation.landing()).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
        }

        btnLogin.setOnClickListener { submit() }
        etPassword.setOnEditorActionListener { _, action, event ->
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_UP)) { submit(); true } else false
        }

        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java)
                .putExtra(ForgotPasswordActivity.EXTRA_EMAIL, etEmail.text.toString().trim()))
        }
        findViewById<TextView>(R.id.tvGoToRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }
}
