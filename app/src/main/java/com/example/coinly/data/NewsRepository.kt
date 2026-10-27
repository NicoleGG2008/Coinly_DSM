package com.example.coinly.data
import com.example.coinly.S

import com.example.coinly.R
import com.example.coinly.model.CalendarEvent
import com.example.coinly.model.EventType
import com.example.coinly.model.NewsItem

object NewsRepository {

    val categories = listOf("Todas", "Ahorro", S.text(R.string.text_083), S.text(R.string.text_084), S.text(R.string.text_085))

    val news = listOf(
        NewsItem("1", S.text(R.string.text_086),
            S.text(R.string.text_087),
            S.text(R.string.text_088), "Ahorro",
            S.text(R.string.text_089),
            R.drawable.img_news_ahorro),
        NewsItem("2", S.text(R.string.text_090),
            S.text(R.string.text_091),
            S.text(R.string.text_092), S.text(R.string.text_083),
            S.text(R.string.text_093),
            R.drawable.img_news_presupuesto),
        NewsItem("3", S.text(R.string.text_094),
            S.text(R.string.text_095),
            S.text(R.string.text_096), S.text(R.string.text_084),
            S.text(R.string.text_097),
            R.drawable.img_news_inversion),
        NewsItem("4", S.text(R.string.text_098),
            S.text(R.string.text_099),
            S.text(R.string.text_100), S.text(R.string.text_085),
            S.text(R.string.text_101),
            R.drawable.img_news_becas)
    )

    val events = listOf(
        CalendarEvent("1", "2026-08-15", S.text(R.string.text_102), EventType.ACADEMIC),
        CalendarEvent("2", "2026-08-20", S.text(R.string.text_103), EventType.FINANCIAL),
        CalendarEvent("3", "2026-08-22", S.text(R.string.text_104), EventType.ACADEMIC),
        CalendarEvent("4", "2026-08-28", S.text(R.string.text_105), EventType.EVENT),
        CalendarEvent("5", "2026-09-01", S.text(R.string.text_106), EventType.FINANCIAL),
        CalendarEvent("6", "2026-09-05", S.text(R.string.text_107), EventType.EVENT),
        CalendarEvent("7", "2026-09-10", S.text(R.string.text_108), EventType.ACADEMIC),
        CalendarEvent("8", "2026-09-15", S.text(R.string.text_109), EventType.FINANCIAL)
    )

    fun byId(id: String) = news.firstOrNull { it.id == id }
}
