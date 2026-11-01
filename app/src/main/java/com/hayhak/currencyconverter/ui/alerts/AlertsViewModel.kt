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

    fun add(base: String, target: String, threshold: Double, isAbove: Boolean, repeating: Boolean) {
        viewModelScope.launch {
            userPrefs.addAlarm(
                RateAlarm(
                    baseCode = base,
                    targetCode = target,
                    threshold = threshold,
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
