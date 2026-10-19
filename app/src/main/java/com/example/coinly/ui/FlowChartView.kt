package com.example.coinly.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.example.coinly.R
import com.example.coinly.model.Movement

/** Evolución de ingresos y gastos por día (últimos 7 días con movimientos), sobre fondo oscuro. */
class FlowChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Day(val label: String, val income: Double, val expense: Double)

    private var days: List<Day> = emptyList()
    private val density = resources.displayMetrics.density
    private val incomeColor = ContextCompat.getColor(context, R.color.coinly_chart_income)
    private val expenseColor = ContextCompat.getColor(context, R.color.coinly_chart_expense)

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(26, 255, 255, 255); strokeWidth = density
        style = Paint.Style.STROKE
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(3 * density, 5 * density), 0f)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2.5f * density
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(190, 255, 255, 255)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
        textAlign = Paint.Align.CENTER
    }

    fun setData(movements: List<Movement>) {
        val dates = movements.map { it.date }.distinct().sorted().takeLast(7)
        days = dates.map { date ->
            Day(
                label = date.substring(8),
                income = movements.filter { it.date == date && it.isIncome }.sumOf { it.amount },
                expense = movements.filter { it.date == date && !it.isIncome }.sumOf { it.amount }
            )
        }
        val peakIncome = days.maxByOrNull { it.income }
        val peakExpense = days.maxByOrNull { it.expense }
        contentDescription = if (days.isEmpty()) "Sin movimientos para graficar" else
            "Evolución de ingresos y gastos de los últimos ${days.size} días con movimientos. " +
                "Mayor ingreso: ${Ui.money(peakIncome?.income ?: 0.0)} el día ${peakIncome?.label}. " +
                "Mayor gasto: ${Ui.money(peakExpense?.expense ?: 0.0)} el día ${peakExpense?.label}."
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desired = (116 * density).toInt()
        setMeasuredDimension(
            resolveSize(suggestedMinimumWidth, widthMeasureSpec),
            resolveSize(desired, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (days.isEmpty()) return

        val padX = 10 * density
        val labelH = 22 * density
        val top = 8 * density
        val bottom = height - labelH
        val plotH = bottom - top
        val max = maxOf(days.maxOf { maxOf(it.income, it.expense) }, 1.0)

        listOf(0.25f, 0.55f, 0.85f).forEach { f ->
            val y = top + plotH * f
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }

        fun x(i: Int) = padX + i * (width - 2 * padX) / maxOf(days.size - 1, 1)
        fun y(v: Double) = (bottom - 4 * density - (v / max).toFloat() * (plotH - 8 * density))

        fun path(selector: (Day) -> Double): Path = Path().also { p ->
            days.forEachIndexed { i, d -> if (i == 0) p.moveTo(x(i), y(selector(d))) else p.lineTo(x(i), y(selector(d))) }
        }

        // Área bajo la línea de ingresos
        val area = Path(path { it.income }).apply {
            lineTo(x(days.size - 1), bottom); lineTo(x(0), bottom); close()
        }
        fillPaint.shader = LinearGradient(0f, top, 0f, bottom, Color.argb(90, 86, 225, 165), Color.argb(0, 86, 225, 165), Shader.TileMode.CLAMP)
        canvas.drawPath(area, fillPaint)

        linePaint.color = incomeColor; canvas.drawPath(path { it.income }, linePaint)
        linePaint.color = expenseColor; canvas.drawPath(path { it.expense }, linePaint)

        days.forEachIndexed { i, d ->
            dotPaint.color = incomeColor; canvas.drawCircle(x(i), y(d.income), 3.5f * density, dotPaint)
            dotPaint.color = expenseColor; canvas.drawCircle(x(i), y(d.expense), 3.5f * density, dotPaint)
            canvas.drawText(d.label, x(i), height - 5 * density, textPaint)
        }
    }
}
