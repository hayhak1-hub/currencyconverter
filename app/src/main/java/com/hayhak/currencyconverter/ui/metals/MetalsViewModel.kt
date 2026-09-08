package com.hayhak.currencyconverter.ui.metals

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.data.remote.api.FawazCurrencyApi
import com.hayhak.currencyconverter.data.remote.api.GoldPriceApi
import com.hayhak.currencyconverter.domain.model.METAL_CODES
import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import com.hayhak.currencyconverter.domain.usecase.GetExchangeRatesUseCase
import com.hayhak.currencyconverter.util.currencyName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MetalsViewModel @Inject constructor(
    private val goldPriceApi: GoldPriceApi,
    private val fawazCurrencyApi: FawazCurrencyApi,
    private val getExchangeRates: GetExchangeRatesUseCase,
    private val repository: ExchangeRateRepository,
    private val userPrefs: UserPreferencesRepository,
    private val application: Application
) : ViewModel() {

    private val catalog = SUPPORTED_CURRENCIES.associateBy { it.code }
    private val _uiState = MutableStateFlow(MetalsUiState())
    val uiState: StateFlow<MetalsUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = it.metals.isEmpty(),
                    isRefreshing = it.metals.isNotEmpty(),
                    error = null
                )
            }
            try {
                runCatching { repository.refreshRates("USD") }
                val quote = userPrefs.lastDashboardBase.first()
                val usdRates = getExchangeRates("USD").first()?.rates ?: emptyMap()
                val usdPerOz = fetchUsdPerOunce()
                if (usdPerOz.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            quote = quote,
                            isLoading = false,
                            isRefreshing = false,
                            error = application.getString(R.string.metals_error)
                        )
                    }
                    return@launch
                }
                val quotePerUsd = if (quote == "USD") 1.0 else usdRates[quote]
                val displayQuote = if (quotePerUsd != null) quote else "USD"
                val factor = quotePerUsd ?: 1.0
                val metals = METAL_CODES.mapNotNull { code ->
                    val usdPrice = usdPerOz[code] ?: return@mapNotNull null
                    val info = catalog[code]
                    MetalQuote(
                        code = code,
                        name = info?.let { application.currencyName(it.code) } ?: code,
                        flag = METAL_ICONS[code] ?: info?.flag ?: "🥇",
                        rate = usdPrice * factor
                    )
                }
                _uiState.update {
                    it.copy(
                        quote = displayQuote,
                        metals = metals,
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = application.getString(R.string.metals_error)
                    )
                }
            }
        }
    }

    private suspend fun fetchUsdPerOunce(): Map<String, Double> {
        val fromGold = fetchGoldApi()
        if (fromGold.size == METAL_CODES.size) return fromGold
        val fromFawaz = fetchFawaz()
        return METAL_CODES.mapNotNull { code ->
            val price = fromGold[code] ?: fromFawaz[code] ?: return@mapNotNull null
            code to price
        }.toMap()
    }

    private suspend fun fetchGoldApi(): Map<String, Double> = coroutineScope {
        METAL_CODES.map { code ->
            async {
                runCatching {
                    val price = goldPriceApi.getPrice(code).price
                    if (price > 0) code to price else null
                }.getOrNull()
            }
        }.mapNotNull { it.await() }.toMap()
    }

    private suspend fun fetchFawaz(): Map<String, Double> = runCatching {
        val usd = fawazCurrencyApi.getUsd().usd
        METAL_CODES.mapNotNull { code ->
            val ozPerUsd = usd[code.lowercase()] ?: return@mapNotNull null
            if (ozPerUsd <= 0.0) return@mapNotNull null
            code to (1.0 / ozPerUsd)
        }.toMap()
    }.getOrDefault(emptyMap())

    companion object {
        private val METAL_ICONS = mapOf(
            "XAU" to "🥇",
            "XAG" to "🥈",
            "XPT" to "⚪",
            "XPD" to "🔘"
        )
    }
}
