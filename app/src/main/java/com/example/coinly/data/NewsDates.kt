package com.example.coinly.data

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

object NewsDates {
    fun valid(value: String): Boolean {
        if (!value.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))) return false
        val position = ParsePosition(0)
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }.parse(value, position)
        return parsed != null && position.index == value.length
    }
}
