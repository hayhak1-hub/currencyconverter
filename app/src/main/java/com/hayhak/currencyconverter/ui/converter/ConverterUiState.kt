package com.hayhak.currencyconverter.ui.converter

import com.hayhak.currencyconverter.domain.model.CurrencyInfo
import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES

data class ConverterUiState(
    val amount: String = "1",
    val fromCurrency: CurrencyInfo = SUPPORTED_CURRENCIES.first { it.code == "USD" },
    val toCurrency: CurrencyInfo = SUPPORTED_CURRENCIES.first { it.code == "TRY" },
    val result: Double? = null,
    val batchResults: Map<String, Double> = emptyMap(),
    val rates: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long? = null
)
