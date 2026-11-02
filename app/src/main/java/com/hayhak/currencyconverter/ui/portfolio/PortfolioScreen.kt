package com.hayhak.currencyconverter.ui.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.CurrencyPairDefaults
import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES
import com.hayhak.currencyconverter.domain.model.getFlagEmoji
import com.hayhak.currencyconverter.ui.components.CurrencySelector
import com.hayhak.currencyconverter.ui.theme.TrendDown
import com.hayhak.currencyconverter.ui.theme.TrendDownDark
import com.hayhak.currencyconverter.ui.theme.TrendUp
import com.hayhak.currencyconverter.ui.theme.TrendUpDark
import com.hayhak.currencyconverter.util.formatMoney

@Composable
fun PortfolioScreen(viewModel: PortfolioViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }

    if (adding) {
        val defaults by viewModel.defaults.collectAsStateWithLifecycle()
        AddHoldingDialog(
            defaults = defaults,
            onDismiss = { adding = false },
            onSave = { code, amount, price, costCode ->
                viewModel.add(code, amount, price, costCode)
                adding = false
            }
        )
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.isEmpty) {
                item {
                    Text(
                        stringResource(R.string.portfolio_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 48.dp, horizontal = 8.dp)
                    )
                }
            }
            state.summary?.let { summary ->
                item { SummaryCard(summary = summary, onPickCurrency = viewModel::setDisplayCode) }
                items(summary.rows, key = { it.entry.id }) { row ->
                    HoldingCard(row, onDelete = { viewModel.remove(row.entry.id) })
                }
                if (summary.unpriced > 0) {
                    item {
                        Text(
                            stringResource(R.string.portfolio_unpriced, summary.unpriced),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.portfolio_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
        FloatingActionButton(
            onClick = { adding = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.portfolio_add))
        }
    }
}

@Composable
private fun pnlColor(value: Double): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when {
        value > 0 -> if (dark) TrendUpDark else TrendUp
        value < 0 -> if (dark) TrendDownDark else TrendDown
        else -> MaterialTheme.colorScheme.onSurface
    }
}

private fun signed(value: Double, code: String): String =
    (if (value > 0) "+" else "") + formatMoney(value, code) + " " + code

private fun percentSuffix(percent: Double?): String =
    percent?.let { "  (%+.2f%%)".format(it) } ?: ""

@Composable
private fun SummaryCard(summary: PortfolioSummary, onPickCurrency: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CurrencySelector(
                label = stringResource(R.string.metals_show_in),
                currency = SUPPORTED_CURRENCIES.firstOrNull { it.code == summary.displayCode }
                    ?: SUPPORTED_CURRENCIES.first { it.code == "USD" },
                onSelect = { onPickCurrency(it.code) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(stringResource(R.string.portfolio_total_value), style = MaterialTheme.typography.labelMedium)
            Text(
                formatMoney(summary.totalValue, summary.displayCode) + " " + summary.displayCode,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.portfolio_cost) + ": " +
                    formatMoney(summary.totalCost, summary.displayCode) + " " + summary.displayCode,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            Text(
                signed(summary.pnl, summary.displayCode) + percentSuffix(summary.pnlPercent),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = pnlColor(summary.pnl)
            )
        }
    }
}

@Composable
private fun HoldingCard(row: HoldingRow, onDelete: () -> Unit) {
    val entry = row.entry
    val date = remember(entry.boughtAt) {
        java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(entry.boughtAt))
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${getFlagEmoji(entry.code)} ${formatMoney(entry.amount, entry.code)} ${entry.code}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "@ ${formatMoney(entry.costPerUnit, entry.costCode)} ${entry.costCode} · $date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    signed(row.pnl, entry.costCode) + percentSuffix(row.pnlPercent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = pnlColor(row.pnl)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.portfolio_delete))
            }
        }
    }
}

@Composable
private fun AddHoldingDialog(
    defaults: CurrencyPairDefaults,
    onDismiss: () -> Unit,
    onSave: (code: String, amount: Double, costPerUnit: Double, costCode: String) -> Unit
) {
    // What was bought starts as one of the user's favorites, paid for in their own base currency.
    var currency by remember { mutableStateOf(currencyOrUsd(defaults.other)) }
    var costCurrency by remember { mutableStateOf(currencyOrUsd(defaults.home)) }
    var amountInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    val amount = amountInput.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 }
    val price = priceInput.replace(",", ".").toDoubleOrNull()?.takeIf { it >= 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.portfolio_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CurrencySelector(
                    label = stringResource(R.string.portfolio_currency),
                    currency = currency,
                    onSelect = { currency = it },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text(stringResource(R.string.converter_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CurrencySelector(
                    label = stringResource(R.string.portfolio_paid_in),
                    currency = costCurrency,
                    onSelect = { costCurrency = it },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    label = { Text(stringResource(R.string.portfolio_price_per_unit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amount != null && price != null) onSave(currency.code, amount, price, costCurrency.code)
                },
                enabled = amount != null && price != null && currency.code != costCurrency.code
            ) { Text(stringResource(R.string.portfolio_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

private fun currencyOrUsd(code: String) =
    SUPPORTED_CURRENCIES.firstOrNull { it.code == code } ?: SUPPORTED_CURRENCIES.first { it.code == "USD" }
