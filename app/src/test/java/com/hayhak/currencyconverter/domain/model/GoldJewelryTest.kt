package com.hayhak.currencyconverter.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GoldJewelryTest {

    // An ounce price that makes one gram of pure gold cost exactly 100.
    private val ounce = GRAMS_PER_TROY_OUNCE * 100.0

    @Test
    fun `24 karat gram equals the pure gold gram price`() {
        assertEquals(100.0, goldGramValue(ounce, 24), 1e-9)
    }

    @Test
    fun `lower karats scale by purity`() {
        assertEquals(100.0 * 22 / 24, goldGramValue(ounce, 22), 1e-9)
        assertEquals(75.0, goldGramValue(ounce, 18), 1e-9)
        assertEquals(100.0 * 14 / 24, goldGramValue(ounce, 14), 1e-9)
    }

    @Test
    fun `coin value is weight times fineness times gram price`() {
        assertEquals(1.75 * 0.916 * 100.0, goldCoinValue(ounce, GoldCoin.QUARTER), 1e-9)
        assertEquals(7.216 * 0.916 * 100.0, goldCoinValue(ounce, GoldCoin.ATA), 1e-9)
    }

    @Test
    fun `half and full coins are two and four quarters`() {
        val quarter = goldCoinValue(ounce, GoldCoin.QUARTER)
        assertEquals(quarter * 2, goldCoinValue(ounce, GoldCoin.HALF), 1e-9)
        assertEquals(quarter * 4, goldCoinValue(ounce, GoldCoin.FULL), 1e-9)
    }
}
