package com.hayhak.currencyconverter.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

private val ZERO_DECIMAL = setOf("JPY", "KRW", "IDR")
private val METAL_CODES = setOf("XAU", "XAG", "XPT", "XPD")

/** Cash-style fraction digits: none for yen, won and rupiah; four for metals; two otherwise. */
fun fractionDigits(code: String): Int = when (code.uppercase(Locale.US)) {
    in ZERO_DECIMAL -> 0
    in METAL_CODES -> 4
    else -> 2
}

fun formatMoney(value: Double, code: String, locale: Locale = Locale.getDefault()): String {
    if (!value.isFinite()) return "—"
    var digits = fractionDigits(code)
    val magnitude = abs(value)
    if (magnitude > 0 && magnitude < 1) {
        var scaled = magnitude
        while (digits < 6 && scaled < 0.1) {
            digits++
            scaled *= 10
        }
    }
    return NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = digits
        maximumFractionDigits = digits
        isGroupingUsed = true
    }.format(value)
}
