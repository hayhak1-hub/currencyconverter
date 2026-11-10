package com.hayhak.currencyconverter.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoversTest {

    /** [rateChange] is the change of the quoted rate (units per base), as the Rates list shows it. */
    private fun row(code: String, rateChange: Double) =
        CurrencyRow(code = code, name = code, symbol = code, flag = "", rateToTry = 1.0, trend = rateChange)

    @Test
    fun `a rising rate means the currency lost value`() {
        // 1 USD buys 1% more EUR, so one EUR buys less USD: its value fell by about 1%.
        assertEquals(-0.990099, valueChangePercent(1.0), 1e-5)
        assertEquals(1.0101, valueChangePercent(-1.0), 1e-4)
    }

    @Test
    fun `gainers are the currencies whose rate fell`() {
        val movers = topMovers(listOf(row("A", -2.0), row("B", 3.0), row("C", -0.5), row("D", 1.0)), allowed = null)

        assertEquals(listOf("A", "C"), movers.gainers.map { it.code })
        assertEquals(listOf("B", "D"), movers.losers.map { it.code })
        assertTrue(movers.gainers.all { it.trend > 0 })
    }

    @Test
    fun `lists are limited and sorted by size`() {
        val movers = topMovers(
            listOf(row("A", -0.5), row("B", -2.0), row("C", -1.0), row("D", 0.4), row("E", 3.0), row("F", 0.1)),
            count = 2,
            allowed = null
        )
        assertEquals(listOf("B", "C"), movers.gainers.map { it.code })
        assertEquals(listOf("E", "D"), movers.losers.map { it.code })
    }

    @Test
    fun `unchanged and implausible rows are ignored`() {
        val movers = topMovers(listOf(row("Z", 0.0), row("X", 80.0), row("Y", Double.NaN), row("OK", -1.0)), allowed = null)
        assertEquals(listOf("OK"), movers.gainers.map { it.code })
        assertTrue(movers.losers.isEmpty())
    }

    @Test
    fun `exotic currencies are left out by default`() {
        val movers = topMovers(listOf(row("SSP", -4.8), row("EUR", -0.3), row("IRR", 9.0), row("JPY", 0.4)))
        assertEquals(listOf("EUR"), movers.gainers.map { it.code })
        assertEquals(listOf("JPY"), movers.losers.map { it.code })
    }

    @Test
    fun `strength ranks currencies independently of the base`() {
        // USD base. EUR gained 2% in value, GBP lost 2%, JPY unchanged, USD unchanged.
        val rows = listOf(row("EUR", -1.96078), row("GBP", 2.04082), row("JPY", 0.0))
        val movers = strengthMovers(rows, baseCode = "USD", allowed = null)

        assertEquals("EUR", movers.gainers.first().code)
        assertEquals("GBP", movers.losers.first().code)
    }

    @Test
    fun `strength needs enough currencies`() {
        assertTrue(strengthMovers(listOf(row("EUR", -1.0)), baseCode = "USD", allowed = null).isEmpty)
    }

    @Test
    fun `no data gives empty movers`() {
        assertTrue(topMovers(emptyList()).isEmpty)
    }
}
