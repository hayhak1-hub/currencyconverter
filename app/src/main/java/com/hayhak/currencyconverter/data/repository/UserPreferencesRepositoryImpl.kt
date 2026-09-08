package com.hayhak.currencyconverter.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hayhak.currencyconverter.domain.model.RateAlarm
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private val gson = Gson()

    private object PreferencesKeys {
        val FAVORITES = stringSetPreferencesKey("favorites")
        val LAST_FROM = stringPreferencesKey("last_from")
        val LAST_TO = stringPreferencesKey("last_to")
        val HISTORY_BASE = stringPreferencesKey("history_base")
        val HISTORY_TARGET = stringPreferencesKey("history_target")
        val DASHBOARD_BASE = stringPreferencesKey("dashboard_base")
        val ALARMS = stringPreferencesKey("alarms")
        val WIDGET_BASE = stringPreferencesKey("widget_base")
        val WIDGET_CODES = stringPreferencesKey("widget_codes")
    }

    private val defaultFavorites = setOf("USD", "EUR", "GBP", "TRY")
    private val defaultWidgetCodes = listOf("USD", "EUR", "GBP", "CHF")

    override val favorites: Flow<Set<String>> = context.dataStore.data.map {
        it[PreferencesKeys.FAVORITES] ?: defaultFavorites
    }

    override val lastFromCurrency: Flow<String> = context.dataStore.data.map {
        it[PreferencesKeys.LAST_FROM] ?: "USD"
    }

    override val lastToCurrency: Flow<String> = context.dataStore.data.map {
        it[PreferencesKeys.LAST_TO] ?: "TRY"
    }

    override val lastHistoryBase: Flow<String> = context.dataStore.data.map {
        it[PreferencesKeys.HISTORY_BASE] ?: "USD"
    }

    override val lastHistoryTarget: Flow<String> = context.dataStore.data.map {
        it[PreferencesKeys.HISTORY_TARGET] ?: "TRY"
    }

    override val lastDashboardBase: Flow<String> = context.dataStore.data.map {
        it[PreferencesKeys.DASHBOARD_BASE] ?: "USD"
    }

    override val alarms: Flow<List<RateAlarm>> = context.dataStore.data.map {
        val json = it[PreferencesKeys.ALARMS] ?: "[]"
        val type = object : TypeToken<List<RateAlarm>>() {}.type
        gson.fromJson(json, type)
    }

    override suspend fun toggleFavorite(code: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.FAVORITES] ?: defaultFavorites
            val next = if (current.contains(code)) current - code else current + code
            prefs[PreferencesKeys.FAVORITES] = next
        }
    }

    override suspend fun saveLastConverterCurrencies(from: String, to: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_FROM] = from
            prefs[PreferencesKeys.LAST_TO] = to
        }
    }

    override suspend fun saveLastHistoryCurrencies(base: String, target: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HISTORY_BASE] = base
            prefs[PreferencesKeys.HISTORY_TARGET] = target
        }
    }

    override suspend fun saveLastDashboardBase(base: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DASHBOARD_BASE] = base
        }
    }

    override val widgetBase: Flow<String> = context.dataStore.data.map {
        it[PreferencesKeys.WIDGET_BASE] ?: (it[PreferencesKeys.DASHBOARD_BASE] ?: "USD")
    }

    override val widgetCodes: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[PreferencesKeys.WIDGET_CODES]
        if (raw.isNullOrBlank()) defaultWidgetCodes
        else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { defaultWidgetCodes }
    }

    override suspend fun addAlarm(alarm: RateAlarm) {
        context.dataStore.edit { prefs ->
            val json = prefs[PreferencesKeys.ALARMS] ?: "[]"
            val type = object : TypeToken<List<RateAlarm>>() {}.type
            val current: List<RateAlarm> = gson.fromJson(json, type)
            val next = current + alarm
            prefs[PreferencesKeys.ALARMS] = gson.toJson(next)
        }
    }

    override suspend fun updateAlarm(alarm: RateAlarm) {
        context.dataStore.edit { prefs ->
            val json = prefs[PreferencesKeys.ALARMS] ?: "[]"
            val type = object : TypeToken<List<RateAlarm>>() {}.type
            val current: List<RateAlarm> = gson.fromJson(json, type)
            val next = current.map { if (it.id == alarm.id) alarm else it }
            prefs[PreferencesKeys.ALARMS] = gson.toJson(next)
        }
    }

    override suspend fun removeAlarm(id: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[PreferencesKeys.ALARMS] ?: "[]"
            val type = object : TypeToken<List<RateAlarm>>() {}.type
            val current: List<RateAlarm> = gson.fromJson(json, type)
            val next = current.filter { it.id != id }
            prefs[PreferencesKeys.ALARMS] = gson.toJson(next)
        }
    }

    override suspend fun saveWidgetConfig(base: String, codes: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.WIDGET_BASE] = base
            prefs[PreferencesKeys.WIDGET_CODES] = codes.joinToString(",")
        }
    }
}
