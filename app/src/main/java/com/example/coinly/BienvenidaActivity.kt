package com.example.coinly

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton

class BienvenidaActivity : BaseActivity() {

    private var isRevealed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_bienvenida)
        padForSystemBars(findViewById(R.id.welcomeRoot))

        val logoContainer = findViewById<FrameLayout>(R.id.logoContainer)
        val imgIcon = findViewById<ImageView>(R.id.imgCoinlyIcon)
        val imgLogo = findViewById<ImageView>(R.id.imgCoinlyLogo)
        val tvTapHint = findViewById<TextView>(R.id.tvTapHint)
        val density = resources.displayMetrics.density

        // Animación sutil de "respiración" del icono mientras no se ha revelado el logo
        imgIcon.animate().translationY(-6 * density).setDuration(1700).withEndAction(object : Runnable {
            override fun run() {
                if (isRevealed || !imgIcon.isAttachedToWindow) return
                val goingUp = imgIcon.translationY >= 0f
                imgIcon.animate().translationY(if (goingUp) -6 * density else 0f).setDuration(1700).withEndAction(this).start()
            }
        }).start()

        // Al tocar el logo se revela el logotipo completo de Coinly
        logoContainer.setOnClickListener {
            if (isRevealed) return@setOnClickListener
            isRevealed = true
            logoContainer.contentDescription = "Coinly"

            tvTapHint.animate().alpha(0f).setDuration(200).start()

            imgIcon.animate().cancel()
            imgIcon.animate()
                .translationY(0f)
                .translationX(-82 * density)
                .scaleX(0.7f).scaleY(0.7f)
                .alpha(0f)
                .setDuration(750)
                .start()

            imgLogo.translationX = 38 * density
            imgLogo.alpha = 0f
            imgLogo.visibility = View.VISIBLE
            imgLogo.animate().translationX(0f).alpha(1f).setDuration(720).start()
        }

        findViewById<MaterialButton>(R.id.btnRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
