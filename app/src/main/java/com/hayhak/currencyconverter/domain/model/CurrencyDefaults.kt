package com.hayhak.currencyconverter.domain.model

/**
 * Starting currencies for forms that ask for a pair, taken from what the user already uses instead of a
 * fixed currency: [home] is their working base (the Rates screen base) and [other] the first favorite
 * that differs from it. Falls back to EUR, or USD when the home currency is EUR.
 */
data class CurrencyPairDefaults(val home: String, val other: String)

fun currencyPairDefaults(home: String, favoritesInOrder: List<String>): CurrencyPairDefaults {
    val other = favoritesInOrder.firstOrNull { it != home && it.isNotBlank() }
        ?: if (home == "EUR") "USD" else "EUR"
    return CurrencyPairDefaults(home = home, other = other)
}
