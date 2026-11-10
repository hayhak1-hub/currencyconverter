package com.hayhak.currencyconverter.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RateTimingTest {
    private fun series(vararg rates: Double) = rates.mapIndexed { i, r -> HistoricalRate(i * 86_400_000L, r) }

    @Test
    fun `latest at the period high is at the top`() {
        val t = rateTiming(series(1.0, 1.1, 1.2, 1.0, 1.1, 1.2, 1.0, 1.1, 1.2, 1.5))!!
        assertEquals(100, t.percentile)
        assertEquals(1f, t.positionInRange, 0.0001f)
        assertNotNull(t.vsAveragePercent)
        assert(t.vsAveragePercent > 0)
    }

    @Test
    fun `latest at the period low is at the bottom`() {
        val t = rateTiming(series(1.5, 1.4, 1.3, 1.5, 1.4, 1.3, 1.5, 1.4, 1.3, 1.0))!!
        assertEquals(0, t.percentile)
        assertEquals(0f, t.positionInRange, 0.0001f)
        assert(t.vsAveragePercent < 0)
    }

    @Test
    fun `too little or flat data gives nothing`() {
        assertNull(rateTiming(series(1.0, 1.1, 1.2)))
        assertNull(rateTiming(series(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)))
    }
}
