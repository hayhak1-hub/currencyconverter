package com.hayhak.currencyconverter.ui.dashboard

data class CurrencyRow(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String,
    val rateToTry: Double,
    val trend: Double = 0.0
)

data class DashboardUiState(
    val rows: List<CurrencyRow> = emptyList(),         // sadece favoriler
    val allRates: Map<String, Double> = emptyMap(),    // tüm kurlar (favori olmayanlar için)
    val baseCurrency: String = "USD",
    val favorites: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long? = null,
    val showAlarmDialog: CurrencyRow? = null
)
