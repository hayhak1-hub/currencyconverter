package com.hayhak.currencyconverter.ui.converter

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES
import com.hayhak.currencyconverter.ui.components.CurrencySelector
import com.hayhak.currencyconverter.domain.usecase.ConvertCurrencyUseCase
import com.hayhak.currencyconverter.domain.model.BASE_CURRENCY
import com.hayhak.currencyconverter.util.ShareHelper
import com.hayhak.currencyconverter.util.buySell
import com.hayhak.currencyconverter.util.currencyName
import com.hayhak.currencyconverter.util.formatMoney
import com.hayhak.currencyconverter.util.formatRelativeTime
import com.hayhak.currencyconverter.util.profitPercent
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    viewModel: ConverterViewModel = hiltViewModel(),
    isWide: Boolean = false,
    onAlertPair: (String, String) -> Unit = { _, _ -> }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showNumberPad by remember { mutableStateOf(false) }
    var showTools by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val mainColumn: @Composable ColumnScope.() -> Unit = {
        RateStatusBanner(
            lastUpdated = state.lastUpdated,
            isLoading = state.isLoading,
            hasRates = state.rates.isNotEmpty(),
            onRetry = viewModel::retryRates
        )
        AmountCard(
            amount = state.amount,
            onOpenPad = {
                viewModel.startAmountEntry()
                showNumberPad = true
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurrencySelector(
                label = stringResource(R.string.converter_from),
                currency = state.fromCurrency,
                onSelect = viewModel::onFromCurrencyChange,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = viewModel::swapCurrencies) {
                Icon(Icons.Default.SwapHoriz, stringResource(R.string.cd_swap))
            }
            CurrencySelector(
                label = stringResource(R.string.converter_to),
                currency = state.toCurrency,
                onSelect = viewModel::onToCurrencyChange,
                modifier = Modifier.weight(1f)
            )
        }
        HistoricalDateRow(
            label = state.historicalLabel,
            loading = state.historicalLoading,
            error = state.historicalError,
            onPick = { showDatePicker = true },
            onClear = { viewModel.setHistoricalDate(null) }
        )
        SavedPairsRow(
            pairs = state.savedPairs,
            current = state.fromCurrency.code to state.toCurrency.code,
            onApply = viewModel::applyPair,
            onSave = viewModel::saveCurrentPair,
            onRemove = viewModel::removePair,
            onAlert = onAlertPair
        )
        if (state.isLoading && state.result == null && state.historicalDate == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            ResultDisplay(
                state = state,
                onEditResult = {
                    viewModel.beginTargetEntry()
                    showNumberPad = true
                }
            )
            if (state.sparkline.size >= 2) {
                Text(stringResource(R.string.converter_sparkline), style = MaterialTheme.typography.labelMedium)
                Sparkline(
                    points = state.sparkline,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                )
            }
        }
        TextButton(onClick = { showTools = true }) { Text(stringResource(R.string.tools_title)) }
    }

    if (isWide) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = mainColumn
            )
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text(stringResource(R.string.converter_other_units), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                BatchList(state, Modifier.fillMaxSize())
            }
        }
    } else {
        // One scrolling list so the amount, selectors and result move up with the batch results.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), content = mainColumn)
            }
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider()
                    Text(
                        stringResource(R.string.converter_other_units),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
            batchItems(state)
        }
    }

    if (showTools) {
        ConverterToolsSheet(state, { showTools = false }, viewModel::reuseConversion, viewModel::onToCurrencyChange)
    }
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.historicalDate ?: System.currentTimeMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let(viewModel::setHistoricalDate)
                    showDatePicker = false
                }) { Text(stringResource(R.string.converter_done)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) { DatePicker(state = pickerState) }
    }
    if (showNumberPad) {
        ModalBottomSheet(
            onDismissRequest = {
                if (state.targetDraft != null) viewModel.cancelTargetEntry() else viewModel.finishAmountEntry()
                showNumberPad = false
            },
            sheetState = sheetState
        ) {
            val editingResult = state.targetDraft != null
            NumberPalette(
                amount = if (editingResult) state.targetDraft.orEmpty() else state.amount,
                onKey = if (editingResult) viewModel::onTargetKey else viewModel::onNumpadClick,
                onDone = {
                    if (editingResult) viewModel.finishTargetEntry() else viewModel.finishAmountEntry()
                    showNumberPad = false
                }
            )
        }
    }
}

@Composable
private fun AmountCard(amount: String, onOpenPad: () -> Unit) {
    OutlinedCard(onClick = onOpenPad, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.converter_amount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                AnimatedContent(
                    targetState = amount.ifEmpty { "1" },
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "amountField"
                ) { value ->
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.Default.Dialpad,
                contentDescription = stringResource(R.string.cd_open_numpad),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RateStatusBanner(
    lastUpdated: Long?,
    isLoading: Boolean,
    hasRates: Boolean,
    onRetry: () -> Unit
) {
    if (hasRates || (isLoading && lastUpdated == null)) return
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.converter_offline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) { Text(stringResource(R.string.converter_retry)) }
        }
    }
}

@Composable
private fun BatchList(state: ConverterUiState, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        batchItems(state)
    }
}

private fun LazyListScope.batchItems(state: ConverterUiState) {
    items(state.batchResults.entries.toList(), key = { it.key }) { (code, value) ->
        val info = SUPPORTED_CURRENCIES.find { it.code == code }
        ListItem(
            headlineContent = { Text(code) },
            supportingContent = { Text(info?.let { currencyName(it.code) } ?: "") },
            leadingContent = { Text(info?.flag ?: "", fontSize = 20.sp) },
            trailingContent = { Text(formatMoney(value, code), fontWeight = FontWeight.Bold) },
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}

@Composable
private fun ResultDisplay(state: ConverterUiState, onEditResult: () -> Unit) {
    val clip = LocalClipboardManager.current
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${state.fromCurrency.flag} ${state.fromCurrency.code} -> ${state.toCurrency.flag} ${state.toCurrency.code}",
                style = MaterialTheme.typography.labelMedium
            )
            val res = state.result?.let { formatMoney(it, state.toCurrency.code) } ?: "—"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(res, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                if (state.result != null) {
                    IconButton(onClick = {
                        clip.setText(AnnotatedString(res))
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.converter_copied),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, stringResource(R.string.cd_copy), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = {
                        val amount = state.amount.ifEmpty { "1" }
                        ShareHelper.shareText(
                            context,
                            context.getString(R.string.settings_share),
                            context.getString(
                                R.string.converter_share_text,
                                amount,
                                state.fromCurrency.code,
                                res,
                                state.toCurrency.code
                            )
                        )
                    }) {
                        Icon(Icons.Default.Share, stringResource(R.string.cd_share_conversion), modifier = Modifier.size(18.dp))
                    }
                }
            }
            state.inverseRate?.let { inv ->
                if (inv > 0) {
                    Text(
                        stringResource(
                            R.string.converter_inverse_rate,
                            state.toCurrency.code,
                            formatMoney(inv, state.fromCurrency.code),
                            state.fromCurrency.code
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            if (state.result != null) {
                TextButton(onClick = onEditResult) { Text(stringResource(R.string.converter_edit_result)) }
            }
            val todayValue = state.amount.replace(',', '.').toDoubleOrNull()?.let { amount ->
                if (state.historicalDate == null) null
                else ConvertCurrencyUseCase()(amount, state.fromCurrency.code, state.toCurrency.code, state.rates, BASE_CURRENCY)
            }
            if (state.result != null && todayValue != null) {
                val change = profitPercent(state.result, todayValue)
                Text(
                    stringResource(R.string.converter_pnl_today, formatMoney(todayValue, state.toCurrency.code)),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (change != null) {
                    Text(
                        stringResource(R.string.converter_pnl_change, "%+.2f%%".format(change)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (change >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                }
            }
            val spread = state.result?.let { buySell(it, state.spreadPercent) }
            if (spread != null) {
                Text(
                    stringResource(R.string.converter_buy, formatMoney(spread.first, state.toCurrency.code)),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    stringResource(R.string.converter_sell, formatMoney(spread.second, state.toCurrency.code)),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            val historical = state.historicalLabel?.let(::formatIsoDate)
            if (historical != null) {
                Text(
                    stringResource(R.string.converter_as_of, historical),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            } else {
                state.lastUpdated?.let { ts ->
                    Text(
                        stringResource(R.string.converter_updated, formatRelativeTime(context, ts)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoricalDateRow(
    label: String?,
    loading: Boolean,
    error: Boolean,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPick) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(label?.let(::formatIsoDate) ?: stringResource(R.string.converter_pick_date))
            }
            if (label != null || error) {
                TextButton(onClick = onClear) { Text(stringResource(R.string.converter_latest)) }
            }
            if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        }
        if (error) {
            Text(
                stringResource(R.string.converter_history_missing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavedPairsRow(
    pairs: List<Pair<String, String>>,
    current: Pair<String, String>,
    onApply: (String, String) -> Unit,
    onSave: () -> Unit,
    onRemove: (String, String) -> Unit,
    onAlert: (String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.pairs_saved),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
            if (current !in pairs && current.first != current.second) {
                TextButton(onClick = onSave) { Text(stringResource(R.string.pairs_save)) }
            }
        }
        if (pairs.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                pairs.forEach { (from, to) ->
                    FilterChip(
                        selected = current == from to to,
                        onClick = { onApply(from, to) },
                        label = { Text("$from → $to") }
                    )
                    IconButton(onClick = { onAlert(from, to) }) {
                        Icon(Icons.Default.Notifications, stringResource(R.string.pairs_alert))
                    }
                    IconButton(onClick = { onRemove(from, to) }) {
                        Icon(Icons.Default.Close, stringResource(R.string.pairs_remove))
                    }
                }
            }
        }
    }
}

@Composable
private fun Sparkline(points: List<Double>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val min = points.min()
        val max = points.max()
        val span = (max - min).takeIf { it > 0.0 } ?: 1.0
        val step = if (points.size == 1) 0f else size.width / (points.size - 1)
        val offsets = points.mapIndexed { index, value ->
            Offset(
                x = step * index,
                y = size.height - ((value - min) / span * size.height).toFloat()
            )
        }
        for (index in 0 until offsets.lastIndex) {
            drawLine(
                color = color,
                start = offsets[index],
                end = offsets[index + 1],
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun formatIsoDate(iso: String): String {
    val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return iso
    val millis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date.from(Instant.ofEpochMilli(millis)))
}
