package com.example.coinly

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.example.coinly.data.UserPrefs
import com.example.coinly.ui.Ui
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setScreen(R.layout.activity_register)

        val tilName = findViewById<TextInputLayout>(R.id.tilNameRegister)
        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmailRegister)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPasswordRegister)
        val tilConfirm = findViewById<TextInputLayout>(R.id.tilConfirmRegister)
        val tilCurrency = findViewById<TextInputLayout>(R.id.tilCurrency)
        val etName = findViewById<TextInputEditText>(R.id.etNameRegister)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmailRegister)
        val etPassword = findViewById<TextInputEditText>(R.id.etPasswordRegister)
        val etConfirm = findViewById<TextInputEditText>(R.id.etConfirmRegister)
        val acCurrency = findViewById<MaterialAutoCompleteTextView>(R.id.acCurrency)

        acCurrency.setSimpleItems(Ui.CURRENCIES)
        Ui.clearErrorOnType(tilName, etName)
        Ui.clearErrorOnType(tilEmail, etEmail)
        Ui.clearErrorOnType(tilPassword, etPassword)
        Ui.clearErrorOnType(tilConfirm, etConfirm)
        acCurrency.setOnItemClickListener { _, _, _, _ -> tilCurrency.error = null }

        fun submit() {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirm = etConfirm.text.toString()
            val currency = acCurrency.text.toString().trim()
            var valid = true

            if (name.isEmpty()) { tilName.error = "Completa este campo."; valid = false }
            if (email.isEmpty()) { tilEmail.error = "Completa este campo."; valid = false }
            else if (!Ui.isEmail(email)) { tilEmail.error = "Ingresa un correo válido."; valid = false }
            if (password.isEmpty()) { tilPassword.error = "Completa este campo."; valid = false }
            else if (password.length < 6) { tilPassword.error = "Mínimo 6 caracteres."; valid = false }
            if (password != confirm) { tilConfirm.error = "Las contraseñas no coinciden."; valid = false }
            if (currency.isEmpty()) { tilCurrency.error = "Selecciona una moneda."; valid = false }
            if (!valid) return

            UserPrefs(this).register(name, email, password, currency)
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        findViewById<MaterialButton>(R.id.btnRegister).setOnClickListener { submit() }
        etConfirm.setOnEditorActionListener { _, _, _ -> submit(); true }

        findViewById<TextView>(R.id.tvGoToLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
