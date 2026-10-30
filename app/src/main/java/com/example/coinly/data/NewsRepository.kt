package com.example.coinly.data
import com.example.coinly.S

import com.example.coinly.R
import com.example.coinly.model.CalendarEvent
import com.example.coinly.model.EventType
import com.example.coinly.model.NewsItem

object NewsRepository {

    val categories = listOf("Todas", "Ahorro", S.text(R.string.text_083), S.text(R.string.text_084), S.text(R.string.text_085))

    private val seedNews = listOf(
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

    private val seedEvents = listOf(
        CalendarEvent("1", "2026-08-15", S.text(R.string.text_102), EventType.ACADEMIC),
        CalendarEvent("2", "2026-08-20", S.text(R.string.text_103), EventType.FINANCIAL),
        CalendarEvent("3", "2026-08-22", S.text(R.string.text_104), EventType.ACADEMIC),
        CalendarEvent("4", "2026-08-28", S.text(R.string.text_105), EventType.EVENT),
        CalendarEvent("5", "2026-09-01", S.text(R.string.text_106), EventType.FINANCIAL),
        CalendarEvent("6", "2026-09-05", S.text(R.string.text_107), EventType.EVENT),
        CalendarEvent("7", "2026-09-10", S.text(R.string.text_108), EventType.ACADEMIC),
        CalendarEvent("8", "2026-09-15", S.text(R.string.text_109), EventType.FINANCIAL)
    )

    private var remoteNews: List<NewsItem> = emptyList()
    private var remoteEvents: List<CalendarEvent> = emptyList()
    val news get() = (remoteNews + seedNews).distinctBy { it.id }
    val events get() = (remoteEvents + seedEvents).distinctBy { it.id }

    fun image(key: String): Int = when (key) {
        "presupuesto" -> R.drawable.img_news_presupuesto
        "inversion" -> R.drawable.img_news_inversion
        "becas" -> R.drawable.img_news_becas
        else -> R.drawable.img_news_ahorro
    }
    fun fromDocument(doc: com.google.firebase.firestore.DocumentSnapshot): NewsItem? = runCatching {
        val date = doc.getString("date").orEmpty()
        if (!doc.exists() || doc.getBoolean("published") != true || !NewsDates.valid(date)) return@runCatching null
        NewsItem(doc.id, doc.getString("title").orEmpty(), doc.getString("summary").orEmpty(),
            com.example.coinly.ui.Ui.longDate(date), doc.getString("category").orEmpty(), doc.getString("content").orEmpty(), image(doc.getString("imageKey").orEmpty()))
    }.getOrNull()

    fun subscribe(onChange: (Boolean) -> Unit): () -> Unit {
        val registration = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("noticias")
            .whereEqualTo("published", true).addSnapshotListener { snap, error ->
                if (snap != null) {
                    remoteNews = snap.documents.sortedByDescending { it.getString("date").orEmpty() }.mapNotNull(::fromDocument)
                    remoteEvents = snap.documents.mapNotNull { doc ->
                        val date = doc.getString("eventDate").orEmpty()
                        if (NewsDates.valid(date)) CalendarEvent("news-" + doc.id, date, doc.getString("title").orEmpty(), EventType.FINANCIAL) else null
                    }
                }
                onChange(error != null)
            }
        return { registration.remove() }
    }

    fun byId(id: String) = news.firstOrNull { it.id == id }
}
