package com.hayhak.currencyconverter.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hayhak.currencyconverter.domain.model.AppBackup
import com.hayhak.currencyconverter.domain.model.QuietHours
import com.hayhak.currencyconverter.domain.model.RateAlarm
import com.hayhak.currencyconverter.domain.model.RatePromptState
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        val FAVORITE_ORDER = stringPreferencesKey("favorite_order")
        val SPREAD_PERCENT = doublePreferencesKey("spread_percent")
        val QUIET_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_START = intPreferencesKey("quiet_hours_start")
        val QUIET_END = intPreferencesKey("quiet_hours_end")
        val SAVED_PAIRS = stringPreferencesKey("saved_pairs")
        val MORNING_DIGEST = booleanPreferencesKey("morning_digest")
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        val LAST_FROM = stringPreferencesKey("last_from")
        val LAST_TO = stringPreferencesKey("last_to")
        val HISTORY_BASE = stringPreferencesKey("history_base")
        val HISTORY_TARGET = stringPreferencesKey("history_target")
        val HISTORY_RANGE = stringPreferencesKey("history_range")
        val HISTORY_COMPARE_BASE = stringPreferencesKey("history_compare_base")
        val HISTORY_COMPARE_TARGET = stringPreferencesKey("history_compare_target")
        val DASHBOARD_BASE = stringPreferencesKey("dashboard_base")
        val ALARMS = stringPreferencesKey("alarms")
        val WIDGET_BASE = stringPreferencesKey("widget_base")
        val WIDGET_CODES = stringPreferencesKey("widget_codes")
        val RATE_PROMPT_FIRST_SEEN = longPreferencesKey("rate_prompt_first_seen")
        val RATE_PROMPT_LAST_SHOWN = longPreferencesKey("rate_prompt_last_shown")
        val RATE_PROMPT_ACCEPTED = booleanPreferencesKey("rate_prompt_accepted")
    }

    private val defaultFavorites = setOf("USD", "EUR", "GBP", "JPY")
    private val defaultWidgetCodes = listOf("USD", "EUR", "GBP", "CHF")

    override val favorites: Flow<Set<String>> = context.dataStore.data.map {
        it[PreferencesKeys.FAVORITES] ?: defaultFavorites
    }

    override val favoriteOrder: Flow<List<String>> = context.dataStore.data.map { prefs ->
        orderedFavorites(prefs[PreferencesKeys.FAVORITES] ?: defaultFavorites, prefs[PreferencesKeys.FAVORITE_ORDER])
    }

    override val spreadPercent: Flow<Double> = context.dataStore.data.map {
        (it[PreferencesKeys.SPREAD_PERCENT] ?: 1.0).coerceIn(0.0, 20.0)
    }

    override val quietHours: Flow<QuietHours> = context.dataStore.data.map {
        QuietHours(
            enabled = it[PreferencesKeys.QUIET_ENABLED] ?: false,
            startMinute = it[PreferencesKeys.QUIET_START] ?: (23 * 60),
            endMinute = it[PreferencesKeys.QUIET_END] ?: (8 * 60)
        )
    }

    override val savedPairs: Flow<List<Pair<String, String>>> = context.dataStore.data.map {
        decodePairs(it[PreferencesKeys.SAVED_PAIRS])
    }

    override val analyticsEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[PreferencesKeys.ANALYTICS_ENABLED] ?: false
    }

    override val morningDigest: Flow<Boolean> = context.dataStore.data.map {
        it[PreferencesKeys.MORNING_DIGEST] ?: true
    }

    override val lastFromCurrency: Flow<String> = context.dataStore.data.map {
        it[PreferencesKeys.LAST_FROM] ?: "USD"
    }

    override val lastToCurrency: Flow<String> = context.dataStore.data.map {
        it[PreferencesKeys.LAST_TO] ?: "EUR"
    }

    override val lastHistoryBase: Flow<String> = context.dataStore.data.map {
        it[PreferencesKeys.HISTORY_BASE] ?: "USD"
    }

    override val lastHistoryTarget: Flow<String> = context.dataStore.data.map {
        it[PreferencesKeys.HISTORY_TARGET] ?: "EUR"
    }

    override val historyRange: Flow<String> = context.dataStore.data.map {
        it[PreferencesKeys.HISTORY_RANGE] ?: ""
    }

    override val historyCompare: Flow<Pair<String, String>> = context.dataStore.data.map {
        (it[PreferencesKeys.HISTORY_COMPARE_BASE] ?: "EUR") to (it[PreferencesKeys.HISTORY_COMPARE_TARGET] ?: "USD")
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
            val order = orderedFavorites(current, prefs[PreferencesKeys.FAVORITE_ORDER]).toMutableList()
            if (code in current) order.remove(code) else if (code !in order) order.add(code)
            prefs[PreferencesKeys.FAVORITE_ORDER] = order.filter { it in next }.joinToString(",")
        }
    }

    override suspend fun moveFavorite(fromIndex: Int, toIndex: Int) {
        context.dataStore.edit { prefs ->
            val set = prefs[PreferencesKeys.FAVORITES] ?: defaultFavorites
            val order = orderedFavorites(set, prefs[PreferencesKeys.FAVORITE_ORDER]).toMutableList()
            if (fromIndex !in order.indices || toIndex !in order.indices || fromIndex == toIndex) return@edit
            val item = order.removeAt(fromIndex)
            order.add(toIndex, item)
            prefs[PreferencesKeys.FAVORITE_ORDER] = order.joinToString(",")
        }
    }

    override suspend fun setSpreadPercent(percent: Double) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SPREAD_PERCENT] = percent.coerceIn(0.0, 20.0)
        }
    }

    override suspend fun setQuietHours(hours: QuietHours) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.QUIET_ENABLED] = hours.enabled
            prefs[PreferencesKeys.QUIET_START] = hours.startMinute.coerceIn(0, 24 * 60 - 1)
            prefs[PreferencesKeys.QUIET_END] = hours.endMinute.coerceIn(0, 24 * 60 - 1)
        }
    }

    override suspend fun savePair(from: String, to: String) {
        if (from.equals(to, ignoreCase = true)) return
        context.dataStore.edit { prefs ->
            val next = decodePairs(prefs[PreferencesKeys.SAVED_PAIRS]).toMutableList()
            val pair = from.uppercase() to to.uppercase()
            if (pair !in next) next.add(0, pair)
            prefs[PreferencesKeys.SAVED_PAIRS] = encodePairs(next.take(12))
        }
    }

    override suspend fun removePair(from: String, to: String) {
        context.dataStore.edit { prefs ->
            val next = decodePairs(prefs[PreferencesKeys.SAVED_PAIRS])
                .filterNot { it.first == from.uppercase() && it.second == to.uppercase() }
            prefs[PreferencesKeys.SAVED_PAIRS] = encodePairs(next)
        }
    }

    override suspend fun setAnalyticsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.ANALYTICS_ENABLED] = enabled }
    }

    override suspend fun setMorningDigest(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.MORNING_DIGEST] = enabled }
    }

    override suspend fun exportBackup(): AppBackup {
        val prefs = context.dataStore.data.first()
        val favorites = orderedFavorites(
            prefs[PreferencesKeys.FAVORITES] ?: defaultFavorites,
            prefs[PreferencesKeys.FAVORITE_ORDER]
        )
        val alarmJson = prefs[PreferencesKeys.ALARMS] ?: "[]"
        val alarmType = object : TypeToken<List<RateAlarm>>() {}.type
        val alarms: List<RateAlarm> = gson.fromJson(alarmJson, alarmType)
        val widgetCodes = prefs[PreferencesKeys.WIDGET_CODES]
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: defaultWidgetCodes
        return AppBackup(
            favorites = favorites,
            alarms = alarms,
            widgetBase = prefs[PreferencesKeys.WIDGET_BASE] ?: (prefs[PreferencesKeys.DASHBOARD_BASE] ?: "USD"),
            widgetCodes = widgetCodes,
            spreadPercent = (prefs[PreferencesKeys.SPREAD_PERCENT] ?: 1.0).coerceIn(0.0, 20.0),
            quietHoursEnabled = prefs[PreferencesKeys.QUIET_ENABLED] ?: false,
            quietStartMinute = prefs[PreferencesKeys.QUIET_START] ?: (23 * 60),
            quietEndMinute = prefs[PreferencesKeys.QUIET_END] ?: (8 * 60),
            savedPairs = decodePairs(prefs[PreferencesKeys.SAVED_PAIRS]).map { "${it.first}>${it.second}" },
            morningDigest = prefs[PreferencesKeys.MORNING_DIGEST] ?: true
        )
    }

    override suspend fun importBackup(backup: AppBackup) {
        val codes = backup.favorites.map { it.trim().uppercase() }.filter { it.length == 3 }.distinct()
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FAVORITES] = codes.toSet()
            prefs[PreferencesKeys.FAVORITE_ORDER] = codes.joinToString(",")
            prefs[PreferencesKeys.ALARMS] = gson.toJson(backup.alarms)
            prefs[PreferencesKeys.WIDGET_BASE] = backup.widgetBase.ifBlank { "USD" }
            prefs[PreferencesKeys.WIDGET_CODES] = backup.widgetCodes.joinToString(",")
            prefs[PreferencesKeys.SPREAD_PERCENT] = backup.spreadPercent.coerceIn(0.0, 20.0)
            prefs[PreferencesKeys.QUIET_ENABLED] = backup.quietHoursEnabled
            prefs[PreferencesKeys.QUIET_START] = backup.quietStartMinute.coerceIn(0, 24 * 60 - 1)
            prefs[PreferencesKeys.QUIET_END] = backup.quietEndMinute.coerceIn(0, 24 * 60 - 1)
            backup.savedPairs?.let { prefs[PreferencesKeys.SAVED_PAIRS] = encodePairs(decodePairs(it.joinToString("|"))) }
            backup.morningDigest?.let { prefs[PreferencesKeys.MORNING_DIGEST] = it }
        }
    }

    private fun decodePairs(raw: String?): List<Pair<String, String>> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split('|').mapNotNull { token ->
            val parts = token.split('>')
            if (parts.size != 2) return@mapNotNull null
            val from = parts[0].trim().uppercase()
            val to = parts[1].trim().uppercase()
            if (from.length != 3 || to.length != 3 || from == to) null else from to to
        }.distinct()
    }

    private fun encodePairs(pairs: List<Pair<String, String>>): String =
        pairs.joinToString("|") { "${it.first}>${it.second}" }

    private fun orderedFavorites(set: Set<String>, raw: String?): List<String> {
        if (raw == null) {
            val defaults = defaultFavorites.filter { it in set }
            return defaults + set.filter { it !in defaults }
        }
        val stored = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() && it in set }
        return stored + set.filter { it !in stored }
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

    override suspend fun saveHistoryRange(range: String) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.HISTORY_RANGE] = range }
    }

    override suspend fun saveHistoryCompare(base: String, target: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HISTORY_COMPARE_BASE] = base
            prefs[PreferencesKeys.HISTORY_COMPARE_TARGET] = target
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
        if (raw == null) defaultWidgetCodes
        else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
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

    override suspend fun ensureRatePromptFirstSeen() {
        @Suppress("DEPRECATION")
        val installTime = context.packageManager
            .getPackageInfo(context.packageName, 0)
            .firstInstallTime
        context.dataStore.edit { prefs ->
            if ((prefs[PreferencesKeys.RATE_PROMPT_FIRST_SEEN] ?: 0L) <= 0L) {
                prefs[PreferencesKeys.RATE_PROMPT_FIRST_SEEN] = installTime
            }
        }
    }

    override suspend fun ratePromptState(): RatePromptState {
        val prefs = context.dataStore.data.first()
        return RatePromptState(
            firstSeenAt = prefs[PreferencesKeys.RATE_PROMPT_FIRST_SEEN] ?: 0L,
            lastShownAt = prefs[PreferencesKeys.RATE_PROMPT_LAST_SHOWN] ?: 0L,
            accepted = prefs[PreferencesKeys.RATE_PROMPT_ACCEPTED] ?: false
        )
    }

    override suspend fun markRatePromptShown(now: Long) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.RATE_PROMPT_LAST_SHOWN] = now
        }
    }

    override suspend fun markRatePromptAccepted() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.RATE_PROMPT_ACCEPTED] = true
        }
    }
}
