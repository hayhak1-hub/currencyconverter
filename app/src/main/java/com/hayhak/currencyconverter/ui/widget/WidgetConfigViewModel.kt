package com.hayhak.currencyconverter.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WidgetConfigViewModel @Inject constructor(
    private val userPrefs: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val base: StateFlow<String> = userPrefs.widgetBase.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), "USD"
    )
    val codes: StateFlow<List<String>> = userPrefs.widgetCodes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf("USD", "EUR", "GBP", "CHF")
    )
    val favorites: StateFlow<Set<String>> = userPrefs.favorites.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet()
    )

    fun save(base: String, codes: List<String>) {
        viewModelScope.launch {
            userPrefs.saveWidgetConfig(base, codes.filter { it != base }.take(4))
            CurrencyWidget().updateAll(context)
        }
    }
}
