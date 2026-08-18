package com.hayhak.currencyconverter.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedCurrenciesTest {

    @Test
    fun `catalog covers Frankfurter v2 world currencies`() {
        val codes = SUPPORTED_CURRENCIES.map { it.code }.toSet()
        assertTrue("expected 150+ currencies, got ${codes.size}", codes.size >= 150)
        listOf("USD", "EUR", "TRY", "AED", "SAR", "RUB", "NGN", "ARS", "XAU").forEach { code ->
            assertTrue("$code should be in catalog", code in codes)
        }
    }

    @Test
    fun `dashboard currencies are all supported`() {
        val codes = SUPPORTED_CURRENCIES.map { it.code }.toSet()
        DASHBOARD_CURRENCIES.forEach { code ->
            assertTrue("$code should be in SUPPORTED_CURRENCIES", code in codes)
        }
    }

    @Test
    fun `no duplicate currency codes`() {
        val codes = SUPPORTED_CURRENCIES.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
    }
}
