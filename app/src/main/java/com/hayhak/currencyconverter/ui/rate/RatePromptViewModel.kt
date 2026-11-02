package com.hayhak.currencyconverter.ui.rate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayhak.currencyconverter.domain.model.RatePromptPolicy
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RatePromptViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private val _showPrompt = MutableStateFlow(false)
    val showPrompt: StateFlow<Boolean> = _showPrompt.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.ensureRatePromptFirstSeen()
            val now = System.currentTimeMillis()
            val state = preferences.ratePromptState()
            if (RatePromptPolicy.shouldShow(now, state)) {
                preferences.markRatePromptShown(now)
                _showPrompt.value = true
            }
        }
    }

    fun onLater() {
        _showPrompt.value = false
    }

    fun onOk() {
        _showPrompt.value = false
        viewModelScope.launch {
            preferences.markRatePromptAccepted()
        }
    }
}
