package com.example.coinly

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.example.coinly.ui.Ui
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : BaseActivity() {
    private var confirmation: androidx.appcompat.app.AlertDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setScreen(R.layout.activity_change_password)
        val model = ViewModelProvider(this)[PasswordActionsViewModel::class.java]
        val tilCurrent = findViewById<TextInputLayout>(R.id.tilCurrentPassword)
        val tilNew = findViewById<TextInputLayout>(R.id.tilNewPassword)
        val tilConfirm = findViewById<TextInputLayout>(R.id.tilConfirmNewPassword)
        val current = findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val next = findViewById<TextInputEditText>(R.id.etNewPassword)
        val confirm = findViewById<TextInputEditText>(R.id.etConfirmNewPassword)
        val error = findViewById<TextView>(R.id.tvChangePasswordError)
        val save = findViewById<MaterialButton>(R.id.btnSavePassword)
        val reset = findViewById<View>(R.id.btnResetFromChange)
        Ui.clearErrorOnType(tilCurrent, current)
        Ui.clearErrorOnType(tilNew, next)
        Ui.clearErrorOnType(tilConfirm, confirm)
        findViewById<View>(R.id.btnBackChangePassword).setOnClickListener { finish() }
        reset.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java)
                .putExtra(ForgotPasswordActivity.EXTRA_EMAIL, FirebaseAuth.getInstance().currentUser?.email.orEmpty()))
        }
        fun change() {
            if (model.state.value?.busy == true || model.state.value?.passwordChanged == true) return
            tilCurrent.error = null; tilNew.error = null; tilConfirm.error = null
            error.visibility = View.GONE
            var valid = true
            if (current.text.isNullOrEmpty()) { tilCurrent.error = getString(R.string.text_020); valid = false }
            if (next.text.toString().length < 6) { tilNew.error = getString(R.string.text_021); valid = false }
            else if (next.text.toString() == current.text.toString()) { tilNew.error = getString(R.string.password_must_differ); valid = false }
            if (next.text.toString() != confirm.text.toString()) { tilConfirm.error = getString(R.string.text_022); valid = false }
            if (valid) model.changePassword(current.text.toString(), next.text.toString())
        }
        save.setOnClickListener { change() }
        confirm.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_UP)) {
                change(); true
            } else false
        }
        model.state.observe(this) { state ->
            listOf<View>(current, next, confirm, reset, save).forEach { it.isEnabled = !state.busy && !state.passwordChanged }
            save.setText(if (state.busy) R.string.password_changing else R.string.text_276)
            val reason = state.errorRes
            if (reason != null) when (state.errorField) {
                PasswordErrorField.CURRENT -> { tilCurrent.error = getString(reason); current.requestFocus() }
                PasswordErrorField.NEW -> { tilNew.error = getString(reason); next.requestFocus() }
                else -> { error.setText(reason); error.visibility = View.VISIBLE }
            }
            if (state.passwordChanged && confirmation?.isShowing != true) {
                current.text?.clear(); next.text?.clear(); confirm.text?.clear()
                confirmation = MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.password_changed_title)
                    .setMessage(R.string.password_changed_message)
                    .setPositiveButton(R.string.password_done) { _, _ -> finish() }
                    .setCancelable(false).show()
            }
        }
    }
    override fun onDestroy() { confirmation?.dismiss(); confirmation = null; super.onDestroy() }
}
