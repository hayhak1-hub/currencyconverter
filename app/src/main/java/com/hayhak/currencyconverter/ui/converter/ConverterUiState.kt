package com.hayhak.currencyconverter.ui.converter

import com.hayhak.currencyconverter.domain.model.CurrencyInfo
import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES

data class ConverterUiState(
    val amount: String = "1",
    val fromCurrency: CurrencyInfo = SUPPORTED_CURRENCIES.first { it.code == "USD" },
    val toCurrency: CurrencyInfo = SUPPORTED_CURRENCIES.first { it.code == "EUR" },
    val result: Double? = null,
    val inverseRate: Double? = null,
    val batchResults: Map<String, Double> = emptyMap(),
    val rates: Map<String, Double> = emptyMap(),
    val favorites: Set<String> = emptySet(),
    val favoriteOrder: List<String> = emptyList(),
    val savedPairs: List<Pair<String, String>> = emptyList(),
    val sparkline: List<Double> = emptyList(),
    val targetDraft: String? = null,
    val spreadPercent: Double = 1.0,
    val historicalDate: Long? = null,
    val historicalLabel: String? = null,
    val historicalRates: Map<String, Double> = emptyMap(),
    val historicalLoading: Boolean = false,
    val historicalError: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long? = null
)
