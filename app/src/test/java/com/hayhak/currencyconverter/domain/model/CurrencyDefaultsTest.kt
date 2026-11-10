package com.hayhak.currencyconverter.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyDefaultsTest {

    @Test
    fun `first favorite that differs from the base is used`() {
        val d = currencyPairDefaults("USD", listOf("USD", "GBP", "JPY"))
        assertEquals("USD", d.home)
        assertEquals("GBP", d.other)
    }

    @Test
    fun `no favorites falls back to EUR`() {
        assertEquals("EUR", currencyPairDefaults("USD", emptyList()).other)
    }

    @Test
    fun `euro home falls back to USD`() {
        assertEquals("USD", currencyPairDefaults("EUR", listOf("EUR")).other)
    }

    @Test
    fun `base only in favorites never pairs with itself`() {
        assertEquals("EUR", currencyPairDefaults("TRY", listOf("TRY")).other)
    }
}
