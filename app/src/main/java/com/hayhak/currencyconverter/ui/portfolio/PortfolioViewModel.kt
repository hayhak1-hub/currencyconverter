package com.hayhak.currencyconverter.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayhak.currencyconverter.data.store.PortfolioStore
import com.hayhak.currencyconverter.data.store.model.PortfolioEntry
import com.hayhak.currencyconverter.domain.model.BASE_CURRENCY
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import com.hayhak.currencyconverter.domain.model.CurrencyPairDefaults
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import com.hayhak.currencyconverter.domain.usecase.ObserveCurrencyDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PortfolioUiState(
    val summary: PortfolioSummary? = null,
    val isEmpty: Boolean = true,
    val displayCode: String = "USD"
)

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val store: PortfolioStore,
    private val repository: ExchangeRateRepository,
    private val userPrefs: UserPreferencesRepository,
    observeDefaults: ObserveCurrencyDefaults
) : ViewModel() {

    private val displayCode = MutableStateFlow<String?>(null)

    val defaults: StateFlow<CurrencyPairDefaults> = observeDefaults()
        .stateIn(viewModelScope, SharingStarted.Eagerly, CurrencyPairDefaults("USD", "EUR"))

    val uiState: StateFlow<PortfolioUiState> = combine(
        store.entries,
        repository.getLatestRates(BASE_CURRENCY),
        displayCode
    ) { entries, latest, chosen ->
        val display = chosen ?: entries.firstOrNull()?.costCode ?: BASE_CURRENCY
        val rates = latest?.rates.orEmpty()
        PortfolioUiState(
            summary = if (entries.isEmpty() || rates.isEmpty()) null else portfolioSummary(entries, rates, display),
            isEmpty = entries.isEmpty(),
            displayCode = display
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PortfolioUiState())

    init {
        viewModelScope.launch {
            runCatching { repository.refreshRates(BASE_CURRENCY) }
            // With nothing recorded yet, start from the currency used on the Rates screen.
            if (displayCode.value == null && store.entries.first().isEmpty()) {
                displayCode.value = userPrefs.lastDashboardBase.first()
            }
        }
    }

    fun setDisplayCode(code: String) {
        displayCode.value = code
    }

    fun add(code: String, amount: Double, costPerUnit: Double, costCode: String) {
        viewModelScope.launch {
            store.add(
                PortfolioEntry(
                    code = code,
                    amount = amount,
                    costPerUnit = costPerUnit,
                    costCode = costCode,
                    boughtAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun remove(id: String) {
        viewModelScope.launch { store.remove(id) }
    }
}
