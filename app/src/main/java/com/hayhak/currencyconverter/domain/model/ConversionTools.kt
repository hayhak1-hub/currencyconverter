package com.hayhak.currencyconverter.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/** Fees are deducted in the destination currency, after conversion. */
fun netAfterFees(gross: Double, percent: String, fixed: String): Double? {
    if (!gross.isFinite() || gross < 0) return null
    // Accept ordinary decimal input only; bound precision and reject huge exponents.
    val decimal = Regex("[0-9]*([.,][0-9]*)?")
    if (percent.length > 40 || fixed.length > 40 || !decimal.matches(percent) || !decimal.matches(fixed)) return null
    val percentage = percent.replace(',', '.').ifBlank { "0" }.toBigDecimalOrNull() ?: return null
    val flat = fixed.replace(',', '.').ifBlank { "0" }.toBigDecimalOrNull() ?: return null
    if (percentage < BigDecimal.ZERO || percentage > BigDecimal(100) || flat < BigDecimal.ZERO) return null
    val result = BigDecimal.valueOf(gross)
        .multiply(BigDecimal.ONE.subtract(percentage.divide(BigDecimal(100))))
        .subtract(flat)
    return result.takeIf { it >= BigDecimal.ZERO }?.setScale(8, RoundingMode.HALF_UP)?.toDouble()
}

const val GRAMS_PER_TROY_OUNCE = 31.1034768

data class SavedConversion(
    val id: String,
    val amount: String,
    val from: String,
    val to: String,
    val gross: Double,
    val net: Double,
    val percent: String,
    val fixed: String,
    val savedAt: Long,
    val rateTimestamp: Long
)
