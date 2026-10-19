package com.example.coinly

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import com.example.coinly.data.MovementRepository.MovementRepository
import com.example.coinly.model.Movement
import com.example.coinly.ui.Ui
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText

class MovimientosActivity : BaseActivity() {

    private val repository by lazy { MovementRepository(this) }
    private var typeFilter = "all"
    private var categoryFilter = ALL
    private var minAmount = 0.0
    private var maxAmount = Double.MAX_VALUE
    private var dateFrom = ""
    private var dateTo = ""

    private lateinit var list: LinearLayout
    private lateinit var search: TextInputEditText
    private lateinit var acCategory: MaterialAutoCompleteTextView
    private lateinit var etMin: TextInputEditText
    private lateinit var etMax: TextInputEditText
    private lateinit var etFrom: TextInputEditText
    private lateinit var etTo: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(BottomNav.wrap(this, layoutInflater.inflate(R.layout.activity_movimientos, null), "Movimientos"))

        list = findViewById(R.id.layoutMovements)
        search = findViewById(R.id.etSearch)
        acCategory = findViewById(R.id.acFilterCategory)
        etMin = findViewById(R.id.etMinAmount)
        etMax = findViewById(R.id.etMaxAmount)
        etFrom = findViewById(R.id.etDateFrom)
        etTo = findViewById(R.id.etDateTo)

        // Filtro por tipo
        findViewById<ChipGroup>(R.id.chipGroupFilter).setOnCheckedStateChangeListener { _, ids ->
            typeFilter = when (ids.firstOrNull()) {
                R.id.chipIncome -> Movement.TYPE_INCOME
                R.id.chipExpense -> Movement.TYPE_EXPENSE
                else -> "all"
            }
            render()
        }

        // Filtros avanzados
        val panel = findViewById<View>(R.id.advancedFilters)
        val toggle = findViewById<View>(R.id.btnToggleFilters)
        toggle.setOnClickListener {
            val show = panel.visibility != View.VISIBLE
            panel.visibility = if (show) View.VISIBLE else View.GONE
            toggle.contentDescription = if (show) "Ocultar filtros avanzados" else "Mostrar filtros avanzados"
        }
        Ui.bindDateField(this, etFrom)
        Ui.bindDateField(this, etTo)
        setupCategories()
        search.doAfterTextChanged { render() }

        findViewById<MaterialButton>(R.id.btnApplyFilters).setOnClickListener {
            categoryFilter = acCategory.text.toString().ifBlank { ALL }
            minAmount = etMin.text.toString().toDoubleOrNull() ?: 0.0
            maxAmount = etMax.text.toString().toDoubleOrNull() ?: Double.MAX_VALUE
            dateFrom = etFrom.text.toString().trim()
            dateTo = etTo.text.toString().trim()
            render()
        }
        findViewById<MaterialButton>(R.id.btnClearFilters).setOnClickListener {
            categoryFilter = ALL; minAmount = 0.0; maxAmount = Double.MAX_VALUE; dateFrom = ""; dateTo = ""
            acCategory.setText(ALL, false)
            etMin.setText(""); etMax.setText(""); etFrom.setText(""); etTo.setText("")
            render()
        }

        findViewById<View>(R.id.btnAddMovement).setOnClickListener {
            TransactionDialog.show(this) { setupCategories(); render() }
        }
        render()
    }

    private fun setupCategories() {
        val categories = listOf(ALL) + repository.getMovements().map { it.category }.distinct().sorted()
        acCategory.setSimpleItems(categories.toTypedArray())
        if (acCategory.text.isNullOrBlank() || acCategory.text.toString() !in categories) acCategory.setText(ALL, false)
    }

    private fun render() {
        list.removeAllViews()
        val query = search.text.toString().trim().lowercase()
        val movements = repository.getMovements().filter { m ->
            (typeFilter == "all" || m.type == typeFilter) &&
                (categoryFilter == ALL || m.category == categoryFilter) &&
                m.amount in minAmount..maxAmount &&
                (dateFrom.isEmpty() || m.date >= dateFrom) &&
                (dateTo.isEmpty() || m.date <= dateTo) &&
                (query.isEmpty() || m.description.lowercase().contains(query) || m.category.lowercase().contains(query))
        }
        findViewById<TextView>(R.id.tvMovementCount).text =
            if (movements.size == 1) "1 movimiento" else "${movements.size} movimientos"

        if (movements.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "No hay movimientos con estos filtros."
                gravity = android.view.Gravity.CENTER
                setPadding(Ui.dp(context, 20), Ui.dp(context, 32), Ui.dp(context, 20), Ui.dp(context, 32))
                setTextAppearance(R.style.TextAppearance_Coinly_Muted)
                setBackgroundResource(R.drawable.bg_card_container)
            })
            return
        }

        // Agrupados por día, cada grupo en una tarjeta
        movements.groupBy { it.date }.forEach { (date, items) ->
            list.addView(TextView(this).apply {
                text = Ui.longDate(date)
                setTextAppearance(R.style.TextAppearance_Coinly_Overline)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                androidx.core.view.ViewCompat.setAccessibilityHeading(this, true)
                setPadding(Ui.dp(context, 4), Ui.dp(context, 12), 0, Ui.dp(context, 8))
            })
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_card_container)
                setPadding(Ui.dp(context, 8), Ui.dp(context, 8), Ui.dp(context, 8), Ui.dp(context, 8))
            }
            items.forEachIndexed { i, m ->
                if (i > 0) card.addView(View(this).apply {
                    setBackgroundColor(getColor(R.color.coinly_divider))
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 1)))
                val row = layoutInflater.inflate(R.layout.item_fila_movimiento, card, false)
                Ui.bindMovementRow(row, m) { TransactionDialog.showDetail(this, it) { setupCategories(); render() } }
                card.addView(row)
            }
            list.addView(card)
        }
    }

    private companion object {
        const val ALL = "Todas"
    }
}
