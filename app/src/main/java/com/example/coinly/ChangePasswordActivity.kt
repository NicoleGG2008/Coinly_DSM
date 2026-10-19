package com.example.coinly

import android.content.Intent
import android.os.Bundle
import com.example.coinly.ui.ScreenFrame
import com.example.coinly.ui.StubScreen

/** PLACEHOLDER: se reemplaza al agregar la pantalla real. */
class ChangePasswordActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ScreenFrame.wrap(this, StubScreen.build(this, "Cambiar contraseña", withHomeButton = true)))
    }
}
