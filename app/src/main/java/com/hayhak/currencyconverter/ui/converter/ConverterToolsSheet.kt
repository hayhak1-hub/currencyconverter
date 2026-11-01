package com.hayhak.currencyconverter.ui.converter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.*
import com.hayhak.currencyconverter.domain.usecase.ConvertCurrencyUseCase
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterToolsSheet(
    state: ConverterUiState,
    onDismiss: () -> Unit,
    onReuse: (SavedConversion) -> Unit,
    onDestination: (CurrencyInfo) -> Unit,
    viewModel: ConverterToolsViewModel = hiltViewModel()
) {
    var percent by rememberSaveable { mutableStateOf("") }
    var fixed by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val history by viewModel.history.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val gross = state.amount.replace(',', '.').toDoubleOrNull()?.let {
        ConvertCurrencyUseCase()(it, state.fromCurrency.code, state.toCurrency.code, state.rates)
    }
    val net = gross?.let { netAfterFees(it, percent, fixed) }
    val format = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 4 }
    val locale = Locale.getDefault()
    val countries = remember(locale) {
        Locale.getISOCountries().mapNotNull { country ->
            val location = Locale.Builder().setRegion(country).build()
            val code = runCatching { Currency.getInstance(location).currencyCode }.getOrNull()
            val info = SUPPORTED_CURRENCIES.find { it.code == code } ?: return@mapNotNull null
            location.getDisplayCountry(locale) to info
        }.sortedBy { it.first }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        TabRow(selectedTabIndex = tab) {
            listOf(R.string.tools_fees, R.string.tools_history, R.string.tools_travel).forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(stringResource(title)) })
            }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (tab) {
                0 -> {
                    item { Text(stringResource(R.string.tools_fee_explanation, state.toCurrency.code)) }
                    item {
                        OutlinedTextField(percent, { percent = it }, label = { Text(stringResource(R.string.tools_percent)) },
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(fixed, { fixed = it }, label = { Text(stringResource(R.string.tools_fixed, state.toCurrency.code)) },
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        Text(net?.let { stringResource(R.string.tools_net, format.format(it), state.toCurrency.code) }
                            ?: stringResource(R.string.tools_invalid), style = MaterialTheme.typography.titleMedium)
                    }
                    item {
                        Button(enabled = ready && net != null, onClick = {
                            if (gross != null && net != null) {
                                viewModel.save(state, gross, net, percent, fixed)
                                tab = 1
                            }
                        }) { Text(stringResource(R.string.tools_save)) }
                    }
                }
                1 -> {
                    item { Text(stringResource(R.string.tools_history_note)) }
                    if (error) item { Text(stringResource(R.string.tools_save_error), color = MaterialTheme.colorScheme.error) }
                    if (history.isEmpty()) item { Text(stringResource(R.string.tools_empty)) }
                    items(history, key = { it.id }) { entry ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("${getFlagEmoji(entry.from)} ${entry.amount} ${entry.from} = ${format.format(entry.net)} ${entry.to} ${getFlagEmoji(entry.to)}")
                                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.savedAt)))
                                Text(stringResource(R.string.tools_saved_fees, entry.percent.ifBlank { "0" }, entry.fixed.ifBlank { "0" }, entry.to))
                                Row {
                                    TextButton(onClick = { onReuse(entry); onDismiss() }) { Text(stringResource(R.string.tools_reuse)) }
                                    TextButton(enabled = ready, onClick = { viewModel.delete(entry.id) }) { Text(stringResource(R.string.tools_delete)) }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    item { Text(stringResource(R.string.tools_travel_note)) }
                    item { OutlinedTextField(search, { search = it }, label = { Text(stringResource(R.string.tools_country)) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                    items(countries.filter { it.first.contains(search, true) || it.second.code.contains(search, true) }) { (name, currency) ->
                        TextButton(onClick = { viewModel.pinCurrency(currency.code); onDestination(currency); onDismiss() }) {
                            Text("${currency.flag} $name · ${currency.code}")
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
