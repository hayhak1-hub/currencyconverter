package com.hayhak.currencyconverter.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.hayhak.currencyconverter.domain.model.AppBackup
import com.hayhak.currencyconverter.domain.model.QuietHours
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsToolsViewModel @Inject constructor(
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    private val gson = Gson()

    val spread: StateFlow<Double> = userPrefs.spreadPercent.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 1.0
    )
    val quiet: StateFlow<QuietHours> = userPrefs.quietHours.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), QuietHours()
    )
    val digest: StateFlow<Boolean> = userPrefs.morningDigest.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true
    )

    val analytics: StateFlow<Boolean> = userPrefs.analyticsEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false
    )

    fun setAnalytics(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setAnalyticsEnabled(enabled) }
    }

    fun setSpread(percent: Double) {
        viewModelScope.launch { userPrefs.setSpreadPercent(percent) }
    }

    fun setQuiet(hours: QuietHours) {
        viewModelScope.launch { userPrefs.setQuietHours(hours) }
    }

    fun setDigest(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setMorningDigest(enabled) }
    }

    suspend fun exportJson(): String = gson.toJson(userPrefs.exportBackup())

    suspend fun importJson(json: String): Boolean {
        val backup = runCatching { gson.fromJson(json, AppBackup::class.java) }.getOrNull() ?: return false
        if (backup.version != 1) return false
        userPrefs.importBackup(backup)
        return true
    }
}
