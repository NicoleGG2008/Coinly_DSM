package com.example.coinly.model

data class Movement(
    val id: String,
    val description: String,
    val amount: Double,
    val type: String,
    val category: String,
    val date: String,
    val paymentMethod: String = "",
    val tags: List<String> = emptyList()
) {
    val isIncome: Boolean get() = type == TYPE_INCOME

    companion object {
        const val TYPE_INCOME = "income"
        const val TYPE_EXPENSE = "expense"
    }
}
