package com.example.coinly

import com.google.firebase.auth.FirebaseAuth

/** Presentation for the dedicated news account. Publishing still requires the server claim. */
object AccountNavigation {
    val newsAccount: Boolean
        get() = FirebaseAuth.getInstance().currentUser?.email
            ?.equals("coinlyadministrador@gmail.com", ignoreCase = true) == true

    fun landing() = if (newsAccount) NoticiasActivity::class.java else MainActivity::class.java
}
