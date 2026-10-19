package com.example.coinly.model

data class Budget(
    val id: String,
    val category: String,
    val limit: Double,
    val spent: Double,
    val period: String
) {
    val available: Double get() = limit - spent
    val percent: Int get() = if (limit <= 0) 0 else ((spent / limit) * 100).toInt()

    /** Estado visual del presupuesto: excedido, al límite (>= 80 %) o en orden. */
    val status: BudgetStatus
        get() = when {
            spent > limit -> BudgetStatus.DANGER
            limit > 0 && spent / limit >= 0.8 -> BudgetStatus.WARNING
            else -> BudgetStatus.SUCCESS
        }
}

enum class BudgetStatus { SUCCESS, WARNING, DANGER }
