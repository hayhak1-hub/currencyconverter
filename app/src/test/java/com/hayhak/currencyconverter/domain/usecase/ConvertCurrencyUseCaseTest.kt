package com.hayhak.currencyconverter.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ConvertCurrencyUseCaseTest {

    private lateinit var useCase: ConvertCurrencyUseCase

    private val rates = mapOf(
        "EUR" to 0.9,
        "TRY" to 36.0,
        "GBP" to 0.8
    )

    @Before
    fun setUp() {
        useCase = ConvertCurrencyUseCase()
    }

    @Test
    fun `converts from USD base to target`() {
        val result = useCase(100.0, "USD", "TRY", rates)
        assertEquals(3600.0, result!!, 0.0001)
    }

    @Test
    fun `converts between two non-base currencies`() {
        // 100 EUR -> USD = 100 / 0.9; -> TRY = that * 36
        val result = useCase(100.0, "EUR", "TRY", rates)
        assertEquals(100.0 * (36.0 / 0.9), result!!, 0.0001)
    }

    @Test
    fun `same currency returns original amount`() {
        val result = useCase(50.0, "USD", "USD", rates)
        assertEquals(50.0, result!!, 0.0001)
    }

    @Test
    fun `missing rate returns null`() {
        val result = useCase(10.0, "USD", "XYZ", rates)
        assertNull(result)
    }

    @Test
    fun `zero from-rate returns null`() {
        val result = useCase(10.0, "EUR", "TRY", mapOf("EUR" to 0.0, "TRY" to 36.0))
        assertNull(result)
    }
}
