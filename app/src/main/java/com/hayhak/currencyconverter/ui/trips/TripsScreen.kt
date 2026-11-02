package com.hayhak.currencyconverter.ui.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.data.store.model.Trip
import com.hayhak.currencyconverter.domain.model.CurrencyPairDefaults
import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES
import com.hayhak.currencyconverter.domain.model.getFlagEmoji
import com.hayhak.currencyconverter.ui.components.CurrencySelector
import com.hayhak.currencyconverter.util.formatMoney

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripsScreen(viewModel: TripsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var addingExpense by remember { mutableStateOf(false) }

    if (creating) {
        val defaults by viewModel.defaults.collectAsStateWithLifecycle()
        NewTripDialog(
            defaults = defaults,
            onDismiss = { creating = false },
            onSave = { name, local, home, budget ->
                viewModel.createTrip(name, local, home, budget)
                creating = false
            }
        )
    }
    val trip = state.selected
    if (addingExpense && trip != null) {
        ExpenseDialog(
            localCode = trip.localCode,
            onDismiss = { addingExpense = false },
            onSave = { amount, label ->
                viewModel.addExpense(trip.id, amount, label)
                addingExpense = false
            }
        )
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (trip == null) {
                item {
                    Text(
                        stringResource(R.string.trips_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 48.dp, horizontal = 8.dp)
                    )
                }
            } else {
                if (state.trips.size > 1) {
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.trips.forEach { t ->
                                FilterChip(
                                    selected = t.id == trip.id,
                                    onClick = { viewModel.select(t.id) },
                                    label = { Text(t.name) }
                                )
                            }
                        }
                    }
                }
                item { TripSummary(trip, state.status, onAddExpense = { addingExpense = true }, onDelete = { viewModel.deleteTrip(trip.id) }) }
                items(trip.expenses.reversed(), key = { it.id }) { expense ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                formatMoney(expense.amount, trip.localCode) + " " + trip.localCode,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            val date = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
                                .format(java.util.Date(expense.at))
                            Text(
                                listOf(expense.label, date).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = { viewModel.removeExpense(trip.id, expense.id) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.trips_delete_expense))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.trips_new))
        }
    }
}

@Composable
private fun TripSummary(trip: Trip, status: TripStatus?, onAddExpense: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${getFlagEmoji(trip.localCode)} ${trip.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.trips_delete))
                }
            }
            if (status != null) {
                val left = if (status.overBudget) {
                    stringResource(R.string.trips_over, formatMoney(-status.remainingHome, trip.homeCode) + " " + trip.homeCode)
                } else {
                    stringResource(R.string.trips_left, formatMoney(status.remainingHome, trip.homeCode) + " " + trip.homeCode)
                }
                Text(
                    left,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (status.overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                LinearProgressIndicator(
                    progress = { status.usedFraction.coerceIn(0.0, 1.0).toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (status.overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(
                        R.string.trips_spent_of,
                        formatMoney(status.spentHome, trip.homeCode),
                        formatMoney(trip.budget, trip.homeCode) + " " + trip.homeCode
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                if (status.unconverted > 0) {
                    Text(
                        stringResource(R.string.trips_no_rate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Button(onClick = onAddExpense) { Text(stringResource(R.string.trips_add_expense)) }
        }
    }
}

@Composable
private fun NewTripDialog(
    defaults: CurrencyPairDefaults,
    onDismiss: () -> Unit,
    onSave: (name: String, localCode: String, homeCode: String, budget: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    // Spending abroad starts as one of the user's favorites, the budget in their own base currency.
    var local by remember { mutableStateOf(currencyOrUsd(defaults.other)) }
    var home by remember { mutableStateOf(currencyOrUsd(defaults.home)) }
    var budgetInput by remember { mutableStateOf("") }
    val budget = budgetInput.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trips_new)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.trips_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CurrencySelector(
                    label = stringResource(R.string.trips_local_currency),
                    currency = local,
                    onSelect = { local = it },
                    modifier = Modifier.fillMaxWidth()
                )
                CurrencySelector(
                    label = stringResource(R.string.trips_home_currency),
                    currency = home,
                    onSelect = { home = it },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text(stringResource(R.string.trips_budget, home.code)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { budget?.let { onSave(name, local.code, home.code, it) } },
                enabled = budget != null && local.code != home.code
            ) { Text(stringResource(R.string.portfolio_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun ExpenseDialog(localCode: String, onDismiss: () -> Unit, onSave: (amount: Double, label: String) -> Unit) {
    var amountInput by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    val amount = amountInput.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trips_add_expense)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text(stringResource(R.string.trips_expense_amount, localCode)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.trips_expense_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { amount?.let { onSave(it, label) } }, enabled = amount != null) {
                Text(stringResource(R.string.portfolio_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

private fun currencyOrUsd(code: String) =
    SUPPORTED_CURRENCIES.firstOrNull { it.code == code } ?: SUPPORTED_CURRENCIES.first { it.code == "USD" }
