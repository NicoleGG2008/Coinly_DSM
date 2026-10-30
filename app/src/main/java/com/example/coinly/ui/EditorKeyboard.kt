package com.example.coinly.ui

import android.graphics.Rect
import android.os.Build
import android.view.View

/** Older edge-to-edge windows can overlay the IME instead of resizing the editor. */
object EditorKeyboard {
    fun attach(content: View) {
        if (Build.VERSION.SDK_INT >= 30) return
        val visible = Rect()
        val location = IntArray(2)
        content.viewTreeObserver.addOnGlobalLayoutListener {
            content.getWindowVisibleDisplayFrame(visible)
            content.getLocationOnScreen(location)
            val covered = (location[1] + content.height - visible.bottom).coerceAtLeast(0)
            val bottom = if (covered > content.rootView.height / 6) covered else 0
            if (content.paddingBottom != bottom) {
                content.setPadding(content.paddingLeft, content.paddingTop, content.paddingRight, bottom)
            }
        }
    }
}
