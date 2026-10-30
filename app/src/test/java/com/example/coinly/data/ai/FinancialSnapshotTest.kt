package com.example.coinly.data.ai

import com.example.coinly.model.Movement
import org.junit.Assert.*
import org.junit.Test

class FinancialSnapshotTest {
    private fun movement(id: String, amount: Double, type: String, date: String) =
        Movement(id, "Descripción privada", amount, type, "Comida", date, tags = listOf("casa"))

    @Test fun monthlyTotalsExcludeOtherMonthsAndSeparateIncomeFromExpenses() {
        val facts = FinancialSnapshot.describe(listOf(
            movement("a", 100.0, Movement.TYPE_INCOME, "2026-10-01"),
            movement("b", 25.0, Movement.TYPE_EXPENSE, "2026-10-10"),
            movement("c", 900.0, Movement.TYPE_EXPENSE, "2026-09-30")
        ), "2026-10-29", "USD")
        assertTrue(facts.contains("Mes 2026-10: ingresos=100.00; gastos=25.00"))
        assertTrue(facts.contains("Comida: 25.00"))
    }

    @Test fun recentSampleIsBoundedAndDoesNotExposeInternalIdsOrDescriptions() {
        val facts = FinancialSnapshot.describe((1..40).map {
            movement("private-id-$it", 1.0, Movement.TYPE_EXPENSE, "2026-10-10")
        }, "2026-10-29", "USD")
        assertEquals(30, facts.lineSequence().count { it.startsWith("fecha=") })
        assertTrue(facts.contains("30 movimientos más recientes de 40"))
        assertFalse(facts.contains("private-id"))
        assertFalse(facts.contains("Descripción privada"))
    }

    @Test fun newAccountHasExplicitZeroTotalsAndNoExampleTransactions() {
        val facts = FinancialSnapshot.describe(emptyList(), "2026-10-29", "USD")
        assertTrue(facts.contains("ingresos=0.00; gastos=0.00"))
        assertFalse(facts.lineSequence().any { it.startsWith("fecha=") })
    }
}
