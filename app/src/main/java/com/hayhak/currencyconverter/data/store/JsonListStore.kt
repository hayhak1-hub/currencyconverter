package com.hayhak.currencyconverter.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.gson.Gson
import java.lang.reflect.Type
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A list of records kept as JSON in its own DataStore file, so each feature stays independent of
 * the shared preferences file. [type] must come from a concrete `object : TypeToken<List<T>>() {}`
 * (the keep rule for TypeToken subclasses is what makes that survive R8).
 */
class JsonListStore<T>(
    context: Context,
    fileName: String,
    private val type: Type,
    private val gson: Gson = Gson()
) {
    private val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.applicationContext.preferencesDataStoreFile(fileName) }
    )
    private val key = stringPreferencesKey("items")

    val items: Flow<List<T>> = store.data.map { prefs -> decode(prefs[key]) }

    suspend fun update(transform: (List<T>) -> List<T>) {
        store.edit { prefs -> prefs[key] = gson.toJson(transform(decode(prefs[key]))) }
    }

    private fun decode(json: String?): List<T> {
        if (json.isNullOrBlank()) return emptyList()
        // A corrupted or older file must not crash the screen that reads it.
        return runCatching { gson.fromJson<List<T>>(json, type) }.getOrNull().orEmpty().filterNotNull()
    }
}
