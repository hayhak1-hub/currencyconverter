package com.hayhak.currencyconverter.ui.dashboard

enum class DashboardSortOption {
    DEFAULT,
    NAME,
    RATE,
    TREND
}

data class CurrencyRow(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String,
    val rateToTry: Double,
    val trend: Double = 0.0
)

data class DashboardUiState(
    val rows: List<CurrencyRow> = emptyList(),
    val allRates: Map<String, Double> = emptyMap(),
    val baseCurrency: String = "USD",
    val favorites: Set<String> = emptySet(),
    val favoriteOrder: List<String> = emptyList(),
    val sortOption: DashboardSortOption = DashboardSortOption.DEFAULT,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long? = null,
    val yesterdayRates: Map<String, Double> = emptyMap(),
    val showAlarmDialog: CurrencyRow? = null
)
