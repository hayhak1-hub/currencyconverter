package com.hayhak.currencyconverter.ui.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayhak.currencyconverter.domain.model.CurrencyInfo
import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import com.hayhak.currencyconverter.domain.usecase.ConvertCurrencyUseCase
import com.hayhak.currencyconverter.domain.usecase.GetExchangeRatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class ConverterViewModel @Inject constructor(
    private val getExchangeRates: GetExchangeRatesUseCase,
    private val convertCurrency: ConvertCurrencyUseCase,
    private val repository: ExchangeRateRepository,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConverterUiState(isLoading = true))
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPrefs.lastFromCurrency.onEach { code ->
                SUPPORTED_CURRENCIES.find { it.code == code }?.let { info ->
                    _uiState.update { it.copy(fromCurrency = info) }
                }
            }.launchIn(this)

            userPrefs.lastToCurrency.onEach { code ->
                SUPPORTED_CURRENCIES.find { it.code == code }?.let { info ->
                    _uiState.update { it.copy(toCurrency = info) }
                }
            }.launchIn(this)
        }

        refreshRates()

        getExchangeRates("USD")
            .onEach { exchangeRate ->
                _uiState.update { state ->
                    val rates = exchangeRate?.rates ?: emptyMap()
                    val result = recalculate(
                        state.amount,
                        state.fromCurrency.code,
                        state.toCurrency.code,
                        rates
                    )
                    val batchResults = calculateBatch(state.amount, state.fromCurrency.code, rates)
                    state.copy(
                        rates = rates,
                        isLoading = false,
                        lastUpdated = exchangeRate?.timestamp,
                        result = result,
                        batchResults = batchResults
                    )
                }
            }
            .launchIn(viewModelScope)

        _uiState
            .debounce(500)
            .distinctUntilChanged { old, new ->
                old.amount == new.amount &&
                    old.fromCurrency == new.fromCurrency &&
                    old.toCurrency == new.toCurrency
            }
            .onEach { state ->
                val result = recalculate(
                    state.amount,
                    state.fromCurrency.code,
                    state.toCurrency.code,
                    state.rates
                )
                val batchResults = calculateBatch(state.amount, state.fromCurrency.code, state.rates)
                _uiState.update { it.copy(result = result, batchResults = batchResults) }
                userPrefs.saveLastConverterCurrencies(state.fromCurrency.code, state.toCurrency.code)
                if (needsRefresh(state.fromCurrency.code, state.toCurrency.code, state.rates)) {
                    refreshRates()
                }
            }
            .launchIn(viewModelScope)
    }

    fun onAmountChange(value: String) {
        if (value.count { it == ',' || it == '.' } > 1) return
        _uiState.update { it.copy(amount = value) }
    }

    fun onNumpadClick(key: String) {
        _uiState.update { state ->
            val current = state.amount
            val newValue = when (key) {
                "C" -> ""
                "DEL" -> current.dropLast(1)
                ",", "." -> {
                    if (current.contains(',') || current.contains('.')) current
                    else if (current.isEmpty()) "0$key" else current + key
                }
                else -> {
                    if (current.length >= 15) current
                    else if (current == "0") key
                    else current + key
                }
            }
            state.copy(amount = newValue)
        }
    }

    fun onFromCurrencyChange(currency: CurrencyInfo) {
        _uiState.update { it.copy(fromCurrency = currency) }
    }

    fun onToCurrencyChange(currency: CurrencyInfo) {
        _uiState.update { it.copy(toCurrency = currency) }
    }

    fun swapCurrencies() {
        _uiState.update { it.copy(fromCurrency = it.toCurrency, toCurrency = it.fromCurrency) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun refreshRates() {
        viewModelScope.launch {
            try {
                repository.refreshRates("USD")
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun needsRefresh(from: String, to: String, rates: Map<String, Double>): Boolean {
        return listOf(from, to).any { code -> code != "USD" && code !in rates }
    }

    private fun recalculate(
        amountStr: String,
        from: String,
        to: String,
        rates: Map<String, Double>
    ): Double? {
        val amount = amountStr.replace(",", ".").toDoubleOrNull() ?: return null
        return convertCurrency(amount, from, to, rates, baseCurrency = "USD")
    }

    private fun calculateBatch(
        amountStr: String,
        from: String,
        rates: Map<String, Double>
    ): Map<String, Double> {
        val amount = amountStr.replace(",", ".").toDoubleOrNull() ?: return emptyMap()
        val batchCodes = listOf("TRY", "EUR", "USD", "GBP", "CHF", "JPY", "CAD", "AUD").filter { it != from }
        return batchCodes.mapNotNull { to ->
            val value = convertCurrency(amount, from, to, rates, baseCurrency = "USD") ?: return@mapNotNull null
            to to value
        }.toMap()
    }
}
