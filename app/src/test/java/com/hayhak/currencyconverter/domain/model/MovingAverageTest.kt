package com.hayhak.currencyconverter.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class MovingAverageTest {

    private fun day(n: Int) = TimeUnit.DAYS.toMillis(n.toLong())
    private fun series(vararg rates: Double) = rates.mapIndexed { i, r -> HistoricalRate(day(i), r) }

    @Test
    fun `average is taken over the trailing window and starts once it is covered`() {
        // Daily points 10, 20, 30, 40, 50 with a 2 day window.
        val sma = movingAverage(series(10.0, 20.0, 30.0, 40.0, 50.0), windowDays = 2)

        // First point covering the full window is day 2: average of days 0..2.
        assertEquals(listOf(day(2), day(3), day(4)), sma.map { it.date })
        assertEquals(listOf(20.0, 30.0, 40.0), sma.map { it.rate })
    }

    @Test
    fun `sparse weekly data still averages by calendar days`() {
        val weekly = (0..8).map { HistoricalRate(day(it * 7), (it + 1) * 10.0) }
        val sma = movingAverage(weekly, windowDays = 14)

        // From day 14 on: three weekly points fall in the window.
        assertEquals(day(14), sma.first().date)
        assertEquals((10.0 + 20.0 + 30.0) / 3, sma.first().rate, 1e-9)
    }

    @Test
    fun `too little data or a bad window gives no line`() {
        assertTrue(movingAverage(series(1.0), 5).isEmpty())
        assertTrue(movingAverage(series(1.0, 2.0, 3.0), 30).isEmpty())
        assertTrue(movingAverage(series(1.0, 2.0, 3.0), 0).isEmpty())
    }
}
