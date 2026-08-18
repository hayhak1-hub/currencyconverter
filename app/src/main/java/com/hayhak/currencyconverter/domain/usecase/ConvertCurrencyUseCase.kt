package com.hayhak.currencyconverter.domain.usecase

import javax.inject.Inject

class ConvertCurrencyUseCase @Inject constructor() {

    operator fun invoke(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        rates: Map<String, Double>,
        baseCurrency: String = "USD"
    ): Double? {
        val fromRate = if (fromCurrency == baseCurrency) 1.0 else rates[fromCurrency] ?: return null
        val toRate = if (toCurrency == baseCurrency) 1.0 else rates[toCurrency] ?: return null

        if (fromRate == 0.0) return null
        return amount * (toRate / fromRate)
    }
}
