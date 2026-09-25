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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject

data class MetalQuote(
    val code: String,
    val name: String,
    val flag: String,
    val rate: Double,
    val source: String = "",
    val retrievedAt: Long = 0,
    val cached: Boolean = false
)

data class CachedMetal(val price: Double, val source: String, val retrievedAt: Long)

data class MetalsUiState(
    val quote: String = "USD",
    val metals: List<MetalQuote> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val fxTimestamp: Long? = null,
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
    private val cache = application.getSharedPreferences("metal_quotes", android.content.Context.MODE_PRIVATE)
    private val gson = Gson()
    private var refreshJob: Job? = null
    private val _uiState = MutableStateFlow(MetalsUiState())
    val uiState: StateFlow<MetalsUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = it.metals.isEmpty(),
                    isRefreshing = it.metals.isNotEmpty(),
                    error = null
                )
            }
            try {
                val quote = userPrefs.lastDashboardBase.first()
                var rateSnapshot = getExchangeRates("USD").first()
                var usdRates = rateSnapshot?.rates ?: emptyMap()
                _uiState.update { it.copy(fxTimestamp = rateSnapshot?.timestamp) }
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        gson.fromJson<Map<String, CachedMetal>>(cache.getString("quotes", "{}"),
                            object : TypeToken<Map<String, CachedMetal>>() {}.type) ?: emptyMap()
                    }.getOrDefault(emptyMap()).filter { it.key in METAL_CODES && it.value.price.isFinite() && it.value.price > 0 }
                }
                if (saved.isNotEmpty()) publishQuotes(saved, quote, usdRates, saved.keys, refreshing = true)
                try { repository.refreshRates("USD") } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
                rateSnapshot = getExchangeRates("USD").first()
                usdRates = rateSnapshot?.rates ?: usdRates
                _uiState.update { it.copy(fxTimestamp = rateSnapshot?.timestamp) }
                val fresh = fetchUsdPerOunce()
                val usdPerOz = saved + fresh
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
                withContext(Dispatchers.IO) {
                    cache.edit().putString("quotes", gson.toJson(usdPerOz)).commit()
                }
                publishQuotes(usdPerOz, quote, usdRates, saved.keys - fresh.keys, refreshing = false)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
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

    private fun publishQuotes(prices: Map<String, CachedMetal>, quote: String, rates: Map<String, Double>, cached: Set<String>, refreshing: Boolean) {
        val factor = if (quote == "USD") 1.0 else rates[quote]?.takeIf { it.isFinite() && it > 0 }
        val displayQuote = if (factor != null) quote else "USD"
        val metals = METAL_CODES.mapNotNull { code ->
            val value = prices[code] ?: return@mapNotNull null
            val info = catalog[code]
            MetalQuote(code, info?.let { application.currencyName(it.code) } ?: code,
                METAL_ICONS[code] ?: "🥇", value.price * (factor ?: 1.0), value.source, value.retrievedAt, code in cached)
        }
        _uiState.update { it.copy(quote = displayQuote, metals = metals, isLoading = false, isRefreshing = refreshing, error = null) }
    }

    private suspend fun fetchUsdPerOunce(): Map<String, CachedMetal> {
        val fromGold = fetchGoldApi()
        if (fromGold.size == METAL_CODES.size) return fromGold
        val fromFawaz = fetchFawaz()
        return METAL_CODES.mapNotNull { code ->
            val price = fromGold[code] ?: fromFawaz[code] ?: return@mapNotNull null
            code to price
        }.toMap()
    }

    private suspend fun fetchGoldApi(): Map<String, CachedMetal> = coroutineScope {
        METAL_CODES.map { code ->
            async {
                runCatching {
                    val price = goldPriceApi.getPrice(code).price
                    if (price.isFinite() && price > 0) code to CachedMetal(price, "Gold API", System.currentTimeMillis()) else null
                }.onFailure { if (it is CancellationException) throw it }.getOrNull()
            }
        }.mapNotNull { it.await() }.toMap()
    }

    private suspend fun fetchFawaz(): Map<String, CachedMetal> = runCatching {
        val usd = fawazCurrencyApi.getUsd().usd
        METAL_CODES.mapNotNull { code ->
            val ozPerUsd = usd[code.lowercase()] ?: return@mapNotNull null
            if (!ozPerUsd.isFinite() || ozPerUsd <= 0.0 || !(1.0 / ozPerUsd).isFinite()) return@mapNotNull null
            code to CachedMetal(1.0 / ozPerUsd, "Fawaz Currency API", System.currentTimeMillis())
        }.toMap()
    }.onFailure { if (it is CancellationException) throw it }.getOrDefault(emptyMap())

    companion object {
        private val METAL_ICONS = mapOf(
            "XAU" to "🥇",
            "XAG" to "🥈",
            "XPT" to "⚪",
            "XPD" to "🔘"
        )
    }
}
