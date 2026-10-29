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
        setContentView(BottomNav.wrap(this, layoutInflater.inflate(R.layout.activity_main, null), S.text(R.string.text_013)))

        val prefs = UserPrefs(this)
        findViewById<TextView>(R.id.tvGreeting).text = Ui.greeting()
        findViewById<TextView>(R.id.tvUserName).text = S.text(R.string.name_greeting, prefs.name)
        renderAvatar()

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

    override fun onCloudChanged() {
        val prefs = UserPrefs(this)
        findViewById<TextView>(R.id.tvUserName).text = S.text(R.string.name_greeting, prefs.name)
        renderAvatar()
        render()
    }

    override fun onResume() {
        super.onResume()
        renderAvatar()
    }

    private fun renderAvatar() {
        val prefs = UserPrefs(this)
        val bitmap = com.example.coinly.ui.AvatarImages.load(this, prefs.avatarUri)
        findViewById<TextView>(R.id.tvHomeAvatarInitial).apply {
            text = prefs.initial
            visibility = if (bitmap == null) View.VISIBLE else View.GONE
        }
        findViewById<android.widget.ImageView>(R.id.ivHomeAvatarPhoto).apply {
            setImageBitmap(bitmap)
            visibility = if (bitmap == null) View.GONE else View.VISIBLE
        }
    }

    private fun render() {
        val movements = movementRepository.getMovements()
        val income = movements.filter { it.isIncome }.sumOf { it.amount }
        val expense = movements.filter { !it.isIncome }.sumOf { it.amount }

        findViewById<TextView>(R.id.tvBalance).text = Ui.money(income - expense)
        findViewById<TextView>(R.id.tvIncomeTotal).text = "+" + Ui.money(income)
        findViewById<TextView>(R.id.tvExpenseTotal).text = "−" + Ui.money(expense)
        findViewById<FlowChartView>(R.id.flowChart).apply {
            visibility = if (movements.isEmpty()) View.GONE else View.VISIBLE
            setData(movements)
        }

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
                    S.text(R.string.text_030, b.category, Ui.money(b.spent), Ui.money(b.limit))
                else
                    S.text(R.string.text_031, b.category, Ui.money(b.spent), Ui.money(b.limit))
                Ui.banner(container, Ui.tone(b.status), message) {
                    budgetRepository.dismissAlert(b.id)
                    renderAlerts()
                }
            }
    }

    private fun renderRecentMovements(movements: List<Movement>) {
        val container = findViewById<LinearLayout>(R.id.layoutRecentMovements)
        container.removeAllViews()
        if (movements.isEmpty()) container.addView(TextView(this).apply {
            text = S.text(R.string.empty_movements)
            setPadding(20, 24, 20, 24)
            setTextColor(getColor(R.color.coinly_text_secondary))
        })
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
        if (budgetRepository.getBudgets().isEmpty()) container.addView(TextView(this).apply {
            text = S.text(R.string.empty_budgets)
            setPadding(20, 24, 20, 24)
            setTextColor(getColor(R.color.coinly_text_secondary))
        })
        budgetRepository.getBudgets().take(3).forEach { b ->
            val row = layoutInflater.inflate(R.layout.item_budget_mini, container, false)
            row.findViewById<TextView>(R.id.tvBudgetCategory).text = b.category
            Ui.bindStatusBadge(row.findViewById(R.id.tvBudgetStatusBadge), b)
            Ui.styleBudgetProgress(row.findViewById(R.id.progressBarBudget), b)
            row.findViewById<TextView>(R.id.tvBudgetSummary).text =
                S.text(R.string.text_032, Ui.money(b.spent), Ui.money(b.limit), b.percent)
            row.setOnClickListener { go(PresupuestosActivity::class.java) }
            container.addView(row)
        }
    }
}
