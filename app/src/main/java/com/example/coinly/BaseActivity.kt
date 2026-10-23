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
import com.example.coinly.ui.CoinlyAiChatSheet
import com.example.coinly.ui.ScreenFrame

/**
 * Base común: dibujo de borde a borde con iconos claros sobre el azul de la marca
 * y escala de texto elegida por la persona en Perfil › Accesibilidad.
 * Conserva el estado de Coinly AI Chat al rotar la pantalla.
 */
open class BaseActivity : AppCompatActivity() {

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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_CHAT_WAS_OPEN, CoinlyAiChatSheet.isShowing)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState.getBoolean(KEY_CHAT_WAS_OPEN, false)) {
            CoinlyAiChatSheet.show(this)
        }
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

    companion object {
        private const val KEY_CHAT_WAS_OPEN = "coinly_chat_was_open"
    }
}
