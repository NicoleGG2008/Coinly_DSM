package com.example.coinly

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import com.example.coinly.data.NewsRepository
import com.example.coinly.model.CalendarEvent
import com.example.coinly.model.EventType
import com.example.coinly.ui.Ui
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.Calendar

class NoticiasActivity : BaseActivity() {

    private var category = "Todas"
    private var year = 2026
    private var month = 7
    private var selectedDay: Int? = null

    private lateinit var tabNews: TextView
    private lateinit var tabCalendar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(BottomNav.wrap(this, layoutInflater.inflate(R.layout.activity_noticias, null), S.text(R.string.text_016)))

        tabNews = findViewById(R.id.btnTabNews)
        tabCalendar = findViewById(R.id.btnTabCalendar)
        tabNews.setOnClickListener { showTab(news = true) }
        tabCalendar.setOnClickListener { showTab(news = false) }

        setupCategoryChips()
        renderNews()

        // El calendario abre en el mes del próximo evento
        val today = Ui.todayIso()
        val first = NewsRepository.events.filter { it.date >= today }.minByOrNull { it.date } ?: NewsRepository.events.first()
        first.date.split("-").let { year = it[0].toInt(); month = it[1].toInt() - 1 }
        setupCalendar()
        showTab(news = true)
    }

    private fun showTab(news: Boolean) {
        findViewById<View>(R.id.layoutNewsSection).visibility = if (news) View.VISIBLE else View.GONE
        findViewById<View>(R.id.layoutCalendarSection).visibility = if (news) View.GONE else View.VISIBLE
        findViewById<View>(R.id.indicatorNews).visibility = if (news) View.VISIBLE else View.INVISIBLE
        findViewById<View>(R.id.indicatorCalendar).visibility = if (news) View.INVISIBLE else View.VISIBLE
        tabNews.setTextColor(getColor(if (news) R.color.coinly_on_dark else R.color.coinly_on_dark_muted))
        tabCalendar.setTextColor(getColor(if (news) R.color.coinly_on_dark_muted else R.color.coinly_on_dark))
        ViewCompat.setStateDescription(tabNews, if (news) S.text(R.string.text_018) else null)
        ViewCompat.setStateDescription(tabCalendar, if (news) null else S.text(R.string.text_018))
    }

    // ---------------- Noticias ----------------
    private fun setupCategoryChips() {
        val group = findViewById<ChipGroup>(R.id.chipGroupNews)
        NewsRepository.categories.forEachIndexed { i, name ->
            group.addView(Chip(this).apply {
                id = View.generateViewId()
                text = name
                tag = name
                isCheckable = true
                isChecked = i == 0
            })
        }
        group.setOnCheckedStateChangeListener { g, ids ->
            category = ids.firstOrNull()?.let { g.findViewById<View>(it)?.tag as? String } ?: "Todas"
            renderNews()
        }
    }

    private fun renderNews() {
        val list = findViewById<LinearLayout>(R.id.layoutNewsList)
        list.removeAllViews()
        NewsRepository.news.filter { category == "Todas" || it.category == category }.forEach { n ->
            val card = layoutInflater.inflate(R.layout.item_news_card, list, false)
            card.findViewById<ImageView>(R.id.ivNewsImage).setImageResource(n.imageRes)
            card.findViewById<TextView>(R.id.tvNewsCategory).text = n.category
            card.findViewById<TextView>(R.id.tvNewsTitle).text = n.title
            card.findViewById<TextView>(R.id.tvNewsSummary).text = n.summary
            card.findViewById<TextView>(R.id.tvNewsDate).text = n.date
            card.contentDescription = "${n.category}. ${n.title}. ${n.date}"
            card.setOnClickListener {
                startActivity(Intent(this, NoticiaDetalleActivity::class.java).putExtra(NoticiaDetalleActivity.EXTRA_ID, n.id))
            }
            list.addView(card)
        }
        if (list.childCount == 0) list.addView(TextView(this).apply {
            text = S.text(R.string.text_039)
            gravity = Gravity.CENTER
            setPadding(Ui.dp(context, 20), Ui.dp(context, 32), Ui.dp(context, 20), Ui.dp(context, 32))
            setTextAppearance(R.style.TextAppearance_Coinly_Muted)
            setBackgroundResource(R.drawable.bg_card_container)
        })
    }

    // ---------------- Calendario ----------------
    private val weekdays = listOf("Dom" to "domingo", "Lun" to "lunes", S.text(R.string.text_040) to "martes", S.text(R.string.text_041) to S.text(R.string.text_042), "Jue" to "jueves", "Vie" to "viernes", S.text(R.string.text_043) to S.text(R.string.text_044))

    private fun setupCalendar() {
        val header = findViewById<LinearLayout>(R.id.layoutWeekdays)
        weekdays.forEach { (short, full) ->
            header.addView(TextView(this).apply {
                text = short
                contentDescription = full
                gravity = Gravity.CENTER
                setTextAppearance(R.style.TextAppearance_Coinly_Overline)
                setPadding(0, Ui.dp(context, 8), 0, Ui.dp(context, 8))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        findViewById<View>(R.id.btnPrevMonth).setOnClickListener { shiftMonth(-1) }
        findViewById<View>(R.id.btnNextMonth).setOnClickListener { shiftMonth(1) }
        renderCalendar()
    }

    private fun shiftMonth(delta: Int) {
        val cal = Calendar.getInstance().apply { set(year, month, 1); add(Calendar.MONTH, delta) }
        year = cal.get(Calendar.YEAR); month = cal.get(Calendar.MONTH); selectedDay = null
        renderCalendar()
    }

    private fun eventColor(type: EventType) = when (type) {
        EventType.FINANCIAL -> R.color.coinly_green
        EventType.ACADEMIC -> R.color.coinly_accent
        EventType.EVENT -> R.color.coinly_heading
    }

    private fun eventTextColor(type: EventType) = when (type) {
        EventType.FINANCIAL -> R.color.coinly_green_text
        EventType.ACADEMIC -> R.color.coinly_accent
        EventType.EVENT -> R.color.coinly_heading
    }

    private fun eventTint(type: EventType) = when (type) {
        EventType.FINANCIAL -> R.color.coinly_tint_success
        EventType.ACADEMIC -> R.color.coinly_tint_info
        EventType.EVENT -> R.color.coinly_tint_neutral
    }

    private fun eventsOfMonth(): List<CalendarEvent> = NewsRepository.events.filter {
        val p = it.date.split("-"); p[0].toInt() == year && p[1].toInt() - 1 == month
    }.sortedBy { it.date }

    private fun dayOf(e: CalendarEvent) = e.date.split("-")[2].toInt()

    private fun renderCalendar() {
        findViewById<TextView>(R.id.tvMonthTitle).text = Ui.monthName(month).replaceFirstChar { it.uppercase() } + " " + year

        val monthEvents = eventsOfMonth()
        val grid = findViewById<LinearLayout>(R.id.layoutCalendarGrid)
        grid.removeAllViews()

        val first = Calendar.getInstance().apply { set(year, month, 1) }
        val offset = first.get(Calendar.DAY_OF_WEEK) - 1
        val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        val now = Calendar.getInstance()
        val isThisMonth = now.get(Calendar.YEAR) == year && now.get(Calendar.MONTH) == month

        val weeks = (offset + days + 6) / 7
        for (w in 0 until weeks) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (d in 0 until 7) {
                val day = w * 7 + d - offset + 1
                row.addView(
                    if (day in 1..days) dayCell(day, monthEvents.filter { dayOf(it) == day }, isThisMonth && now.get(Calendar.DAY_OF_MONTH) == day)
                    else View(this),
                    LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f)
                )
            }
            grid.addView(row)
        }
        renderEvents(monthEvents)
    }

    private fun dayCell(day: Int, events: List<CalendarEvent>, isToday: Boolean): View {
        val selected = selectedDay == day
        val cell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_nav_item)
            contentDescription = S.text(R.string.text_045, day, Ui.monthName(month)) +
                (if (events.isEmpty()) "" else ", ${events.size} ${if (events.size == 1) "evento" else "eventos"}") +
                (if (isToday) S.text(R.string.text_046) else "")
            ViewCompat.setStateDescription(this, if (selected) S.text(R.string.text_018) else null)
            setOnClickListener { selectedDay = if (selected) null else day; renderCalendar() }
        }
        val number = TextView(this).apply {
            text = day.toString()
            gravity = Gravity.CENTER
            textSize = 15f
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            when {
                isToday -> { setBackgroundResource(R.drawable.bg_calendar_today); setTextColor(getColor(R.color.coinly_on_dark)); setTypeface(typeface, android.graphics.Typeface.BOLD) }
                selected -> { setBackgroundResource(R.drawable.bg_calendar_selected); setTextColor(getColor(R.color.coinly_accent)); setTypeface(typeface, android.graphics.Typeface.BOLD) }
                else -> setTextColor(getColor(R.color.coinly_text_primary))
            }
        }
        cell.addView(number, LinearLayout.LayoutParams(Ui.dp(this, 34), Ui.dp(this, 34)).apply { topMargin = Ui.dp(this@NoticiasActivity, 3) })

        val dots = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        events.take(2).forEach { e ->
            dots.addView(View(this).apply {
                setBackgroundResource(R.drawable.bg_dot)
                backgroundTintList = ColorStateList.valueOf(getColor(eventColor(e.type)))
            }, LinearLayout.LayoutParams(Ui.dp(this, 6), Ui.dp(this, 6)).apply { marginStart = Ui.dp(this@NoticiasActivity, 1); marginEnd = Ui.dp(this@NoticiasActivity, 1); topMargin = Ui.dp(this@NoticiasActivity, 2) })
        }
        cell.addView(dots)
        return cell
    }

    private fun renderEvents(monthEvents: List<CalendarEvent>) {
        val list = findViewById<LinearLayout>(R.id.layoutEventsList)
        list.removeAllViews()
        val day = selectedDay
        val shown = if (day == null) monthEvents else monthEvents.filter { dayOf(it) == day }
        findViewById<TextView>(R.id.tvEventsTitle).text =
            if (day == null) S.text(R.string.text_047, Ui.monthName(month)) else S.text(R.string.text_048, day, Ui.monthName(month))

        if (shown.isEmpty()) {
            list.addView(TextView(this).apply {
                text = S.text(R.string.text_051, if (day == null) S.text(R.string.text_049) else S.text(R.string.text_050))
                gravity = Gravity.CENTER
                setPadding(Ui.dp(context, 20), Ui.dp(context, 24), Ui.dp(context, 20), Ui.dp(context, 24))
                setTextAppearance(R.style.TextAppearance_Coinly_Muted)
                setBackgroundResource(R.drawable.bg_card_container)
            })
            return
        }
        shown.forEach { e ->
            val item = layoutInflater.inflate(R.layout.item_event, list, false)
            val tile = item.findViewById<View>(R.id.layoutEventTile)
            tile.backgroundTintList = ColorStateList.valueOf(getColor(eventTint(e.type)))
            val textColor = getColor(eventTextColor(e.type))
            item.findViewById<TextView>(R.id.tvEventMonth).apply { text = Ui.monthShort(month); setTextColor(textColor) }
            item.findViewById<TextView>(R.id.tvEventDay).apply { text = dayOf(e).toString(); setTextColor(textColor) }
            item.findViewById<TextView>(R.id.tvEventTitle).text = e.title
            item.findViewById<TextView>(R.id.tvEventType).text = e.type.label
            item.contentDescription = "${e.title}, ${e.type.label}, ${Ui.longDate(e.date)}"
            list.addView(item)
        }
    }
}
