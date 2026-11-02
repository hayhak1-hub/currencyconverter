package com.hayhak.currencyconverter.util

/**
 * Splits a mid-market amount into bureau buy and sell.
 * [spreadPercent] is the full gap: half sits below the mid rate and half above it.
 * Returns buy (lower) then sell (higher).
 */
fun buySell(mid: Double, spreadPercent: Double): Pair<Double, Double>? {
    if (!mid.isFinite() || mid < 0.0) return null
    if (!spreadPercent.isFinite() || spreadPercent <= 0.0 || spreadPercent > 20.0) return null
    val half = spreadPercent / 200.0
    val buy = mid * (1.0 - half)
    val sell = mid * (1.0 + half)
    if (!buy.isFinite() || !sell.isFinite()) return null
    return buy to sell
}

/** Percent change from a past value to today's value. */
fun profitPercent(thenValue: Double, nowValue: Double): Double? {
    if (!thenValue.isFinite() || !nowValue.isFinite() || thenValue == 0.0) return null
    val change = (nowValue - thenValue) / thenValue * 100.0
    return change.takeIf { it.isFinite() }
}
