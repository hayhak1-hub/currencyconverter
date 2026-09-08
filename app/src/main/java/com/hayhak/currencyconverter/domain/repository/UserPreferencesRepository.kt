package com.hayhak.currencyconverter.domain.repository

import com.hayhak.currencyconverter.domain.model.RateAlarm
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val favorites: Flow<Set<String>>
    val lastFromCurrency: Flow<String>
    val lastToCurrency: Flow<String>
    val lastHistoryBase: Flow<String>
    val lastHistoryTarget: Flow<String>
    val lastDashboardBase: Flow<String>
    val alarms: Flow<List<RateAlarm>>
    val widgetBase: Flow<String>
    val widgetCodes: Flow<List<String>>

    suspend fun toggleFavorite(code: String)
    suspend fun saveLastConverterCurrencies(from: String, to: String)
    suspend fun saveLastHistoryCurrencies(base: String, target: String)
    suspend fun saveLastDashboardBase(base: String)
    suspend fun addAlarm(alarm: RateAlarm)
    suspend fun updateAlarm(alarm: RateAlarm)
    suspend fun removeAlarm(id: String)
    suspend fun saveWidgetConfig(base: String, codes: List<String>)
}
