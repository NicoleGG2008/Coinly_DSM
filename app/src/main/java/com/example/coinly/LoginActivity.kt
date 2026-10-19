package com.example.coinly

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.example.coinly.data.UserPrefs
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
        val prefs = UserPrefs(this)

        Ui.clearErrorOnType(tilEmail, etEmail)
        Ui.clearErrorOnType(tilPassword, etPassword)

        fun submit() {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            var valid = true

            if (email.isEmpty()) { tilEmail.error = "Completa este campo."; valid = false }
            else if (!Ui.isEmail(email)) { tilEmail.error = "Ingresa un correo válido."; valid = false }
            if (password.isEmpty()) { tilPassword.error = "Completa este campo."; valid = false }
            if (!valid) return

            if (!prefs.login(email, password)) {
                tilPassword.error = "Correo o contraseña incorrectos."
                return
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Ingresando..."
            btnLogin.postDelayed({
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }, 500)
        }

        btnLogin.setOnClickListener { submit() }
        etPassword.setOnEditorActionListener { _, _, _ -> submit(); true }

        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
        findViewById<TextView>(R.id.tvGoToRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }
}
