package com.example.coinly

import android.os.Bundle
import android.view.View
import com.example.coinly.data.UserPrefs
import com.example.coinly.ui.Tone
import com.example.coinly.ui.Ui
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class EditProfileActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setScreen(R.layout.activity_edit_profile)

        val prefs = UserPrefs(this)
        val tilName = findViewById<TextInputLayout>(R.id.tilProfileName)
        val tilEmail = findViewById<TextInputLayout>(R.id.tilProfileEmail)
        val etName = findViewById<TextInputEditText>(R.id.etProfileName)
        val etEmail = findViewById<TextInputEditText>(R.id.etProfileEmail)
        val etUniversity = findViewById<TextInputEditText>(R.id.etProfileUniversity)
        val etCareer = findViewById<TextInputEditText>(R.id.etProfileCareer)
        val acCurrency = findViewById<MaterialAutoCompleteTextView>(R.id.acProfileCurrency)

        acCurrency.setSimpleItems(Ui.CURRENCIES)
        etName.setText(prefs.name)
        etEmail.setText(prefs.email)
        etUniversity.setText(prefs.university)
        etCareer.setText(prefs.career)
        acCurrency.setText(prefs.currency, false)
        Ui.clearErrorOnType(tilName, etName)
        Ui.clearErrorOnType(tilEmail, etEmail)

        findViewById<View>(R.id.btnBackEditProfile).setOnClickListener { finish() }

        fun save() {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            var valid = true
            if (name.isEmpty()) { tilName.error = "Completa este campo."; valid = false }
            if (!Ui.isEmail(email)) { tilEmail.error = "Ingresa un correo válido."; valid = false }
            if (!valid) return

            prefs.name = name
            prefs.email = email
            prefs.university = etUniversity.text.toString().trim()
            prefs.career = etCareer.text.toString().trim()
            prefs.currency = acCurrency.text.toString().ifBlank { prefs.currency }
            Ui.toast(findViewById(android.R.id.content), Tone.SUCCESS, "Perfil actualizado correctamente.")
            findViewById<View>(R.id.btnSaveProfile).postDelayed({ finish() }, 900)
        }
        findViewById<MaterialButton>(R.id.btnSaveProfile).setOnClickListener { save() }
    }
}
