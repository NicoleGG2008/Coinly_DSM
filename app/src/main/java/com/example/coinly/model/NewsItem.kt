package com.example.coinly.model

import androidx.annotation.DrawableRes

data class NewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val date: String,
    val category: String,
    val content: String,
    @DrawableRes val imageRes: Int
)

enum class EventType(val label: String) {
    ACADEMIC("Académico"), FINANCIAL("Financiero"), EVENT("Evento")
}

data class CalendarEvent(
    val id: String,
    val date: String, // yyyy-MM-dd
    val title: String,
    val type: EventType
)
