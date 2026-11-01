package com.hayhak.currencyconverter.ui.statistics

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.BASE_CURRENCY
import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES
import com.hayhak.currencyconverter.domain.model.quotePerUnit
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import com.hayhak.currencyconverter.util.currencyName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val currencyInfo = SUPPORTED_CURRENCIES.associateBy { it.code }

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: ExchangeRateRepository,
    private val userPrefs: UserPreferencesRepository,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState

    private val defaultCodes = listOf("USD", "EUR", "TRY", "CHF")

    // Bumped after the recent history has been stored so the change column is recomputed.
    private val historyLoaded = MutableStateFlow(0)

    private fun trackedCodes(favorites: Set<String>, quote: String) =
        favorites.ifEmpty { defaultCodes.toSet() }.filter { it != quote }
    private val dateFmt get() = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale.getDefault())

    init {
        viewModelScope.launch {
            combine(
                repository.getLatestRates(BASE_CURRENCY),
                userPrefs.favorites,
                userPrefs.lastDashboardBase,
                historyLoaded
            ) { today, favorites, quote, _ ->
                Triple(today, favorites, quote)
            }.collect { (todayExchangeRate, favorites, quote) ->
                val todayRates = todayExchangeRate?.rates ?: run {
                    _uiState.update { it.copy(isLoading = false, error = application.getString(R.string.stats_no_data)) }
                    return@collect
                }

                val cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(20)
                val yesterdayRates = repository.getRatesBefore(BASE_CURRENCY, cutoff)?.rates

                val codes = trackedCodes(favorites, quote)

                val stats = codes
                    .sortedWith(compareBy { if (it in defaultCodes) defaultCodes.indexOf(it) else 999 })
                    .mapNotNull { code ->
                        val todayRate = quotePerUnit(code, quote, todayRates) ?: return@mapNotNull null
                        val prevRate = yesterdayRates?.let { quotePerUnit(code, quote, it) }
                        val change = if (prevRate != null && prevRate != 0.0)
                            (todayRate - prevRate) / prevRate * 100.0
                        else null
                        val info = currencyInfo[code]
                        CurrencyStat(
                            code = code,
                            name = info?.let { application.currencyName(it.code) } ?: code,
                            flag = info?.flag ?: "🏳",
                            rateToTry = todayRate,
                            changePercent = change,
                            prevRateToTry = prevRate
                        )
                    }

                _uiState.update {
                    it.copy(
                        stats = stats,
                        quoteCode = quote,
                        date = dateFmt.format(Date()),
                        isLoading = false,
                        hasYesterdayData = yesterdayRates != null,
                        error = null
                    )
                }
            }
        }

        // A fresh install has no stored history, so "yesterday" would be missing for a day.
        // Pull the last week once per tracked set of currencies instead of waiting.
        viewModelScope.launch {
            combine(userPrefs.favorites, userPrefs.lastDashboardBase) { favorites, quote ->
                (trackedCodes(favorites, quote) + quote).filter { it != BASE_CURRENCY }.toSortedSet()
            }.distinctUntilChanged().collect { targets ->
                if (targets.isEmpty()) return@collect
                try {
                    repository.refreshHistoricalRates(BASE_CURRENCY, targets.joinToString(","), 7)
                    historyLoaded.update { it + 1 }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.refreshRates(BASE_CURRENCY, force = true)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
