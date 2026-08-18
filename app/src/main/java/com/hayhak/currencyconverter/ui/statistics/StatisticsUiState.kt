package com.hayhak.currencyconverter.ui.statistics

data class CurrencyStat(
    val code: String,
    val name: String,
    val flag: String,
    val rateToTry: Double,
    val changePercent: Double?,   // null = önceki veri yok
    val prevRateToTry: Double?
)

data class StatisticsUiState(
    val stats: List<CurrencyStat> = emptyList(),
    val date: String = "",
    val isLoading: Boolean = true,
    val hasYesterdayData: Boolean = false,
    val error: String? = null
)
