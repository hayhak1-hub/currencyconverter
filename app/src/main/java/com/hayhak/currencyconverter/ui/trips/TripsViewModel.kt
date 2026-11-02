package com.hayhak.currencyconverter.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayhak.currencyconverter.data.store.TripStore
import com.hayhak.currencyconverter.data.store.model.Trip
import com.hayhak.currencyconverter.data.store.model.TripExpense
import com.hayhak.currencyconverter.domain.model.BASE_CURRENCY
import com.hayhak.currencyconverter.domain.model.CurrencyPairDefaults
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import com.hayhak.currencyconverter.domain.usecase.ObserveCurrencyDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TripsUiState(
    val trips: List<Trip> = emptyList(),
    val selected: Trip? = null,
    val status: TripStatus? = null
)

@HiltViewModel
class TripsViewModel @Inject constructor(
    private val store: TripStore,
    private val repository: ExchangeRateRepository,
    observeDefaults: ObserveCurrencyDefaults
) : ViewModel() {

    private val selectedId = MutableStateFlow<String?>(null)

    val defaults: StateFlow<CurrencyPairDefaults> = observeDefaults()
        .stateIn(viewModelScope, SharingStarted.Eagerly, CurrencyPairDefaults("USD", "EUR"))

    val uiState: StateFlow<TripsUiState> = combine(
        store.trips,
        repository.getLatestRates(BASE_CURRENCY),
        selectedId
    ) { trips, latest, chosen ->
        // Fall back to the newest trip when nothing is picked or the picked one was deleted.
        val selected = trips.firstOrNull { it.id == chosen } ?: trips.lastOrNull()
        TripsUiState(
            trips = trips,
            selected = selected,
            status = selected?.let { tripStatus(it, latest?.rates.orEmpty()) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripsUiState())

    init {
        viewModelScope.launch { runCatching { repository.refreshRates(BASE_CURRENCY) } }
    }

    fun select(id: String) {
        selectedId.value = id
    }

    fun createTrip(name: String, localCode: String, homeCode: String, budget: Double) {
        val trip = Trip(
            name = name.trim().ifBlank { localCode },
            localCode = localCode,
            homeCode = homeCode,
            budget = budget,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            store.add(trip)
            selectedId.value = trip.id
        }
    }

    fun deleteTrip(id: String) {
        viewModelScope.launch { store.remove(id) }
    }

    fun addExpense(tripId: String, amount: Double, label: String) {
        viewModelScope.launch {
            store.addExpense(tripId, TripExpense(amount = amount, label = label.trim(), at = System.currentTimeMillis()))
        }
    }

    fun removeExpense(tripId: String, expenseId: String) {
        viewModelScope.launch { store.removeExpense(tripId, expenseId) }
    }
}
