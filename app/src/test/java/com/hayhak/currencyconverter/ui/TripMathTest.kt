package com.hayhak.currencyconverter.ui

import com.hayhak.currencyconverter.data.store.model.Trip
import com.hayhak.currencyconverter.data.store.model.TripExpense
import com.hayhak.currencyconverter.ui.trips.tripStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripMathTest {

    // 1 USD = 0.9 EUR = 40 TRY.
    private val rates = mapOf("EUR" to 0.9, "TRY" to 40.0)

    private fun trip(vararg amounts: Double, budget: Double = 1000.0) = Trip(
        name = "Test", localCode = "EUR", homeCode = "TRY", budget = budget, createdAt = 0L,
        expenses = amounts.map { TripExpense(amount = it, at = 0L) }
    )

    @Test
    fun `spending is converted from local to home currency`() {
        val status = tripStatus(trip(9.0, 9.0), rates)

        // 18 EUR is 20 USD, which is 800 TRY.
        assertEquals(800.0, status.spentHome, 1e-6)
        assertEquals(200.0, status.remainingHome, 1e-6)
        assertEquals(0.8, status.usedFraction, 1e-9)
        assertFalse(status.overBudget)
    }

    @Test
    fun `overspending is reported`() {
        // 30 EUR is 1333.33 TRY against a 1000 budget.
        val status = tripStatus(trip(30.0), rates)

        assertTrue(status.overBudget)
        assertEquals(1000.0 - 30.0 / 0.9 * 40.0, status.remainingHome, 1e-6)
    }

    @Test
    fun `missing rate leaves expenses unconverted`() {
        val status = tripStatus(trip(5.0, 5.0), emptyMap())

        assertEquals(2, status.unconverted)
        assertEquals(0.0, status.spentHome, 0.0)
    }

    @Test
    fun `zero budget gives no fraction`() {
        assertEquals(0.0, tripStatus(trip(5.0, budget = 0.0), rates).usedFraction, 0.0)
    }
}
