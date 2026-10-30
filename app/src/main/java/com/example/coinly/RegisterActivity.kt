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
        tilPassword.helperText = getString(R.string.password_requirements)
        val acCurrency = findViewById<MaterialAutoCompleteTextView>(R.id.acCurrency)

        Ui.choices(acCurrency, Ui.CURRENCIES)
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

            if (name.isEmpty()) { tilName.error = S.text(R.string.text_020); valid = false }
            if (email.isEmpty()) { tilEmail.error = S.text(R.string.text_020); valid = false }
            else if (!Ui.isEmail(email)) { tilEmail.error = S.text(R.string.text_005); valid = false }
            if (password.isEmpty()) { tilPassword.error = S.text(R.string.text_020); valid = false }
            else if (!PasswordPolicy.valid(password)) { tilPassword.error = S.text(R.string.password_requirements); valid = false }
            if (password != confirm) { tilConfirm.error = S.text(R.string.text_022); valid = false }
            if (currency.isEmpty()) { tilCurrency.error = S.text(R.string.text_067); valid = false }
            if (!valid) return

            AuthUi.run(this, findViewById(R.id.btnRegister), {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                auth.createUserWithEmailAndPassword(email, password).continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: IllegalStateException()
                    com.example.coinly.data.AuthSession.remember = false
                    val profile = mapOf("name" to name, "email" to email, "currency" to currency,
                        "university" to "", "career" to "")
                    UserPrefs(this).cacheProfile(profile)
                    com.example.coinly.data.CloudStore.userRef().set(profile)
                }
            }) {
                com.example.coinly.data.CloudStore.start(true)
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
        }

        findViewById<MaterialButton>(R.id.btnRegister).setOnClickListener { submit() }
        etConfirm.setOnEditorActionListener { _, action, event ->
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_UP)) { submit(); true } else false
        }

        findViewById<TextView>(R.id.tvGoToLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
