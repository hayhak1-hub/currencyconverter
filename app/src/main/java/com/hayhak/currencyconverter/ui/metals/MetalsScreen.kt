package com.hayhak.currencyconverter.ui.metals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.hayhak.currencyconverter.domain.model.GRAMS_PER_TROY_OUNCE
import java.text.DateFormat
import java.util.Date
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.getFlagEmoji
import com.hayhak.currencyconverter.util.currencyName
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetalsScreen(viewModel: MetalsViewModel = hiltViewModel(), isWide: Boolean = false) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var amount by rememberSaveable { mutableStateOf("1") }
    var grams by rememberSaveable { mutableStateOf(false) }
    val quantity = amount.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            state.isLoading && state.metals.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.metals.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            state.error ?: stringResource(R.string.metals_error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        TextButton(onClick = viewModel::refresh) {
                            Text(stringResource(R.string.converter_retry))
                        }
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isWide) 2 else 1),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(amount, { amount = it }, label = { Text(stringResource(R.string.metal_amount)) },
                                singleLine = true, isError = quantity == null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                FilterChip(selected = !grams, onClick = { grams = false }, label = { Text(stringResource(R.string.metal_ounce)) })
                                FilterChip(selected = grams, onClick = { grams = true }, label = { Text(stringResource(R.string.metal_gram)) })
                            }
                            Text(stringResource(R.string.metal_estimate), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "${getFlagEmoji(state.quote)} ${state.quote} · $amount " +
                                stringResource(if (grams) R.string.metal_gram else R.string.metal_ounce),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (state.quote != "USD" && state.fxTimestamp != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text("USD/${state.quote} · " + stringResource(R.string.converter_updated,
                                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(state.fxTimestamp!!))),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    items(state.metals, key = { it.code }) { metal ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(metal.flag, fontSize = 28.sp, modifier = Modifier.padding(end = 14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        metal.code,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        currencyName(metal.code),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                    )
                                }
                                Text(
                                    (quantity?.let { q -> (metal.rate * q / (if (grams) GRAMS_PER_TROY_OUNCE else 1.0)).takeIf { it.isFinite() } }?.let(::formatMetal) ?: "—") + " ${state.quote}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(stringResource(R.string.metal_retrieved, metal.source,
                                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(metal.retrievedAt))),
                                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
                            if (metal.cached && !state.isRefreshing) Text(stringResource(R.string.metal_cache), modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

private fun formatMetal(v: Double): String = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}.format(v)
