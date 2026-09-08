package com.hayhak.currencyconverter.ui.metals

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayhak.currencyconverter.domain.model.METAL_CODES
import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES
import com.hayhak.currencyconverter.domain.model.quotePerUnit
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import com.hayhak.currencyconverter.domain.usecase.GetExchangeRatesUseCase
import com.hayhak.currencyconverter.util.currencyName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MetalQuote(
    val code: String,
    val name: String,
    val flag: String,
    val rate: Double
)

data class MetalsUiState(
    val quote: String = "USD",
    val metals: List<MetalQuote> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class MetalsViewModel @Inject constructor(
    private val getExchangeRates: GetExchangeRatesUseCase,
    private val repository: ExchangeRateRepository,
    private val userPrefs: UserPreferencesRepository,
    private val application: Application
) : ViewModel() {

    private val catalog = SUPPORTED_CURRENCIES.associateBy { it.code }
    private val _uiState = MutableStateFlow(MetalsUiState())
    val uiState: StateFlow<MetalsUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.refreshRates("USD")
        }
        viewModelScope.launch {
            combine(getExchangeRates("USD"), userPrefs.lastDashboardBase) { rates, base ->
                rates to base
            }.collect { (exchange, base) ->
                val usd = exchange?.rates ?: emptyMap()
                val metals = METAL_CODES.mapNotNull { code ->
                    val rate = quotePerUnit(code, base, usd) ?: return@mapNotNull null
                    val info = catalog[code]
                    MetalQuote(
                        code = code,
                        name = info?.let { application.currencyName(it.code) } ?: code,
                        flag = info?.flag ?: "🥇",
                        rate = rate
                    )
                }
                _uiState.update { it.copy(quote = base, metals = metals, isLoading = false) }
            }
        }
    }
}
