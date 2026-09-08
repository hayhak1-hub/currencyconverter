package com.hayhak.currencyconverter.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    val alarms: StateFlow<List<RateAlarm>> = userPrefs.alarms.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

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
