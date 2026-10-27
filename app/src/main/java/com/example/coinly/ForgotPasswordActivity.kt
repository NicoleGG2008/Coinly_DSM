package com.example.coinly

import android.os.Bundle
import android.view.View
import com.example.coinly.ui.Ui
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ForgotPasswordActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setScreen(R.layout.activity_forgot_password)

        val til = findViewById<TextInputLayout>(R.id.tilEmailForgot)
        val et = findViewById<TextInputEditText>(R.id.etEmailForgot)
        Ui.clearErrorOnType(til, et)

        findViewById<View>(R.id.btnBackForgot).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnBackToLogin).setOnClickListener { finish() }

        fun send() {
            if (!Ui.isEmail(et.text.toString().trim())) {
                til.error = S.text(R.string.text_005)
                return
            }
            AuthUi.run(this, findViewById(R.id.btnSendReset), {
                com.google.firebase.auth.FirebaseAuth.getInstance().sendPasswordResetEmail(et.text.toString().trim())
            }) {
                findViewById<View>(R.id.layoutForgotForm).visibility = View.GONE
                findViewById<View>(R.id.layoutForgotSent).visibility = View.VISIBLE
            }
        }
        findViewById<MaterialButton>(R.id.btnSendReset).setOnClickListener { send() }
        et.setOnEditorActionListener { _, _, _ -> send(); true }
    }
}
