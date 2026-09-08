package com.hayhak.currencyconverter.ui.history

import com.hayhak.currencyconverter.domain.model.CurrencyInfo
import com.hayhak.currencyconverter.domain.model.HistoricalRate
import com.hayhak.currencyconverter.domain.model.currencyByCode

enum class TimeRange(val labelRes: Int, val days: Int) {
    ONE_DAY(com.hayhak.currencyconverter.R.string.range_1d, 5),
    ONE_WEEK(com.hayhak.currencyconverter.R.string.range_1w, 7),
    ONE_MONTH(com.hayhak.currencyconverter.R.string.range_1m, 30),
    SIX_MONTHS(com.hayhak.currencyconverter.R.string.range_6m, 180),
    ONE_YEAR(com.hayhak.currencyconverter.R.string.range_1y, 365),
    FIVE_YEARS(com.hayhak.currencyconverter.R.string.range_5y, 1825)
}

data class HistoryUiState(
    val baseCurrency: String = "USD",
    val targetCurrency: String = "TRY",
    val baseInfo: CurrencyInfo? = currencyByCode("USD"),
    val targetInfo: CurrencyInfo? = currencyByCode("TRY"),
    val timeRange: TimeRange = TimeRange.ONE_WEEK,
    val data: List<HistoricalRate> = emptyList(),
    val compareEnabled: Boolean = false,
    val compareBase: String = "EUR",
    val compareTarget: String = "USD",
    val compareBaseInfo: CurrencyInfo? = currencyByCode("EUR"),
    val compareTargetInfo: CurrencyInfo? = currencyByCode("USD"),
    val compareData: List<HistoricalRate> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasData: Boolean = false
)
