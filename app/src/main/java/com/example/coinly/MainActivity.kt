package com.example.coinly

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.coinly.data.BudgetRepository
import com.example.coinly.data.MovementRepository.MovementRepository
import com.example.coinly.data.UserPrefs
import com.example.coinly.model.BudgetStatus
import com.example.coinly.model.Movement
import com.example.coinly.ui.FlowChartView
import com.example.coinly.ui.Tone
import com.example.coinly.ui.Ui

class MainActivity : BaseActivity() {

    private val movementRepository by lazy { MovementRepository(this) }
    private val budgetRepository by lazy { BudgetRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(BottomNav.wrap(this, layoutInflater.inflate(R.layout.activity_main, null), "Inicio"))

        val prefs = UserPrefs(this)
        findViewById<TextView>(R.id.tvGreeting).text = Ui.greeting()
        findViewById<TextView>(R.id.tvUserName).text = "${prefs.name} 👋"
        findViewById<TextView>(R.id.btnProfile).text = prefs.initial

        // Navegación
        findViewById<View>(R.id.btnProfile).setOnClickListener { go(PerfilActivity::class.java) }
        findViewById<View>(R.id.btnViewHistory).setOnClickListener { go(MovimientosActivity::class.java) }
        findViewById<View>(R.id.tvViewAllMovements).setOnClickListener { go(MovimientosActivity::class.java) }
        findViewById<View>(R.id.btnViewBudgets).setOnClickListener { go(PresupuestosActivity::class.java) }
        findViewById<View>(R.id.tvViewAllBudgets).setOnClickListener { go(PresupuestosActivity::class.java) }
        findViewById<View>(R.id.btnAddIncome).setOnClickListener {
            TransactionDialog.show(this, Movement.TYPE_INCOME) { render() }
        }
        findViewById<View>(R.id.btnAddExpense).setOnClickListener {
            TransactionDialog.show(this, Movement.TYPE_EXPENSE) { render() }
        }
        render()
    }

    private fun go(target: Class<*>) {
        startActivity(Intent(this, target))
        finish()
    }

    private fun render() {
        val movements = movementRepository.getMovements()
        val income = movements.filter { it.isIncome }.sumOf { it.amount }
        val expense = movements.filter { !it.isIncome }.sumOf { it.amount }

        findViewById<TextView>(R.id.tvBalance).text = Ui.money(income - expense)
        findViewById<TextView>(R.id.tvIncomeTotal).text = "+" + Ui.money(income)
        findViewById<TextView>(R.id.tvExpenseTotal).text = "−" + Ui.money(expense)
        findViewById<FlowChartView>(R.id.flowChart).setData(movements)

        renderAlerts()
        renderRecentMovements(movements)
        renderBudgets()
    }

    private fun renderAlerts() {
        val container = findViewById<LinearLayout>(R.id.layoutAlerts)
        container.removeAllViews()
        val dismissed = budgetRepository.dismissedAlerts()
        val active = budgetRepository.getBudgets()
            .filter { it.status != BudgetStatus.SUCCESS && it.id !in dismissed }

        // "Limpiar todas" solo tiene sentido si hay alertas; si no, se muestra el mensaje de "sin alertas"
        findViewById<View>(R.id.tvClearAlertsHome).apply {
            visibility = if (active.isEmpty()) View.GONE else View.VISIBLE
            setOnClickListener {
                budgetRepository.dismissAll(active.map { it.id })
                renderAlerts()
            }
        }
        if (active.isEmpty()) {
            Ui.emptyAlerts(container)
            return
        }
        active.forEach { b ->
                val message = if (b.status == BudgetStatus.DANGER)
                    "Has excedido tu presupuesto de ${b.category}: gastaste ${Ui.money(b.spent)} de ${Ui.money(b.limit)}."
                else
                    "Presupuesto de ${b.category} cercano al límite: ${Ui.money(b.spent)} de ${Ui.money(b.limit)}."
                Ui.banner(container, Ui.tone(b.status), message) {
                    budgetRepository.dismissAlert(b.id)
                    renderAlerts()
                }
            }
    }

    private fun renderRecentMovements(movements: List<Movement>) {
        val container = findViewById<LinearLayout>(R.id.layoutRecentMovements)
        container.removeAllViews()
        movements.take(4).forEachIndexed { i, m ->
            if (i > 0) container.addView(View(this).apply {
                setBackgroundColor(getColor(R.color.coinly_divider))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 1)))
            val row = layoutInflater.inflate(R.layout.item_fila_movimiento, container, false)
            Ui.bindMovementRow(row, m) { TransactionDialog.showDetail(this, it) { render() } }
            container.addView(row)
        }
    }

    private fun renderBudgets() {
        val container = findViewById<LinearLayout>(R.id.layoutBudgetsSummary)
        container.removeAllViews()
        budgetRepository.getBudgets().take(3).forEach { b ->
            val row = layoutInflater.inflate(R.layout.item_budget_mini, container, false)
            row.findViewById<TextView>(R.id.tvBudgetCategory).text = b.category
            Ui.bindStatusBadge(row.findViewById(R.id.tvBudgetStatusBadge), b)
            Ui.styleBudgetProgress(row.findViewById(R.id.progressBarBudget), b)
            row.findViewById<TextView>(R.id.tvBudgetSummary).text =
                "${Ui.money(b.spent)} de ${Ui.money(b.limit)} · ${b.percent}%"
            row.setOnClickListener { go(PresupuestosActivity::class.java) }
            container.addView(row)
        }
    }
}
