package com.example.coinly

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.coinly.data.BudgetRepository
import com.example.coinly.data.MovementRepository.MovementRepository
import com.example.coinly.model.Budget
import com.example.coinly.model.BudgetStatus
import com.example.coinly.ui.Tone
import com.example.coinly.ui.Ui
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar

class PresupuestosActivity : BaseActivity() {

    private val repository by lazy { BudgetRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(BottomNav.wrap(this, layoutInflater.inflate(R.layout.activity_presupuestos, null), "Presupuestos"))

        findViewById<View>(R.id.tvClearAlerts).setOnClickListener {
            repository.dismissAll(repository.getBudgets().map { it.id })
            render()
        }
        findViewById<View>(R.id.btnCreateBudgetAction).setOnClickListener { editBudget(null) }
        render()
    }

    private fun render() {
        val alerts = findViewById<LinearLayout>(R.id.layoutAlertsContainer)
        val budgetsContainer = findViewById<LinearLayout>(R.id.layoutBudgetsContainer)
        alerts.removeAllViews()
        budgetsContainer.removeAllViews()

        val budgets = repository.getBudgets()
        val dismissed = repository.dismissedAlerts()

        // 1. Alertas activas
        val active = budgets.filter { it.status != BudgetStatus.SUCCESS && it.id !in dismissed }
        if (active.isEmpty()) {
            Ui.emptyAlerts(alerts)
        } else {
            active.forEach { b ->
                val message = if (b.status == BudgetStatus.DANGER)
                    "Has excedido tu presupuesto de ${b.category}: gastaste ${Ui.money(b.spent)} de ${Ui.money(b.limit)}."
                else
                    "Presupuesto de ${b.category} cercano al límite."
                Ui.banner(alerts, Ui.tone(b.status), message) {
                    repository.dismissAlert(b.id)
                    render()
                }
            }
        }

        // 2. Tarjetas de presupuesto
        if (budgets.isEmpty()) {
            budgetsContainer.addView(TextView(this).apply {
                text = "Aún no tienes presupuestos. Toca + para crear el primero."
                gravity = android.view.Gravity.CENTER
                setPadding(Ui.dp(context, 20), Ui.dp(context, 32), Ui.dp(context, 20), Ui.dp(context, 32))
                setTextAppearance(R.style.TextAppearance_Coinly_Muted)
                setBackgroundResource(R.drawable.bg_card_container)
            })
        }
        budgets.forEach { b ->
            val row = layoutInflater.inflate(R.layout.item_fila_presupuesto, budgetsContainer, false)
            Ui.bindBudgetRow(row, b) { showBudgetDetails(it) }
            budgetsContainer.addView(row)
        }
    }

    private fun showBudgetDetails(b: Budget) {
        val view = LayoutInflater.from(this).inflate(R.layout.sheet_budget_detail, null)
        val dialog = Ui.sheet(this, view)

        view.findViewById<TextView>(R.id.tvBdTitle).text = b.category
        view.findViewById<TextView>(R.id.tvBdPeriod).text = b.period
        Ui.bindStatusBadge(view.findViewById(R.id.tvBdBadge), b)
        view.findViewById<TextView>(R.id.tvBdPercent).text = "${b.percent}% usado"
        Ui.styleBudgetProgress(view.findViewById(R.id.progressBd), b)
        view.findViewById<TextView>(R.id.tvBdLimit).text = Ui.money(b.limit)
        view.findViewById<TextView>(R.id.tvBdSpent).text = Ui.money(b.spent)
        view.findViewById<TextView>(R.id.tvBdAvailable).apply {
            text = (if (b.available < 0) "−" else "") + Ui.money(kotlin.math.abs(b.available))
            setTextColor(getColor(if (b.available >= 0) R.color.coinly_green_text else R.color.coinly_danger_text))
        }

        view.findViewById<View>(R.id.btnBdEdit).setOnClickListener { dialog.dismiss(); editBudget(b) }
        view.findViewById<View>(R.id.btnBdDelete).setOnClickListener {
            dialog.dismiss()
            Ui.confirm(this, "Eliminar presupuesto", "¿Deseas eliminar el presupuesto de ${b.category}?", "Eliminar") {
                repository.delete(b.id)
                render()
                Ui.toast(findViewById(android.R.id.content), Tone.SUCCESS, "Presupuesto eliminado.")
            }
        }
        dialog.show()
    }

    private fun editBudget(b: Budget?) {
        val view = LayoutInflater.from(this).inflate(R.layout.sheet_budget_form, null)
        val dialog = Ui.sheet(this, view)
        val tilCategory = view.findViewById<TextInputLayout>(R.id.tilBudgetCategory)
        val tilLimit = view.findViewById<TextInputLayout>(R.id.tilBudgetLimit)
        val acCategory = view.findViewById<MaterialAutoCompleteTextView>(R.id.acBudgetCategory)
        val etLimit = view.findViewById<TextInputEditText>(R.id.etBudgetLimit)

        acCategory.setSimpleItems(MovementRepository.EXPENSE_CATEGORIES.toTypedArray())
        Ui.clearErrorOnType(tilLimit, etLimit)
        acCategory.setOnItemClickListener { _, _, _, _ -> tilCategory.error = null }

        if (b != null) {
            view.findViewById<TextView>(R.id.tvBudgetSheetTitle).text = "Editar presupuesto"
            acCategory.setText(b.category, false)
            etLimit.setText(b.limit.toString())
        }

        view.findViewById<View>(R.id.btnBudgetCancel).setOnClickListener { dialog.dismiss() }
        view.findViewById<View>(R.id.btnBudgetSave).setOnClickListener {
            val category = acCategory.text.toString().trim()
            val limit = etLimit.text.toString().toDoubleOrNull()
            var valid = true
            if (category.isEmpty()) { tilCategory.error = "Selecciona una categoría."; valid = false }
            if (limit == null || limit <= 0) { tilLimit.error = "Ingresa un monto válido."; valid = false }
            if (!valid) return@setOnClickListener

            repository.upsert(
                b?.copy(category = category, limit = limit!!)
                    ?: Budget(System.currentTimeMillis().toString(), category, limit!!, 0.0, currentPeriod(this))
            )
            dialog.dismiss()
            render()
            Ui.toast(findViewById(android.R.id.content), Tone.SUCCESS, "Presupuesto guardado correctamente.")
        }
        dialog.show()
    }

    private fun currentPeriod(context: Context): String {
        val cal = Calendar.getInstance()
        return Ui.monthName(cal.get(Calendar.MONTH)).replaceFirstChar { it.uppercase() } + " " + cal.get(Calendar.YEAR)
    }
}
