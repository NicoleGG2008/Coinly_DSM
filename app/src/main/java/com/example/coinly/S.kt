package com.example.coinly
import android.content.Context
object S {
    lateinit var context: Context
    fun text(id: Int, vararg args: Any?): String =
        if (args.isEmpty()) context.getString(id) else context.getString(id, *args)
}
