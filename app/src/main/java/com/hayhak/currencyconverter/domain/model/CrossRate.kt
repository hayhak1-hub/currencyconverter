package com.hayhak.currencyconverter.domain.model

fun quotePerUnit(code: String, quote: String, usdRates: Map<String, Double>): Double? {
    if (code == quote) return 1.0
    val codeUsd = if (code == "USD") 1.0 else usdRates[code] ?: return null
    val quoteUsd = if (quote == "USD") 1.0 else usdRates[quote] ?: return null
    if (codeUsd == 0.0) return null
    return quoteUsd / codeUsd
}

val METAL_CODES = listOf("XAU", "XAG", "XPT", "XPD")
