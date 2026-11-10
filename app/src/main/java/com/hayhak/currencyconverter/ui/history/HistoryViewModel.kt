package com.hayhak.currencyconverter.ui.history

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayhak.currencyconverter.domain.model.CurrencyInfo
import com.hayhak.currencyconverter.domain.model.currencyByCode
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import com.hayhak.currencyconverter.domain.usecase.GetHistoricalRatesUseCase
import com.hayhak.currencyconverter.util.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoricalRates: GetHistoricalRatesUseCase,
    private val repository: ExchangeRateRepository,
    private val userPrefs: UserPreferencesRepository,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var historyJob: Job? = null

    init {
        userPrefs.favorites
            .onEach { favs -> _uiState.update { it.copy(favorites = favs) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val base = userPrefs.lastHistoryBase.first()
            val target = userPrefs.lastHistoryTarget.first()
            val range = TimeRange.entries.firstOrNull { it.name == userPrefs.historyRange.first() }
                ?: TimeRange.ONE_WEEK
            val (compareBase, compareTarget) = userPrefs.historyCompare.first()
            _uiState.update {
                it.copy(
                    baseCurrency = base,
                    targetCurrency = target,
                    baseInfo = currencyByCode(base),
                    targetInfo = currencyByCode(target),
                    timeRange = range,
                    compareBase = compareBase,
                    compareTarget = compareTarget,
                    compareBaseInfo = currencyByCode(compareBase),
                    compareTargetInfo = currencyByCode(compareTarget)
                )
            }
            loadHistory(base, target, range)
        }
    }

    fun selectBase(currency: CurrencyInfo) {
        // Picking the other side's currency swaps the pair instead of silently doing nothing.
        if (currency.code == _uiState.value.targetCurrency) {
            swapCurrencies()
            return
        }
        _uiState.update {
            it.copy(
                baseCurrency = currency.code,
                baseInfo = currency
            )
        }
        persistAndLoad()
    }

    fun selectTarget(currency: CurrencyInfo) {
        if (currency.code == _uiState.value.baseCurrency) {
            swapCurrencies()
            return
        }
        _uiState.update {
            it.copy(
                targetCurrency = currency.code,
                targetInfo = currency
            )
        }
        persistAndLoad()
    }

    fun swapCurrencies() {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                baseCurrency = state.targetCurrency,
                targetCurrency = state.baseCurrency,
                baseInfo = state.targetInfo,
                targetInfo = state.baseInfo
            )
        }
        persistAndLoad()
    }

    fun selectPair(base: String, target: String) {
        if (base == target) return
        _uiState.update {
            it.copy(
                baseCurrency = base,
                targetCurrency = target,
                baseInfo = currencyByCode(base),
                targetInfo = currencyByCode(target)
            )
        }
        persistAndLoad()
    }

    fun selectTimeRange(range: TimeRange) {
        _uiState.update { it.copy(timeRange = range) }
        viewModelScope.launch { userPrefs.saveHistoryRange(range.name) }
        val state = _uiState.value
        loadHistory(state.baseCurrency, state.targetCurrency, range)
        if (state.compareEnabled) loadCompare()
    }

    fun setCompareEnabled(enabled: Boolean) {
        if (!enabled) compareJob?.cancel()
        _uiState.update { it.copy(compareEnabled = enabled, compareData = if (enabled) it.compareData else emptyList()) }
        if (enabled) loadCompare()
    }

    fun selectCompareBase(currency: CurrencyInfo) {
        if (currency.code == _uiState.value.compareTarget) {
            swapCompare()
            return
        }
        _uiState.update { it.copy(compareBase = currency.code, compareBaseInfo = currency) }
        persistCompareAndLoad()
    }

    fun selectCompareTarget(currency: CurrencyInfo) {
        if (currency.code == _uiState.value.compareBase) {
            swapCompare()
            return
        }
        _uiState.update { it.copy(compareTarget = currency.code, compareTargetInfo = currency) }
        persistCompareAndLoad()
    }

    private fun swapCompare() {
        _uiState.update {
            it.copy(
                compareBase = it.compareTarget,
                compareTarget = it.compareBase,
                compareBaseInfo = it.compareTargetInfo,
                compareTargetInfo = it.compareBaseInfo
            )
        }
        persistCompareAndLoad()
    }

    private fun persistCompareAndLoad() {
        val state = _uiState.value
        viewModelScope.launch { userPrefs.saveHistoryCompare(state.compareBase, state.compareTarget) }
        loadCompare()
    }

    /** Retries the current pair after a failed load. */
    fun reload() {
        val state = _uiState.value
        refreshedAt.remove("${state.baseCurrency}/${state.targetCurrency}/${state.timeRange.days}")
        loadHistory(state.baseCurrency, state.targetCurrency, state.timeRange)
        if (state.compareEnabled) loadCompare()
    }

    private fun persistAndLoad() {
        val state = _uiState.value
        viewModelScope.launch {
            userPrefs.saveLastHistoryCurrencies(state.baseCurrency, state.targetCurrency)
        }
        loadHistory(state.baseCurrency, state.targetCurrency, state.timeRange)
    }

    private fun loadHistory(base: String, target: String, range: TimeRange) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, data = emptyList(), hasData = false) }

            // The network refresh (up to several sequential requests for long ranges) runs next to
            // the database flow, so stored data is on screen immediately instead of after the fetch.
            val key = "$base/$target/${range.days}"
            val fresh = refreshedAt[key]?.let { System.currentTimeMillis() - it < REFRESH_MIN_AGE_MS } == true
            val refresh = if (fresh) null else launch {
                val failure = try {
                    repository.refreshHistoricalRates(base, target, range.days)
                    refreshedAt[key] = System.currentTimeMillis()
                    null
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    e
                }
                _uiState.update { s ->
                    s.copy(
                        isLoading = false,
                        error = if (s.data.isEmpty() && failure != null) failure.message ?: "error" else s.error
                    )
                }
            }
            if (refresh == null) _uiState.update { it.copy(isLoading = false) }

            getHistoricalRates(base, target, daysBack = range.days).collect { data ->
                _uiState.update {
                    it.copy(
                        data = data,
                        // Keep the spinner only while there is nothing to show yet and a fetch is running.
                        isLoading = if (data.isNotEmpty() || refresh?.isActive != true) false else it.isLoading,
                        hasData = data.isNotEmpty(),
                        error = if (data.isNotEmpty()) null else it.error
                    )
                }
            }
        }
    }

    private companion object {
        /** Process-wide, so reopening the screen does not refetch the same range every time. */
        val refreshedAt = java.util.concurrent.ConcurrentHashMap<String, Long>()
        const val REFRESH_MIN_AGE_MS = 30 * 60 * 1000L
    }

    private var compareJob: Job? = null

    private fun loadCompare() {
        val state = _uiState.value
        compareJob?.cancel()
        _uiState.update { it.copy(compareData = emptyList()) }
        compareJob = viewModelScope.launch {
            runCatching {
                repository.refreshHistoricalRates(state.compareBase, state.compareTarget, state.timeRange.days)
            }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
            getHistoricalRates(state.compareBase, state.compareTarget, state.timeRange.days).collect { data ->
                _uiState.update { it.copy(compareData = data) }
            }
        }
    }

    fun exportToCsv(onResult: (String?) -> Unit) {
        val state = _uiState.value
        if (state.data.isEmpty()) {
            onResult(null)
            return
        }

        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                CsvExporter.exportHistoricalRates(
                    context = application,
                    baseCurrency = state.baseCurrency,
                    targetCurrency = state.targetCurrency,
                    data = state.data
                )
            }
            onResult(path)
        }
    }
}
