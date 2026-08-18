package com.hayhak.currencyconverter.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.app.Application
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import com.hayhak.currencyconverter.util.currencyName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val currencyInfo = SUPPORTED_CURRENCIES.associateBy { it.code }

private fun crossRateToTry(code: String, rates: Map<String, Double>): Double? {
    if (code == "TRY") return 1.0
    val tryRate = rates["TRY"] ?: return null
    if (code == "USD") return tryRate
    val codeRate = rates[code] ?: return null
    if (codeRate == 0.0) return null
    return tryRate / codeRate
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: ExchangeRateRepository,
    private val userPrefs: UserPreferencesRepository,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState

    private val defaultCodes = listOf("USD", "EUR", "TRY", "CHF")
    private val dateFmt get() = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale.getDefault())

    init {
        viewModelScope.launch {
            combine(
                repository.getLatestRates("USD"),
                userPrefs.favorites
            ) { todayExchangeRate, favorites ->
                Pair(todayExchangeRate, favorites)
            }.collect { (todayExchangeRate, favorites) ->
                val todayRates = todayExchangeRate?.rates ?: run {
                    _uiState.update { it.copy(isLoading = false, error = application.getString(R.string.stats_no_data)) }
                    return@collect
                }

                val cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(20)
                val yesterdayExchangeRate = repository.getRatesBefore("USD", cutoff)
                val yesterdayRates = yesterdayExchangeRate?.rates

                val codes = favorites.ifEmpty { defaultCodes.toSet() }
                    .filter { it != "USD" || true }  // USD her zaman dahil

                val stats = codes
                    .sortedWith(compareBy { if (it in defaultCodes) defaultCodes.indexOf(it) else 999 })
                    .mapNotNull { code ->
                        val todayRate = crossRateToTry(code, todayRates) ?: return@mapNotNull null
                        val prevRate  = yesterdayRates?.let { crossRateToTry(code, it) }
                        val change    = if (prevRate != null && prevRate != 0.0)
                            (todayRate - prevRate) / prevRate * 100.0
                        else null
                        val info = currencyInfo[code]
                        CurrencyStat(
                            code           = code,
                            name           = info?.let { application.currencyName(it.code) } ?: code,
                            flag           = info?.flag  ?: "🏳",
                            rateToTry      = todayRate,
                            changePercent  = change,
                            prevRateToTry  = prevRate
                        )
                    }

                _uiState.update {
                    it.copy(
                        stats            = stats,
                        date             = dateFmt.format(Date()),
                        isLoading        = false,
                        hasYesterdayData = yesterdayRates != null,
                        error            = null
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.refreshRates("USD")
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
