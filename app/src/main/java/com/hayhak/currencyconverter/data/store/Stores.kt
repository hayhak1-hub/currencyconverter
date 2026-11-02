package com.hayhak.currencyconverter.data.store

import android.content.Context
import com.google.gson.reflect.TypeToken
import com.hayhak.currencyconverter.data.store.model.AlarmEvent
import com.hayhak.currencyconverter.data.store.model.PortfolioEntry
import com.hayhak.currencyconverter.data.store.model.Trip
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Alerts that fired, newest first, capped so the file stays small. */
@Singleton
class AlarmHistoryStore @Inject constructor(@ApplicationContext context: Context) {
    private val store = JsonListStore<AlarmEvent>(
        context, "alarm_history", object : TypeToken<List<AlarmEvent>>() {}.type
    )
    val events: Flow<List<AlarmEvent>> = store.items

    suspend fun record(event: AlarmEvent) = store.update { (listOf(event) + it).take(MAX_EVENTS) }
    suspend fun clear() = store.update { emptyList() }

    companion object { const val MAX_EVENTS = 100 }
}

@Singleton
class PortfolioStore @Inject constructor(@ApplicationContext context: Context) {
    private val store = JsonListStore<PortfolioEntry>(
        context, "portfolio", object : TypeToken<List<PortfolioEntry>>() {}.type
    )
    val entries: Flow<List<PortfolioEntry>> = store.items

    suspend fun add(entry: PortfolioEntry) = store.update { it + entry }
    suspend fun remove(id: String) = store.update { list -> list.filterNot { it.id == id } }
}

@Singleton
class TripStore @Inject constructor(@ApplicationContext context: Context) {
    private val store = JsonListStore<Trip>(
        context, "trips", object : TypeToken<List<Trip>>() {}.type
    )
    val trips: Flow<List<Trip>> = store.items

    suspend fun add(trip: Trip) = store.update { it + trip }
    suspend fun remove(id: String) = store.update { list -> list.filterNot { it.id == id } }
    suspend fun addExpense(tripId: String, expense: com.hayhak.currencyconverter.data.store.model.TripExpense) =
        store.update { list -> list.map { if (it.id == tripId) it.copy(expenses = it.expenses + expense) else it } }
    suspend fun removeExpense(tripId: String, expenseId: String) =
        store.update { list ->
            list.map { if (it.id == tripId) it.copy(expenses = it.expenses.filterNot { e -> e.id == expenseId }) else it }
        }
}
