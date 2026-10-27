package com.example.coinly
import android.content.Context
object S {
    lateinit var context: Context
    fun text(id: Int, vararg args: Any?): String = context.getString(id, *args)
}
