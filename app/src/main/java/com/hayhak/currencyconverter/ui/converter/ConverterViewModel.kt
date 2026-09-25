package com.hayhak.currencyconverter.ui.converter

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayhak.currencyconverter.domain.model.CurrencyInfo
import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import com.hayhak.currencyconverter.domain.usecase.ConvertCurrencyUseCase
import com.hayhak.currencyconverter.domain.usecase.GetExchangeRatesUseCase
import com.hayhak.currencyconverter.util.AnalyticsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.LinkedHashMap
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class ConverterViewModel @Inject constructor(
    private val getExchangeRates: GetExchangeRatesUseCase,
    private val convertCurrency: ConvertCurrencyUseCase,
    private val repository: ExchangeRateRepository,
    private val userPrefs: UserPreferencesRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConverterUiState(isLoading = true))
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    private var accumulator: Double? = null
    private var pendingOp: String? = null
    private var replaceOnNextDigit = false
    private var appliedLaunchPair = false

    init {
        userPrefs.favorites
            .onEach { favs -> _uiState.update { it.copy(favorites = favs) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val from = userPrefs.lastFromCurrency.first()
            val to = userPrefs.lastToCurrency.first()
            if (appliedLaunchPair) return@launch
            val fromInfo = SUPPORTED_CURRENCIES.find { it.code == from }
            val toInfo = SUPPORTED_CURRENCIES.find { it.code == to }
            if (fromInfo != null && toInfo != null && from != to) {
                _uiState.update { it.copy(fromCurrency = fromInfo, toCurrency = toInfo) }
            }
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
                    val batchResults = calculateBatch(
                        state.amount,
                        state.fromCurrency.code,
                        rates,
                        state.favorites
                    )
                    state.copy(
                        rates = rates,
                        isLoading = false,
                        lastUpdated = exchangeRate?.timestamp,
                        error = if (exchangeRate == null) "offline" else null,
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
                    old.toCurrency == new.toCurrency &&
                    old.favorites == new.favorites
            }
            .onEach { state ->
                val result = recalculate(
                    state.amount,
                    state.fromCurrency.code,
                    state.toCurrency.code,
                    state.rates
                )
                val batchResults = calculateBatch(
                    state.amount,
                    state.fromCurrency.code,
                    state.rates,
                    state.favorites
                )
                _uiState.update { it.copy(result = result, batchResults = batchResults) }
                userPrefs.saveLastConverterCurrencies(state.fromCurrency.code, state.toCurrency.code)
                AnalyticsHelper.logConversion(appContext, state.fromCurrency.code, state.toCurrency.code)
                if (needsRefresh(state.fromCurrency.code, state.toCurrency.code, state.rates)) {
                    refreshRates()
                }
            }
            .launchIn(viewModelScope)
    }

    fun startAmountEntry() {
        accumulator = null
        pendingOp = null
        replaceOnNextDigit = false
        _uiState.update { it.copy(amount = "") }
    }

    fun reuseConversion(entry: com.hayhak.currencyconverter.domain.model.SavedConversion) {
        applyPair(entry.from, entry.to)
        accumulator = null
        pendingOp = null
        replaceOnNextDigit = false
        _uiState.update { it.copy(amount = entry.amount) }
    }

    fun finishAmountEntry() {
        equals()
        _uiState.update { state ->
            if (state.amount.isEmpty()) state.copy(amount = "1") else state
        }
    }

    fun onNumpadClick(key: String) {
        when (key) {
            "C" -> {
                accumulator = null
                pendingOp = null
                replaceOnNextDigit = false
                _uiState.update { it.copy(amount = "") }
            }
            "DEL" -> _uiState.update { it.copy(amount = it.amount.dropLast(1)) }
            "+", "-", "×", "÷", "*", "/" -> applyOperator(normalizeOp(key))
            "=" -> equals()
            "%" -> percent()
            ",", "." -> appendDecimal(key)
            else -> appendDigit(key)
        }
    }

    fun onFromCurrencyChange(currency: CurrencyInfo) {
        _uiState.update { it.copy(fromCurrency = currency) }
    }

    fun onToCurrencyChange(currency: CurrencyInfo) {
        _uiState.update { it.copy(toCurrency = currency) }
    }

    fun applyPair(from: String, to: String) {
        val fromInfo = SUPPORTED_CURRENCIES.find { it.code == from } ?: return
        val toInfo = SUPPORTED_CURRENCIES.find { it.code == to } ?: return
        if (from == to) return
        appliedLaunchPair = true
        _uiState.update { it.copy(fromCurrency = fromInfo, toCurrency = toInfo) }
    }

    fun swapCurrencies() {
        _uiState.update { it.copy(fromCurrency = it.toCurrency, toCurrency = it.fromCurrency) }
    }

    fun refreshRates() {
        viewModelScope.launch {
            try {
                repository.refreshRates("USD")
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = it.error ?: "offline") }
            }
        }
    }

    private fun normalizeOp(key: String) = when (key) {
        "×", "*" -> "×"
        "÷", "/" -> "÷"
        else -> key
    }

    private fun appendDigit(key: String) {
        _uiState.update { state ->
            val current = if (replaceOnNextDigit) "" else state.amount
            replaceOnNextDigit = false
            val next = when {
                current.length >= 15 -> current
                current == "0" -> key
                else -> current + key
            }
            state.copy(amount = next)
        }
    }

    private fun appendDecimal(sep: String) {
        _uiState.update { state ->
            var current = if (replaceOnNextDigit) "" else state.amount
            replaceOnNextDigit = false
            if (current.contains(',') || current.contains('.')) return@update state
            if (current.isEmpty()) current = "0"
            state.copy(amount = current + sep)
        }
    }

    private fun applyOperator(op: String) {
        val current = parseAmount(_uiState.value.amount)
        val acc = accumulator
        val pending = pendingOp
        val result = if (acc != null && pending != null && current != null && !replaceOnNextDigit) {
            compute(acc, current, pending)
        } else {
            current ?: acc
        }
        accumulator = result
        pendingOp = op
        replaceOnNextDigit = true
        if (result != null) {
            _uiState.update { it.copy(amount = formatCalc(result)) }
        }
    }

    private fun equals() {
        val current = parseAmount(_uiState.value.amount)
        val acc = accumulator
        val pending = pendingOp
        if (acc != null && pending != null && current != null) {
            val result = compute(acc, current, pending)
            if (result != null) {
                _uiState.update { it.copy(amount = formatCalc(result)) }
            }
        }
        accumulator = null
        pendingOp = null
        replaceOnNextDigit = true
    }

    private fun percent() {
        val current = parseAmount(_uiState.value.amount) ?: return
        _uiState.update { it.copy(amount = formatCalc(current / 100.0)) }
        replaceOnNextDigit = true
    }

    private fun compute(a: Double, b: Double, op: String): Double? = when (op) {
        "+" -> a + b
        "-" -> a - b
        "×" -> a * b
        "÷" -> if (b == 0.0) null else a / b
        else -> b
    }

    private fun parseAmount(raw: String): Double? =
        raw.replace(",", ".").toDoubleOrNull()

    private fun formatCalc(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return ""
        val asLong = value.toLong()
        return if (value == asLong.toDouble() && kotlin.math.abs(value) < 1e12) {
            asLong.toString()
        } else {
            "%.8f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
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
        val amount = parseAmount(amountStr) ?: return null
        return convertCurrency(amount, from, to, rates, baseCurrency = "USD")
    }

    private fun calculateBatch(
        amountStr: String,
        from: String,
        rates: Map<String, Double>,
        favorites: Set<String>
    ): Map<String, Double> {
        val amount = parseAmount(amountStr) ?: return emptyMap()
        val ordered = (favorites.toList() + SUPPORTED_CURRENCIES.map { it.code })
            .distinct()
            .filter { it != from }
        val out = LinkedHashMap<String, Double>()
        for (to in ordered) {
            val value = convertCurrency(amount, from, to, rates, baseCurrency = "USD") ?: continue
            out[to] = value
        }
        return out
    }
}
