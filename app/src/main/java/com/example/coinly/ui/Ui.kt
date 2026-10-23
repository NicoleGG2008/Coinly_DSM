package com.example.coinly.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.coinly.R
import com.example.coinly.data.UserPrefs
import com.example.coinly.model.Budget
import com.example.coinly.model.BudgetStatus
import com.example.coinly.model.Movement
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Estados visuales compartidos (alertas, avisos, insignias). Nunca dependen solo del color: llevan icono y texto. */
enum class Tone(
    @ColorRes val tint: Int,
    @DrawableRes val banner: Int,
    @ColorRes val text: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val badge: Int
) {
    SUCCESS(R.color.coinly_tint_success, R.drawable.bg_banner_success, R.color.coinly_green_text, R.drawable.ic_check_circle, R.drawable.bg_badge_success),
    WARNING(R.color.coinly_tint_warning, R.drawable.bg_banner_warning, R.color.coinly_on_warning, R.drawable.ic_warning, R.drawable.bg_badge_warning),
    DANGER(R.color.coinly_tint_danger, R.drawable.bg_banner_danger, R.color.coinly_danger_text, R.drawable.ic_x_circle, R.drawable.bg_badge_danger),
    INFO(R.color.coinly_tint_info, R.drawable.bg_banner_info, R.color.coinly_accent, R.drawable.ic_info, R.drawable.bg_badge_info)
}

object Ui {

    private val MONTHS = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
    private val MONTHS_SHORT = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

    // ---------- Formatos ----------
    fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    fun currencySymbol(code: String): String = when (code.uppercase()) {
        "USD" -> "$"
        "EUR" -> "€"
        "MXN" -> "MXN$"
        "COP" -> "COP$"
        "ARS" -> "ARS$"
        "PEN" -> "S/"
        "CLP" -> "CLP$"
        "BRL" -> "R$"
        else -> if (code.isNotBlank()) "$code " else "$"
    }

    fun money(value: Double, currencyCode: String = "USD"): String {
        val symbol = currencySymbol(currencyCode)
        val formatted = String.format(Locale.US, "%,.2f", value)
        return if (symbol.endsWith("$") || symbol == "S/") "$symbol$formatted" else "$symbol $formatted"
    }

    fun money(context: Context, value: Double): String {
        val code = UserPrefs(context).currency
        return money(value, code)
    }

    fun signedMoney(m: Movement, currencyCode: String = "USD"): String =
        (if (m.isIncome) "+" else "−") + money(m.amount, currencyCode)

    fun signedMoney(context: Context, m: Movement): String =
        (if (m.isIncome) "+" else "−") + money(context, m.amount)

    fun monthName(index: Int): String = MONTHS[index]
    fun monthShort(index: Int): String = MONTHS_SHORT[index]

    private fun parts(date: String): Triple<Int, Int, Int>? = runCatching {
        val p = date.split("-")
        Triple(p[0].toInt(), p[1].toInt() - 1, p[2].toInt())
    }.getOrNull()

    /** "2026-08-10" → "10 Ago" */
    fun shortDate(date: String): String = parts(date)?.let { "${it.third} ${MONTHS_SHORT[it.second]}" } ?: date

    /** "2026-08-10" → "10 de agosto de 2026" */
    fun longDate(date: String): String = parts(date)?.let { "${it.third} de ${MONTHS[it.second]} de ${it.first}" } ?: date

    fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun greeting(): String {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (h < 12) "Buenos días" else if (h < 18) "Buenas tardes" else "Buenas noches"
    }

    fun tone(status: BudgetStatus): Tone = when (status) {
        BudgetStatus.SUCCESS -> Tone.SUCCESS
        BudgetStatus.WARNING -> Tone.WARNING
        BudgetStatus.DANGER -> Tone.DANGER
    }

    private fun color(context: Context, @ColorRes res: Int) = ContextCompat.getColor(context, res)

    // ---------- Banners de alerta ----------
    fun banner(parent: ViewGroup, tone: Tone, message: String, onDismiss: (() -> Unit)? = null): View {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_banner, parent, false)
        v.setBackgroundResource(tone.banner)
        v.findViewById<ImageView>(R.id.bannerIcon).apply {
            setImageResource(tone.icon)
            imageTintList = ColorStateList.valueOf(color(context, tone.text))
        }
        v.findViewById<TextView>(R.id.bannerText).apply {
            text = message
            setTextColor(color(context, tone.text))
        }
        v.findViewById<View>(R.id.bannerDismiss).apply {
            if (onDismiss == null) visibility = View.GONE else setOnClickListener { onDismiss() }
        }
        parent.addView(v)
        return v
    }

    /** Mensaje cuando no hay alertas pendientes (Inicio y Presupuestos). */
    fun emptyAlerts(parent: ViewGroup): View {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_empty_alerts, parent, false)
        parent.addView(v)
        return v
    }

    // ---------- Filas ----------
    fun bindMovementRow(row: View, m: Movement, onClick: ((Movement) -> Unit)? = null) {
        val ctx = row.context
        val tint = if (m.isIncome) R.color.coinly_green_text else R.color.coinly_danger_text
        val amountColor = if (m.isIncome) R.color.coinly_green_text else R.color.coinly_danger_text
        row.findViewById<View>(R.id.layoutIconContainer)
            .setBackgroundResource(if (m.isIncome) R.drawable.bg_tile_income else R.drawable.bg_tile_expense)
        row.findViewById<ImageView>(R.id.ivMovementIcon).apply {
            setImageResource(if (m.isIncome) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down)
            imageTintList = ColorStateList.valueOf(color(ctx, tint))
        }
        row.findViewById<TextView>(R.id.tvMovementDescription).text = m.description
        row.findViewById<TextView>(R.id.tvMovementSubDetails).text = "${m.category} · ${shortDate(m.date)}"
        row.findViewById<TextView>(R.id.tvMovementAmount).apply {
            text = signedMoney(ctx, m)
            setTextColor(color(ctx, amountColor))
        }
        val typeLabel = if (m.isIncome) "Ingreso" else "Gasto"
        row.findViewById<TextView>(R.id.tvMovementTypeLabel).text = typeLabel
        row.contentDescription = "$typeLabel: ${m.description}, ${money(ctx, m.amount)}, ${m.category}, ${longDate(m.date)}"
        if (onClick != null) row.setOnClickListener { onClick(m) } else row.isClickable = false
    }

    fun styleBudgetProgress(bar: ProgressBar, budget: Budget) {
        val c = when (budget.status) {
            BudgetStatus.SUCCESS -> R.color.coinly_green
            BudgetStatus.WARNING -> R.color.coinly_warning
            BudgetStatus.DANGER -> R.color.coinly_danger
        }
        bar.progress = budget.percent.coerceIn(0, 100)
        bar.progressTintList = ColorStateList.valueOf(color(bar.context, c))
    }

    fun bindStatusBadge(badge: TextView, budget: Budget) {
        val tone = tone(budget.status)
        val (text, icon) = when (budget.status) {
            BudgetStatus.SUCCESS -> "En orden" to R.drawable.ic_check
            BudgetStatus.WARNING -> "Al límite" to R.drawable.ic_warning
            BudgetStatus.DANGER -> "Excedido" to R.drawable.ic_x_circle
        }
        badge.text = text
        badge.setBackgroundResource(tone.badge)
        badge.setTextColor(color(badge.context, tone.text))
        badge.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        badge.compoundDrawableTintList = ColorStateList.valueOf(color(badge.context, tone.text))
    }

    fun bindBudgetRow(row: View, b: Budget, onClick: ((Budget) -> Unit)? = null) {
        val ctx = row.context
        row.findViewById<TextView>(R.id.tvBudgetCategory).text = b.category
        row.findViewById<TextView>(R.id.tvBudgetPeriod).text = b.period
        bindStatusBadge(row.findViewById(R.id.tvBudgetStatusBadge), b)
        styleBudgetProgress(row.findViewById(R.id.progressBarBudget), b)
        row.findViewById<TextView>(R.id.tvBudgetLimit).text = money(ctx, b.limit)
        row.findViewById<TextView>(R.id.tvBudgetSpent).text = money(ctx, b.spent)
        row.findViewById<TextView>(R.id.tvBudgetAvailable).apply {
            text = (if (b.available < 0) "−" else "") + money(ctx, kotlin.math.abs(b.available))
            setTextColor(color(ctx, if (b.available >= 0) R.color.coinly_green_text else R.color.coinly_danger_text))
        }
        val state = when (b.status) { BudgetStatus.SUCCESS -> "en orden"; BudgetStatus.WARNING -> "al límite"; BudgetStatus.DANGER -> "excedido" }
        row.contentDescription = "Presupuesto de ${b.category}, $state. Límite ${money(ctx, b.limit)}, gastado ${money(ctx, b.spent)}, ${b.percent} por ciento usado."
        if (onClick != null) row.setOnClickListener { onClick(b) }
    }

    // ---------- Formularios ----------
    fun setError(layout: TextInputLayout, message: String?) {
        layout.error = message
    }

    /** Quita el error de un campo en cuanto la persona vuelve a escribir. */
    fun clearErrorOnType(layout: TextInputLayout, edit: EditText) {
        edit.doAfterTextChanged { if (layout.error != null) layout.error = null }
    }

    val CURRENCIES = arrayOf("USD", "EUR", "MXN", "COP", "ARS", "PEN", "CLP", "BRL")
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    fun isEmail(value: String) = EMAIL.matches(value)

    /** Campo de fecha de solo lectura que abre el selector nativo. */
    fun bindDateField(context: Context, field: EditText, onPicked: ((String) -> Unit)? = null) {
        field.isFocusable = false
        field.isClickable = true
        field.setOnClickListener {
            val cal = Calendar.getInstance()
            parts(field.text.toString())?.let { cal.set(it.first, it.second, it.third) }
            DatePickerDialog(context, { _, y, m, d ->
                val iso = "%04d-%02d-%02d".format(y, m + 1, d)
                field.setText(iso)
                onPicked?.invoke(iso)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    // ---------- Hojas inferiores ----------
    fun sheet(context: Context, view: View): BottomSheetDialog {
        val dialog = BottomSheetDialog(context)
        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return dialog
    }

    fun confirm(context: Context, title: String, message: String, confirmLabel: String, onConfirm: () -> Unit) {
        val v = LayoutInflater.from(context).inflate(R.layout.sheet_confirm, null)
        val dialog = sheet(context, v)
        v.findViewById<TextView>(R.id.tvConfirmTitle).text = title
        v.findViewById<TextView>(R.id.tvConfirmMessage).text = message
        v.findViewById<TextView>(R.id.btnConfirmOk).text = confirmLabel
        v.findViewById<View>(R.id.btnConfirmCancel).setOnClickListener { dialog.dismiss() }
        v.findViewById<View>(R.id.btnConfirmOk).setOnClickListener { dialog.dismiss(); onConfirm() }
        dialog.show()
    }

    // ---------- Avisos ----------
    fun toast(anchor: View, tone: Tone, message: String) {
        val symbol = when (tone) { Tone.SUCCESS -> "✓"; Tone.WARNING -> "⚠"; Tone.DANGER -> "✕"; Tone.INFO -> "ℹ" }
        val bar = Snackbar.make(anchor, "$symbol  $message", Snackbar.LENGTH_LONG)
        bar.setBackgroundTint(color(anchor.context, tone.tint))
        bar.setTextColor(color(anchor.context, R.color.coinly_text_primary))
        bar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.maxLines = 3
        anchor.rootView.findViewById<View>(R.id.nav_bar)?.let { bar.anchorView = it }
        bar.show()
    }
}
