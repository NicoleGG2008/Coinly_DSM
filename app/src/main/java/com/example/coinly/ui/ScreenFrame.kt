package com.example.coinly.ui

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.coinly.R

/**
 * Marco que coloca el contenido entre la barra de estado (azul marino de la marca)
 * y la barra de navegación del sistema, respetando recortes de pantalla y teclado.
 * Necesario porque la app dibuja de borde a borde (edge-to-edge).
 */
object ScreenFrame {
    fun wrap(context: Context, content: View, bar: View? = null, handleIme: Boolean = bar == null): View {
        val navy = ContextCompat.getColor(context, R.color.coinly_brand_navy)
        val surface = ContextCompat.getColor(context, R.color.coinly_surface)
        val white = ContextCompat.getColor(context, R.color.coinly_card)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(surface)
        }
        val topSpacer = View(context).apply { setBackgroundColor(navy) }
        val bottomSpacer = View(context).apply { setBackgroundColor(if (bar != null) white else surface) }

        root.addView(topSpacer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0))
        root.addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        if (bar != null) root.addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        root.addView(bottomSpacer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0))

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, 0, bars.right, 0)
            topSpacer.layoutParams.height = bars.top
            bottomSpacer.layoutParams.height = if (handleIme) maxOf(bars.bottom, ime.bottom) else bars.bottom
            topSpacer.requestLayout()
            bottomSpacer.requestLayout()
            WindowInsetsCompat.CONSUMED
        }
        return root
    }
}
