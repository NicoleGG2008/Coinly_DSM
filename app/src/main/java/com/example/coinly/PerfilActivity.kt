package com.example.coinly

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.example.coinly.data.UserPrefs
import com.google.android.material.materialswitch.MaterialSwitch
import com.example.coinly.ui.Tone
import com.example.coinly.ui.Ui
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class PerfilActivity : BaseActivity() {

    private lateinit var prefs: UserPrefs

    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            prefs.avatarUri = uri.toString()
            renderAvatar()
            Ui.toast(findViewById(android.R.id.content), Tone.SUCCESS, S.text(R.string.text_052))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(BottomNav.wrap(this, layoutInflater.inflate(R.layout.activity_perfil, null), S.text(R.string.text_017)))
        prefs = UserPrefs(this)

        findViewById<View>(R.id.btnChangeAvatar).setOnClickListener { pickImage.launch(arrayOf("image/*")) }
        findViewById<View>(R.id.btnEditProfile).setOnClickListener { startActivity(Intent(this, EditProfileActivity::class.java)) }
        findViewById<View>(R.id.btnChangePassword).setOnClickListener { startActivity(Intent(this, ChangePasswordActivity::class.java)) }
        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            com.example.coinly.data.AuthSession.logout()
            CoinlyApp.applyTheme(false)
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        // Tema oscuro: se aplica a toda la app y se recuerda entre sesiones
        val themeSwitch = findViewById<MaterialSwitch>(R.id.switchDarkTheme)
        val themeStatus = findViewById<TextView>(R.id.tvThemeStatus)
        themeSwitch.isChecked = prefs.darkMode
        themeStatus.text = if (prefs.darkMode) S.text(R.string.text_053) else S.text(R.string.text_054)
        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.darkMode = isChecked
            themeStatus.text = if (isChecked) S.text(R.string.text_053) else S.text(R.string.text_054)
            CoinlyApp.applyTheme(isChecked) // recrea la pantalla con el tema nuevo
        }
        findViewById<View>(R.id.rowDarkTheme).setOnClickListener { themeSwitch.toggle() }

        // Tamaño de letra: se marca el valor actual y luego se escuchan los cambios
        val toggle = findViewById<MaterialButtonToggleGroup>(R.id.toggleFont)
        toggle.check(
            when {
                prefs.fontFactor < 1f -> R.id.btnFontSmall
                prefs.fontFactor > 1f -> R.id.btnFontLarge
                else -> R.id.btnFontMedium
            }
        )
        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            prefs.fontFactor = when (checkedId) {
                R.id.btnFontSmall -> 0.9f
                R.id.btnFontLarge -> 1.15f
                else -> 1.0f
            }
            recreate()
        }
    }

    override fun onResume() {
        super.onResume()
        bindProfile()
    }

    override fun onCloudChanged() { bindProfile() }

    private fun bindProfile() {
        findViewById<TextView>(R.id.tvProfileName).text = prefs.name
        findViewById<TextView>(R.id.tvProfileEmail).text = prefs.email
        findViewById<TextView>(R.id.tvProfileCareerUni).text = S.text(R.string.profile_study, prefs.career, prefs.university)
        findViewById<TextView>(R.id.tvInfoName).text = prefs.name
        findViewById<TextView>(R.id.tvInfoEmail).text = prefs.email
        findViewById<TextView>(R.id.tvInfoUniversity).text = prefs.university
        findViewById<TextView>(R.id.tvInfoCareer).text = prefs.career
        findViewById<TextView>(R.id.tvInfoCurrency).text = prefs.currency
        findViewById<TextView>(R.id.tvAvatarInitial).text = prefs.initial
        renderAvatar()
    }

    private fun renderAvatar() {
        val photo = findViewById<ImageView>(R.id.ivAvatarPhoto)
        val bitmap = prefs.avatarUri?.let { loadBitmap(Uri.parse(it)) }
        if (bitmap != null) {
            photo.setImageBitmap(bitmap)
            photo.visibility = View.VISIBLE
        } else {
            photo.visibility = View.GONE
        }
    }

    /** Decodifica reducida (≤ 512 px) para no gastar memoria con fotos grandes. */
    private fun loadBitmap(uri: Uri): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 512) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()
}
