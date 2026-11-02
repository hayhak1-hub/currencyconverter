package com.hayhak.currencyconverter.domain.model

/** Karat steps offered for gold by purity. */
val GOLD_KARATS = listOf(24, 22, 18, 14)

/** Fineness of Turkish gold coins (ziynet): 22 karat, 916 parts per thousand. */
const val COIN_FINENESS = 0.916

/**
 * Standard gross weights in grams. These are the commonly quoted figures; individual coins vary,
 * so values are indicative melt values, not dealer prices.
 */
enum class GoldCoin(val grossGrams: Double) {
    QUARTER(1.75),
    HALF(3.5),
    FULL(7.0),
    ATA(7.216)
}

/** Value of one gram of gold of the given karat, from the price of a troy ounce of pure gold. */
fun goldGramValue(ouncePrice: Double, karat: Int): Double =
    ouncePrice / GRAMS_PER_TROY_OUNCE * (karat / 24.0)

/** Melt value of a coin: gross weight times fineness times the pure-gold gram price. */
fun goldCoinValue(ouncePrice: Double, coin: GoldCoin): Double =
    ouncePrice / GRAMS_PER_TROY_OUNCE * coin.grossGrams * COIN_FINENESS
