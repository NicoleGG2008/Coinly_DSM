package com.example.coinly

import com.example.coinly.data.EconomicEvent
import com.example.coinly.data.EconomicEvents
import com.example.coinly.data.NewsDates
import org.junit.Assert.*
import org.junit.Test

class ReviewValidationTest {
    @Test fun passwordRequiresAllStrengthPartsAndLengthBounds() {
        assertTrue(PasswordPolicy.valid("Coinly123"))
        listOf("abc12345", "ABC12345", "Coinlyabc", "Abc123", "A" + "b".repeat(127) + "1", "Abcdefg١").forEach {
            assertFalse(it, PasswordPolicy.valid(it))
        }
    }
    @Test fun calendarRejectsImpossibleAndMalformedDates() {
        assertTrue(NewsDates.valid("2028-02-29"))
        listOf("2026-02-29", "2026-04-31", "2026-13-01", "2026-1-01", "2026-10-01evil", "").forEach {
            assertFalse(it, NewsDates.valid(it))
        }
    }
    @Test fun eventAmountMustBeFiniteAndPositive() {
        val event = EconomicEvent("test", "Alquiler", 50.0, "expense", "Vivienda", "2026-12-01")
        assertTrue(EconomicEvents.valid(event))
        listOf(-1.0, 0.0, Double.NaN, Double.POSITIVE_INFINITY, 1_000_000_001.0).forEach {
            assertFalse(EconomicEvents.valid(event.copy(amount = it)))
        }
        assertFalse(EconomicEvents.valid(event.copy(type = "admin")))
    }
    @Test fun economicDatesUseElSalvadorMidnight() {
        val timestamp = EconomicEvents.dueAt("2026-12-01")!!
        val utc = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.ROOT).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        assertEquals("2026-12-01 06:00", utc.format(timestamp.toDate()))
    }
}
