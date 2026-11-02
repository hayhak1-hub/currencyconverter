package com.hayhak.currencyconverter.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoversTest {

    private fun row(code: String, trend: Double) =
        CurrencyRow(code = code, name = code, symbol = code, flag = "", rateToTry = 1.0, trend = trend)

    @Test
    fun `gainers and losers are sorted by size and limited`() {
        val movers = topMovers(
            listOf(row("A", 0.5), row("B", 2.0), row("C", -1.0), row("D", -3.0), row("E", 1.0), row("F", 0.1), row("G", -0.2)),
            count = 2,
            allowed = null
        )
        assertEquals(listOf("B", "E"), movers.gainers.map { it.code })
        assertEquals(listOf("D", "C"), movers.losers.map { it.code })
    }

    @Test
    fun `unchanged and implausible rows are ignored`() {
        val movers = topMovers(listOf(row("Z", 0.0), row("X", 80.0), row("Y", Double.NaN), row("OK", 1.0)), allowed = null)
        assertEquals(listOf("OK"), movers.gainers.map { it.code })
        assertTrue(movers.losers.isEmpty())
    }

    @Test
    fun `exotic currencies are left out by default`() {
        val movers = topMovers(listOf(row("SSP", 4.8), row("EUR", 0.3), row("IRR", -9.0), row("JPY", -0.4)))
        assertEquals(listOf("EUR"), movers.gainers.map { it.code })
        assertEquals(listOf("JPY"), movers.losers.map { it.code })
    }

    @Test
    fun `no data gives empty movers`() {
        assertTrue(topMovers(emptyList()).isEmpty)
    }
}
