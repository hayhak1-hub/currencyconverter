package com.hayhak.currencyconverter.util

import com.hayhak.currencyconverter.domain.model.QuietHours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ConverterToolsTest {

    @Test
    fun spreadSplitsHalfOnEachSide() {
        val (buy, sell) = buySell(100.0, 1.0)!!
        assertEquals(99.5, buy, 0.0001)
        assertEquals(100.5, sell, 0.0001)
    }

    @Test
    fun zeroSpreadIsHidden() {
        assertNull(buySell(100.0, 0.0))
    }

    @Test
    fun quietHoursWrapPastMidnight() {
        val hours = QuietHours(enabled = true, startMinute = 23 * 60, endMinute = 8 * 60)
        assertTrue(isQuietNow(hours, calendar(23, 30)))
        assertTrue(isQuietNow(hours, calendar(7, 59)))
        assertFalse(isQuietNow(hours, calendar(12, 0)))
        assertFalse(isQuietNow(hours.copy(enabled = false), calendar(1, 0)))
    }

    @Test
    fun parsesAmountAndPair() {
        val parsed = parseProcessText("Pay 1,200.50 USD to EUR please")
        assertEquals(1200.50, parsed.amount!!, 0.0001)
        assertEquals("USD", parsed.from)
        assertEquals("EUR", parsed.to)
    }

    @Test
    fun europeanThousandsAndDecimal() {
        assertEquals(1200.5, normalizeAmount("1.200,50")!!, 0.0001)
    }

    @Test
    fun profitPercentFromPastToToday() {
        assertEquals(10.0, profitPercent(100.0, 110.0)!!, 0.0001)
        assertEquals(-50.0, profitPercent(80.0, 40.0)!!, 0.0001)
        assertNull(profitPercent(0.0, 10.0))
    }

    @Test
    fun zeroDecimalCurrencies() {
        assertEquals(0, fractionDigits("JPY"))
        assertEquals(0, fractionDigits("KRW"))
        assertEquals(0, fractionDigits("IDR"))
        assertEquals(2, fractionDigits("EUR"))
        assertEquals(4, fractionDigits("XAU"))
    }

    private fun calendar(hour: Int, minute: Int) = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
}
