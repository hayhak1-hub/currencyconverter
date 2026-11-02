package com.hayhak.currencyconverter.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES
import com.hayhak.currencyconverter.ui.components.CurrencySelector
import com.hayhak.currencyconverter.ui.theme.CurrencyConverterTheme
import kotlinx.coroutines.launch

class WidgetConfigureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val editing = intent.getBooleanExtra(EXTRA_EDITING, false)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        if (!editing) {
            setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        }
        val saved = WidgetInstanceStore.load(this, appWidgetId)
        setContent {
            CurrencyConverterTheme {
                ConfigureScreen(
                    initialBase = saved?.first ?: "USD",
                    initialCodes = saved?.second ?: listOf("EUR", "GBP", "JPY"),
                    onSave = { base, codes ->
                        WidgetInstanceStore.save(this, appWidgetId, base, codes)
                        lifecycleScope.launch {
                            if (editing) {
                                runCatching {
                                    val glanceId = GlanceAppWidgetManager(this@WidgetConfigureActivity)
                                        .getGlanceIdBy(appWidgetId)
                                    CurrencyWidget().update(this@WidgetConfigureActivity, glanceId)
                                }
                            }
                            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                            finish()
                        }
                    },
                    onCancel = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_EDITING = "editing"
    }
}

@Composable
private fun ConfigureScreen(
    initialBase: String,
    initialCodes: List<String>,
    onSave: (String, List<String>) -> Unit,
    onCancel: () -> Unit
) {
    var base by remember {
        mutableStateOf(SUPPORTED_CURRENCIES.find { it.code == initialBase } ?: SUPPORTED_CURRENCIES.first { it.code == "USD" })
    }
    var codes by remember {
        mutableStateOf(
            initialCodes.mapNotNull { code -> SUPPORTED_CURRENCIES.find { it.code == code } }
                .ifEmpty { listOf("EUR", "GBP", "JPY").mapNotNull { code -> SUPPORTED_CURRENCIES.find { it.code == code } } }
        )
    }
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.widget_configure_title), style = MaterialTheme.typography.titleLarge)
            CurrencySelector(
                label = stringResource(R.string.dashboard_base_currency),
                currency = base,
                onSelect = { base = it },
                modifier = Modifier.fillMaxWidth()
            )
            Text(stringResource(R.string.widget_configure_quotes), style = MaterialTheme.typography.titleMedium)
            codes.forEachIndexed { index, currency ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencySelector(
                        label = stringResource(R.string.converter_to),
                        currency = currency,
                        onSelect = { picked -> codes = codes.toMutableList().also { it[index] = picked } },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { codes = codes.filterIndexed { i, _ -> i != index } }) {
                        Text("×")
                    }
                }
            }
            if (codes.size < 4) {
                TextButton(onClick = {
                    val next = SUPPORTED_CURRENCIES.firstOrNull { it.code != base.code && it !in codes }
                    if (next != null) codes = codes + next
                }) { Text(stringResource(R.string.widget_configure_add)) }
            }
            Button(
                onClick = { onSave(base.code, codes.map { it.code }.filter { it != base.code }.distinct().take(4)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.widget_configure_save)) }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) }
        }
    }
}
