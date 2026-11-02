package com.hayhak.currencyconverter.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class CrossSeriesTest {

    private fun day(n: Int) = TimeUnit.DAYS.toMillis(n.toLong())
    private fun series(vararg rates: Pair<Int, Double>) = rates.map { HistoricalRate(day(it.first), it.second) }

    @Test
    fun `value of one code unit in the quote currency`() {
        // Units per USD: EUR 0.9, TRY 40. One EUR is 40 / 0.9 TRY.
        val result = crossSeries(series(0 to 0.9, 1 to 0.8), series(0 to 40.0, 1 to 40.0))

        assertEquals(listOf(40.0 / 0.9, 40.0 / 0.8), result.map { it.rate })
    }

    @Test
    fun `usd as the code uses the quote series directly`() {
        val result = crossSeries(null, series(0 to 40.0, 1 to 41.0))
        assertEquals(listOf(40.0, 41.0), result.map { it.rate })
    }

    @Test
    fun `usd as the quote inverts the code series`() {
        val result = crossSeries(series(0 to 0.5, 1 to 0.25), null)
        assertEquals(listOf(2.0, 4.0), result.map { it.rate })
    }

    @Test
    fun `only days present in both series are used`() {
        val result = crossSeries(series(0 to 1.0, 1 to 1.0, 2 to 1.0), series(1 to 2.0, 2 to 2.0, 3 to 2.0))
        assertEquals(listOf(day(1), day(2)), result.map { it.date })
    }

    @Test
    fun `invalid rates and missing data give no points`() {
        assertTrue(crossSeries(null, null).isEmpty())
        assertTrue(crossSeries(series(0 to 0.0), series(0 to 40.0)).isEmpty())
    }
}
