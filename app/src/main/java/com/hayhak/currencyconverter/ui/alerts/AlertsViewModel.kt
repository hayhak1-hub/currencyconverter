package com.hayhak.currencyconverter.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayhak.currencyconverter.data.store.AlarmHistoryStore
import com.hayhak.currencyconverter.data.store.model.AlarmEvent
import com.hayhak.currencyconverter.domain.model.RateAlarm
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val userPrefs: UserPreferencesRepository,
    private val history: AlarmHistoryStore
) : ViewModel() {

    val alarms: StateFlow<List<RateAlarm>> = userPrefs.alarms.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val events: StateFlow<List<AlarmEvent>> = history.events.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun clearHistory() {
        viewModelScope.launch { history.clear() }
    }

    fun add(
        base: String,
        target: String,
        value: Double,
        isAbove: Boolean,
        repeating: Boolean,
        percent: Boolean
    ) {
        viewModelScope.launch {
            userPrefs.addAlarm(
                RateAlarm(
                    baseCode = base,
                    targetCode = target,
                    threshold = if (percent) 0.0 else value,
                    percent = if (percent) value else 0.0,
                    kind = if (percent) RateAlarm.KIND_PERCENT else RateAlarm.KIND_THRESHOLD,
                    isAbove = isAbove,
                    repeating = repeating
                )
            )
        }
    }

    fun toggle(alarm: RateAlarm) {
        viewModelScope.launch {
            userPrefs.updateAlarm(alarm.copy(isEnabled = !alarm.isEnabled))
        }
    }

    fun update(alarm: RateAlarm) {
        viewModelScope.launch { userPrefs.updateAlarm(alarm) }
    }

    fun remove(id: String) {
        viewModelScope.launch { userPrefs.removeAlarm(id) }
    }
}
