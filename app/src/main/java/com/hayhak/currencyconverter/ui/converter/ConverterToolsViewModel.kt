package com.hayhak.currencyconverter.ui.converter

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hayhak.currencyconverter.domain.model.SavedConversion
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ConverterToolsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {
    private val prefs = context.getSharedPreferences("conversion_tools", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val mutableHistory = MutableStateFlow<List<SavedConversion>>(emptyList())
    val history = mutableHistory.asStateFlow()
    private val mutableReady = MutableStateFlow(false)
    val ready = mutableReady.asStateFlow()
    private val mutableError = MutableStateFlow(false)
    val error = mutableError.asStateFlow()

    init {
        viewModelScope.launch {
            mutableHistory.value = withContext(Dispatchers.IO) {
                runCatching {
                    gson.fromJson<List<SavedConversion>>(prefs.getString("history", "[]"),
                        object : TypeToken<List<SavedConversion>>() {}.type) ?: emptyList()
                }.getOrDefault(emptyList())
            }
            mutableReady.value = true
        }
    }

    fun save(state: ConverterUiState, gross: Double, net: Double, percent: String, fixed: String) {
        if (!mutableReady.value || !gross.isFinite() || !net.isFinite()) return
        val entry = SavedConversion(UUID.randomUUID().toString(), state.amount, state.fromCurrency.code,
            state.toCurrency.code, gross, net, percent, fixed, System.currentTimeMillis(), state.lastUpdated ?: 0)
        persist((listOf(entry) + mutableHistory.value).take(50))
    }

    fun delete(id: String) = persist(mutableHistory.value.filterNot { it.id == id })

    private fun persist(entries: List<SavedConversion>) {
        if (!mutableReady.value) return
        mutableReady.value = false
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                prefs.edit().putString("history", gson.toJson(entries)).commit()
            }
            if (saved) mutableHistory.value = entries
            mutableError.value = !saved
            mutableReady.value = true
        }
    }

    fun pinCurrency(code: String) {
        viewModelScope.launch {
            if (code !in userPrefs.favorites.first()) userPrefs.toggleFavorite(code)
        }
    }
}
