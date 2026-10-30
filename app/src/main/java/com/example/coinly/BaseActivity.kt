package com.example.coinly

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.coinly.data.UserPrefs
import com.example.coinly.ui.ScreenFrame

/**
 * Base común: dibujo de borde a borde con iconos claros sobre el azul de la marca
 * y escala de texto elegida por la persona en Perfil › Accesibilidad.
 */
open class BaseActivity : AppCompatActivity() {
    private var unsubscribe: (() -> Unit)? = null
    private var cloudStatus: android.widget.TextView? = null
    protected open fun onCloudChanged() {}
    private val publicScreen get() = this is BienvenidaActivity || this is LoginActivity ||
        this is RegisterActivity || this is ForgotPasswordActivity
    override fun onStart() {
        super.onStart()
        if (publicScreen) return
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
            startActivity(android.content.Intent(this, LoginActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish(); return
        }
        if (AccountNavigation.newsAccount && (this is MainActivity || this is MovimientosActivity ||
                this is PresupuestosActivity || this is EconomicEventActivity)) {
            startActivity(android.content.Intent(this, NoticiasActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish(); return
        }
        com.example.coinly.data.CloudStore.start()
        if (!BuildConfig.FIREBASE_EMULATOR) com.example.coinly.data.EconomicEventWork.configure(this, com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid)
        unsubscribe = com.example.coinly.data.CloudStore.observe {
            val store = com.example.coinly.data.CloudStore
            cloudStatus?.apply {
                text = when {
                    store.error != null -> store.error + " · " + "Reintentar"
                    store.pending > 0 -> S.text(R.string.text_010)
                    store.loading -> S.text(R.string.text_011)
                    store.cached -> S.text(R.string.text_012)
                    else -> ""
                }
                visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
            }
            onCloudChanged()
        }
    }
    override fun onStop() { unsubscribe?.invoke(); unsubscribe = null; super.onStop() }
    override fun setContentView(layoutResID: Int) { setContentView(layoutInflater.inflate(layoutResID, null)) }
    override fun setContentView(view: View?) {
        val content = requireNotNull(view)
        val column = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL }
        if (!publicScreen) {
            cloudStatus = android.widget.TextView(this).apply {
                setPadding(24, 16, 24, 16)
                androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
                    val top = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top
                    v.setPadding(24, top + 16, 24, 16)
                    insets
                }
                setTextColor(getColor(R.color.coinly_text_primary))
                setBackgroundResource(R.drawable.bg_banner_info)
                setOnClickListener { com.example.coinly.data.CloudStore.start(true) }
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            }
            column.addView(cloudStatus, android.widget.LinearLayout.LayoutParams(-1, -2))
        }
        column.addView(content, android.widget.LinearLayout.LayoutParams(-1, 0, 1f))
        val frame = android.widget.FrameLayout(this)
        frame.addView(column, android.widget.FrameLayout.LayoutParams(-1, -1, android.view.Gravity.CENTER))
        super.setContentView(frame)
        fun adapt() {
            val width = resources.displayMetrics.widthPixels
            val max = ((if (publicScreen) 600 else 1000) * resources.displayMetrics.density).toInt()
            column.layoutParams = android.widget.FrameLayout.LayoutParams(minOf(width, max), -1, android.view.Gravity.CENTER)
        }
        adapt()
        content.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val desired = minOf(resources.displayMetrics.widthPixels, ((if (publicScreen) 600 else 1000) * resources.displayMetrics.density).toInt())
            if (column.layoutParams.width != desired) adapt()
        }
    }


    override fun attachBaseContext(newBase: Context) {
        val factor = UserPrefs(newBase).fontFactor
        if (factor == 1.0f) {
            super.attachBaseContext(newBase)
            return
        }
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = config.fontScale * factor
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    /**
     * Mantiene el tamaño de letra elegido y, a la vez, respeta el modo claro/oscuro
     * que AppCompat aplica a la configuración (sin esto el tema oscuro pisaría la escala).
     */
    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (overrideConfiguration != null) {
            val uiMode = overrideConfiguration.uiMode
            overrideConfiguration.setTo(baseContext.resources.configuration)
            overrideConfiguration.uiMode = uiMode
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Arriba siempre hay azul de marca (iconos claros); abajo, iconos oscuros en claro y claros en oscuro
        val dark = UserPrefs(this).darkMode
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
            else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT < 26) window.navigationBarColor = getColor(R.color.coinly_brand_navy)
    }

    /** Infla [layout] dentro del marco que respeta las barras del sistema. */
    protected fun setScreen(@LayoutRes layout: Int) {
        setContentView(ScreenFrame.wrap(this, layoutInflater.inflate(layout, null)))
    }

    /** Para pantallas cuyo fondo ya cubre todo (Bienvenida): solo añade relleno por los insets. */
    protected fun padForSystemBars(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }
}
