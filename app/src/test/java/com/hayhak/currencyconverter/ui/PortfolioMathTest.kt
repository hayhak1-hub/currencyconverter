package com.hayhak.currencyconverter.ui

import com.hayhak.currencyconverter.data.store.model.PortfolioEntry
import com.hayhak.currencyconverter.ui.portfolio.holdingRow
import com.hayhak.currencyconverter.ui.portfolio.portfolioSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PortfolioMathTest {

    // USD based: 1 USD = 40 TRY = 0.9 EUR.
    private val rates = mapOf("TRY" to 40.0, "EUR" to 0.9)

    private fun entry(code: String, amount: Double, cost: Double, costCode: String) =
        PortfolioEntry(code = code, amount = amount, costPerUnit = cost, costCode = costCode, boughtAt = 0L)

    @Test
    fun `profit when the currency got more expensive`() {
        // Bought 500 USD at 35.20 TRY, worth 40 TRY now.
        val row = holdingRow(entry("USD", 500.0, 35.20, "TRY"), rates)!!
        assertEquals(17_600.0, row.cost, 1e-6)
        assertEquals(20_000.0, row.value, 1e-6)
        assertEquals(2_400.0, row.pnl, 1e-6)
        assertEquals(2_400.0 / 17_600.0 * 100, row.pnlPercent!!, 1e-9)
    }

    @Test
    fun `loss is negative`() {
        val row = holdingRow(entry("USD", 100.0, 45.0, "TRY"), rates)!!
        assertEquals(-500.0, row.pnl, 1e-6)
    }

    @Test
    fun `totals are converted into the display currency`() {
        val summary = portfolioSummary(
            listOf(entry("USD", 100.0, 35.0, "TRY"), entry("EUR", 10.0, 1.0, "USD")),
            rates,
            displayCode = "TRY"
        )
        // First: cost 3500 TRY, value 4000 TRY. Second: cost 10 USD = 400 TRY, value 10 EUR = 11.11 USD = 444.44 TRY.
        assertEquals(3_900.0, summary.totalCost, 1e-6)
        assertEquals(4_000.0 + 10 / 0.9 * 40, summary.totalValue, 1e-6)
        assertNotNull(summary.pnlPercent)
        assertEquals(0, summary.unpriced)
    }

    @Test
    fun `entries without a rate are counted and skipped`() {
        val summary = portfolioSummary(listOf(entry("XXX", 1.0, 1.0, "USD"), entry("USD", 1.0, 1.0, "USD")), rates, "USD")
        assertEquals(1, summary.unpriced)
        assertEquals(1, summary.rows.size)
    }

    @Test
    fun `no cost gives no percentage`() {
        assertNull(holdingRow(entry("USD", 1.0, 0.0, "USD"), rates)!!.pnlPercent)
    }
}
