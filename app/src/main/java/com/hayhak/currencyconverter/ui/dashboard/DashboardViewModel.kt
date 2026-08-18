package com.hayhak.currencyconverter.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES
import com.hayhak.currencyconverter.util.currencyName
import com.hayhak.currencyconverter.domain.model.RateAlarm
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import com.hayhak.currencyconverter.domain.usecase.GetExchangeRatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import android.app.Application
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ExchangeRateRepository,
    private val userPrefs: UserPreferencesRepository,
    private val application: Application,
    private val getExchangeRates: GetExchangeRatesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var ratesJob: Job? = null

    init {
        // Favorileri state'e yaz (SettingsScreen bunu okur)
        userPrefs.favorites
            .onEach { favs -> _uiState.update { it.copy(favorites = favs) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            // İlk yükleme: kaydedilen baz para birimini al ve kurları yükle
            val initialBase = userPrefs.lastDashboardBase.first()
            _uiState.update { it.copy(baseCurrency = initialBase) }
            loadRates(initialBase)

            // Baz para birimi değişirse yeniden yükle
            userPrefs.lastDashboardBase
                .drop(1) // ilk değeri zaten aldık
                .onEach { base ->
                    _uiState.update { it.copy(baseCurrency = base, isLoading = true) }
                    loadRates(base)
                }
                .launchIn(this)
        }
    }

    fun onBaseCurrencyChange(newBase: String) {
        viewModelScope.launch {
            userPrefs.saveLastDashboardBase(newBase)
        }
    }

    fun toggleFavorite(code: String) {
        viewModelScope.launch {
            userPrefs.toggleFavorite(code)
        }
    }

    fun showAlarmDialog(row: CurrencyRow?) {
        _uiState.update { it.copy(showAlarmDialog = row) }
    }

    fun addAlarm(threshold: Double, isAbove: Boolean) {
        val row = _uiState.value.showAlarmDialog ?: return
        viewModelScope.launch {
            userPrefs.addAlarm(
                RateAlarm(
                    baseCode = _uiState.value.baseCurrency,
                    targetCode = row.code,
                    threshold = threshold,
                    isAbove = isAbove
                )
            )
            showAlarmDialog(null)
        }
    }

    private fun loadRates(base: String) {
        ratesJob?.cancel()
        
        viewModelScope.launch {
            try {
                repository.refreshRates(base)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = application.getString(com.hayhak.currencyconverter.R.string.dashboard_error_fetch)) }
            }
        }

        ratesJob = combine(
            getExchangeRates(base),
            userPrefs.favorites
        ) { exchangeRate, favorites ->
            if (exchangeRate == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@combine
            }
            
            val rows = SUPPORTED_CURRENCIES
                .filter { it.code != base }
                .filter { favorites.contains(it.code) }
                .mapNotNull { info ->
                    val rateToBase = exchangeRate.rates[info.code] ?: return@mapNotNull null
                    CurrencyRow(
                        code = info.code,
                        name = application.currencyName(info.code),
                        symbol = info.symbol,
                        rateToTry = rateToBase
                    )
                }
            
            _uiState.update {
                it.copy(
                    rows = rows,
                    allRates = exchangeRate.rates,
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    lastUpdated = exchangeRate.timestamp
                )
            }
        }.launchIn(viewModelScope)
    }

    fun refresh() {
        val base = _uiState.value.baseCurrency
        _uiState.update { it.copy(isRefreshing = true) }
        
        viewModelScope.launch {
            try {
                repository.refreshRates(base)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = application.getString(com.hayhak.currencyconverter.R.string.dashboard_error_refresh), isRefreshing = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
