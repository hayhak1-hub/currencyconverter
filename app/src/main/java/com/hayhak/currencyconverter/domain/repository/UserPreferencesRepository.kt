package com.hayhak.currencyconverter.domain.repository

import com.hayhak.currencyconverter.domain.model.AppBackup
import com.hayhak.currencyconverter.domain.model.QuietHours
import com.hayhak.currencyconverter.domain.model.RateAlarm
import com.hayhak.currencyconverter.domain.model.RatePromptState
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val favorites: Flow<Set<String>>
    val favoriteOrder: Flow<List<String>>
    val spreadPercent: Flow<Double>
    val quietHours: Flow<QuietHours>
    val savedPairs: Flow<List<Pair<String, String>>>
    val morningDigest: Flow<Boolean>
    val analyticsEnabled: Flow<Boolean>
    val lastFromCurrency: Flow<String>
    val lastToCurrency: Flow<String>
    val lastHistoryBase: Flow<String>
    val lastHistoryTarget: Flow<String>
    val historyRange: Flow<String>
    val historyCompare: Flow<Pair<String, String>>
    val lastDashboardBase: Flow<String>
    val alarms: Flow<List<RateAlarm>>
    val widgetBase: Flow<String>
    val widgetCodes: Flow<List<String>>

    suspend fun toggleFavorite(code: String)
    suspend fun moveFavorite(fromIndex: Int, toIndex: Int)
    suspend fun exportBackup(): AppBackup
    suspend fun importBackup(backup: AppBackup)
    suspend fun saveLastConverterCurrencies(from: String, to: String)
    suspend fun saveLastHistoryCurrencies(base: String, target: String)
    suspend fun saveHistoryRange(range: String)
    suspend fun saveHistoryCompare(base: String, target: String)
    suspend fun saveLastDashboardBase(base: String)
    suspend fun addAlarm(alarm: RateAlarm)
    suspend fun updateAlarm(alarm: RateAlarm)
    suspend fun removeAlarm(id: String)
    suspend fun saveWidgetConfig(base: String, codes: List<String>)
    suspend fun setSpreadPercent(percent: Double)
    suspend fun setQuietHours(hours: QuietHours)
    suspend fun savePair(from: String, to: String)
    suspend fun removePair(from: String, to: String)
    suspend fun setMorningDigest(enabled: Boolean)
    suspend fun setAnalyticsEnabled(enabled: Boolean)

    suspend fun ensureRatePromptFirstSeen()
    suspend fun ratePromptState(): RatePromptState
    suspend fun markRatePromptShown(now: Long)
    suspend fun markRatePromptAccepted()
}
