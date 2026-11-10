package com.hayhak.currencyconverter.domain.model

import com.hayhak.currencyconverter.domain.usecase.ConvertCurrencyUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class InverseRateTest {

    private val convertCurrency = ConvertCurrencyUseCase()
    private val rates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.95,
        "TRY" to 36.5
    )

    @Test
    fun `direct and inverse rates match accurately`() {
        val direct = convertCurrency(1.0, "USD", "TRY", rates, "USD")
        val inverse = convertCurrency(1.0, "TRY", "USD", rates, "USD")

        assertEquals(36.5, direct!!, 0.001)
        assertEquals(1.0 / 36.5, inverse!!, 0.001)
    }
}
