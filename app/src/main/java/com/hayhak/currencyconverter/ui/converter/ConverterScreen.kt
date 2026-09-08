package com.hayhak.currencyconverter.ui.converter

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.hayhak.currencyconverter.util.ShareHelper
import com.hayhak.currencyconverter.util.currencyName
import com.hayhak.currencyconverter.util.formatRelativeTime
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    viewModel: ConverterViewModel = hiltViewModel(),
    isWide: Boolean = false
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showNumberPad by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val mainColumn: @Composable ColumnScope.() -> Unit = {
        RateStatusBanner(
            lastUpdated = state.lastUpdated,
            isLoading = state.isLoading,
            hasRates = state.rates.isNotEmpty(),
            onRetry = viewModel::refreshRates
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
        if (state.isLoading && state.result == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            ResultDisplay(state)
        }
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
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            mainColumn()
            HorizontalDivider()
            Text(stringResource(R.string.converter_other_units), style = MaterialTheme.typography.titleSmall)
            BatchList(state, Modifier.weight(1f))
        }
    }

    if (showNumberPad) {
        ModalBottomSheet(
            onDismissRequest = {
                viewModel.finishAmountEntry()
                showNumberPad = false
            },
            sheetState = sheetState
        ) {
            NumberPalette(
                amount = state.amount,
                onKey = viewModel::onNumpadClick,
                onDone = {
                    viewModel.finishAmountEntry()
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
        items(state.batchResults.entries.toList(), key = { it.key }) { (code, value) ->
            val info = SUPPORTED_CURRENCIES.find { it.code == code }
            ListItem(
                headlineContent = { Text(code) },
                supportingContent = { Text(info?.let { currencyName(it.code) } ?: "") },
                leadingContent = { Text(info?.flag ?: "", fontSize = 20.sp) },
                trailingContent = { Text(formatVal(value), fontWeight = FontWeight.Bold) }
            )
        }
    }
}

@Composable
private fun ResultDisplay(state: ConverterUiState) {
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
            val res = state.result?.let { formatVal(it) } ?: "—"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(res, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                if (state.result != null) {
                    IconButton(onClick = { clip.setText(AnnotatedString(res)) }) {
                        Icon(Icons.Default.ContentCopy, stringResource(R.string.cd_copy), modifier = Modifier.size(16.dp))
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
                        Icon(Icons.Default.Share, stringResource(R.string.cd_share_conversion), modifier = Modifier.size(16.dp))
                    }
                }
            }
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

private fun formatVal(v: Double) = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}.format(v)
