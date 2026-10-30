package com.example.coinly

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.TextViewCompat
import com.example.coinly.ui.CoinlyAiChatSheet
import com.example.coinly.ui.ScreenFrame
import com.google.android.material.card.MaterialCardView

object BottomNav {

    private class Item(val label: String, val icon: Int, val target: Class<out Activity>)

    private val items = listOf(
        Item(S.text(R.string.text_013), R.drawable.ic_home, MainActivity::class.java),
        Item(S.text(R.string.text_014), R.drawable.ic_dollar, MovimientosActivity::class.java),
        Item(S.text(R.string.text_015), R.drawable.ic_pie, PresupuestosActivity::class.java),
        Item(S.text(R.string.text_016), R.drawable.ic_news, NoticiasActivity::class.java),
        Item(S.text(R.string.text_017), R.drawable.ic_user, PerfilActivity::class.java)
    )

    fun wrap(activity: Activity, content: View, selected: String): View {
        val bar = LinearLayout(activity).apply {
            id = R.id.nav_bar
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_nav_bar)
            elevation = dp(activity, 8).toFloat()
        }
        val teal = ContextCompat.getColor(activity, R.color.coinly_accent)
        val muted = ContextCompat.getColor(activity, R.color.coinly_text_secondary)

        val visibleItems = if (AccountNavigation.newsAccount) items.filter {
            it.target == NoticiasActivity::class.java || it.target == PerfilActivity::class.java
        } else items
        visibleItems.forEach { item ->
            val isSelected = item.label == selected
            val color = if (isSelected) teal else muted

            val cell = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                minimumHeight = dp(activity, 64)
                setBackgroundResource(R.drawable.bg_nav_item)
                isClickable = true
                isFocusable = true
                contentDescription = item.label
                ViewCompat.setStateDescription(this, if (isSelected) S.text(R.string.text_018) else null)
            }

            // Indicador superior de la pestaña activa
            val indicator = View(activity).apply {
                setBackgroundResource(R.drawable.bg_tab_indicator)
                backgroundTintList = ColorStateList.valueOf(teal)
                visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            cell.addView(indicator, LinearLayout.LayoutParams(dp(activity, 32), dp(activity, 3)))

            val icon = ImageView(activity).apply {
                setImageResource(item.icon)
                imageTintList = ColorStateList.valueOf(color)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            cell.addView(icon, LinearLayout.LayoutParams(dp(activity, 24), dp(activity, 24)).apply { topMargin = dp(activity, 8) })

            val label = TextView(activity).apply {
                text = item.label
                setTextColor(color)
                setTypeface(typeface, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                maxLines = 2
                gravity = Gravity.CENTER
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                // Ajusta el tamaño para que "Presupuestos" quepa en cualquier pantalla
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            }
            cell.addView(label, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(activity, 2)
                bottomMargin = dp(activity, 8)
                marginStart = dp(activity, 2)
                marginEnd = dp(activity, 2)
            })

            cell.setOnClickListener {
                if (activity::class.java != item.target) {
                    activity.startActivity(Intent(activity, item.target))
                    activity.finish()
                }
            }
            bar.addView(cell, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        // Contenedor principal con el botón flotante Coinly AI
        val contentContainer = FrameLayout(activity).apply {
            addView(
                content,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            // Botón flotante FAB para Coinly AI con Gemini
            val fabSize = dp(activity, 58)
            val fab = MaterialCardView(activity).apply {
                radius = dp(activity, 29).toFloat()
                cardElevation = dp(activity, 8).toFloat()
                setCardBackgroundColor(ContextCompat.getColor(activity, R.color.coinly_brand_navy))
                strokeColor = ContextCompat.getColor(activity, R.color.coinly_accent)
                strokeWidth = dp(activity, 2)
                isClickable = true
                isFocusable = true
                contentDescription = S.text(R.string.text_019)

                val logoIcon = ImageView(activity).apply {
                    setImageResource(R.drawable.coinly_icon)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10))
                }
                addView(
                    logoIcon,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )

                setOnClickListener {
                    CoinlyAiChatSheet.show(activity)
                }
            }

            val fabParams = FrameLayout.LayoutParams(fabSize, fabSize).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                marginEnd = dp(activity, 16)
                bottomMargin = dp(activity, 16)
            }
            if (!AccountNavigation.newsAccount) addView(fab, fabParams)
        }

        return ScreenFrame.wrap(activity, contentContainer, bar)
    }

    private fun dp(activity: Activity, value: Int) = (value * activity.resources.displayMetrics.density).toInt()
}
