package com.example.coinly.ui
import com.example.coinly.S

import android.app.Activity
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.coinly.MainActivity
import com.example.coinly.R
import com.google.android.material.button.MaterialButton

/** Pantalla provisional: se reemplaza cuando se agrega la pantalla real en un commit posterior. */
object StubScreen {
    private fun dp(activity: Activity, v: Int) = (v * activity.resources.displayMetrics.density).toInt()

    fun build(activity: Activity, title: String, withHomeButton: Boolean = false): View {
        val box = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.color.coinly_surface)
            setPadding(dp(activity, 32), dp(activity, 32), dp(activity, 32), dp(activity, 32))
        }
        box.addView(TextView(activity).apply {
            text = title
            gravity = Gravity.CENTER
            setTextAppearance(R.style.TextAppearance_Coinly_Heading)
        })
        box.addView(TextView(activity).apply {
            text = S.text(R.string.text_129)
            gravity = Gravity.CENTER
            setPadding(0, dp(activity, 8), 0, dp(activity, 24))
            setTextAppearance(R.style.TextAppearance_Coinly_Muted)
        })
        if (withHomeButton) {
            box.addView(MaterialButton(activity).apply {
                text = S.text(R.string.text_130)
                setOnClickListener {
                    activity.startActivity(Intent(activity, MainActivity::class.java))
                    activity.finish()
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        return box
    }
}
