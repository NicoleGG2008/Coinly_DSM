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

        Ui.choices(acCurrency, Ui.CURRENCIES)
        if (savedInstanceState == null) etName.setText(prefs.name)
        etEmail.setText(prefs.email)
        tilEmail.helperText = S.text(R.string.email_verify_help)
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
            if (name.isEmpty()) { tilName.error = S.text(R.string.text_020); valid = false }
            if (!Ui.isEmail(email)) { tilEmail.error = S.text(R.string.text_005); valid = false }
            if (!valid) return

            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
            val emailChanged = email != user.email
            val profile = mapOf("name" to name, "email" to user.email.orEmpty(),
                "university" to etUniversity.text.toString().trim(),
                "career" to etCareer.text.toString().trim(),
                "currency" to acCurrency.text.toString().ifBlank { prefs.currency })
            AuthUi.run(this, findViewById(R.id.btnSaveProfile), {
                val saveProfile = {
                    com.example.coinly.data.CloudStore.write {
                        com.example.coinly.data.CloudStore.userRef().set(profile, com.google.firebase.firestore.SetOptions.merge())
                    }
                }
                if (emailChanged) user.verifyBeforeUpdateEmail(email).continueWithTask { result ->
                    if (!result.isSuccessful) throw result.exception ?: IllegalStateException()
                    saveProfile()
                } else saveProfile()
            }) {
                prefs.cacheProfile(profile)
                if (emailChanged) {
                    Ui.toast(findViewById(android.R.id.content), Tone.SUCCESS, S.text(R.string.email_verification_sent))
                    finish(); return@run
                }
                Ui.toast(findViewById(android.R.id.content), Tone.SUCCESS, S.text(R.string.text_025))
                finish()
            }
        }
        findViewById<MaterialButton>(R.id.btnSaveProfile).setOnClickListener { save() }
    }
}
