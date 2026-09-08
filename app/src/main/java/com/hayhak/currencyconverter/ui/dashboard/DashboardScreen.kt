package com.hayhak.currencyconverter.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES
import com.hayhak.currencyconverter.ui.theme.TrendDown
import com.hayhak.currencyconverter.ui.theme.TrendDownDark
import com.hayhak.currencyconverter.ui.theme.TrendUp
import com.hayhak.currencyconverter.ui.theme.TrendUpDark
import com.hayhak.currencyconverter.util.currencyName
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val localeKey = LocalConfiguration.current.locales.toLanguageTags()

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (state.showAlarmDialog != null) {
        AlarmDialog(
            row = state.showAlarmDialog!!,
            onDismiss = { viewModel.showAlarmDialog(null) },
            onConfirm = { threshold, isAbove -> viewModel.addAlarm(threshold, isAbove) }
        )
    }

    val query = searchQuery.trim()

    val allRateRows = remember(state.allRates, state.baseCurrency, localeKey) {
        SUPPORTED_CURRENCIES
            .filter { it.code != state.baseCurrency }
            .mapNotNull { info ->
                val rate = state.allRates[info.code] ?: return@mapNotNull null
                CurrencyRow(
                    code = info.code,
                    name = context.currencyName(info.code),
                    symbol = info.symbol,
                    flag = info.flag,
                    rateToTry = rate
                )
            }
    }

    fun rowMatchesQuery(row: CurrencyRow): Boolean {
        if (query.isBlank()) return true
        val info = SUPPORTED_CURRENCIES.find { it.code == row.code }
        return row.code.contains(query, ignoreCase = true) ||
            row.name.contains(query, ignoreCase = true) ||
            (info?.name?.contains(query, ignoreCase = true) == true)
    }

    val favoriteRows = remember(allRateRows, state.favorites, query) {
        allRateRows
            .filter { it.code in state.favorites }
            .filter(::rowMatchesQuery)
    }
    val otherRows = remember(allRateRows, state.favorites, query) {
        allRateRows
            .filter { it.code !in state.favorites }
            .filter(::rowMatchesQuery)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0)
    ) { _ ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh    = viewModel::refresh,
            modifier     = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BaseCurrencySelector(
                        selected = state.baseCurrency,
                        favorites = state.favorites,
                        onSelect = viewModel::onBaseCurrencyChange,
                        modifier = Modifier.weight(1f)
                    )
                    state.lastUpdated?.let { ts ->
                        Text(
                            text  = formatRelativeTime(ts),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 4.dp),
                    placeholder = { Text(stringResource(R.string.settings_search_currency)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true
                )

                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (favoriteRows.isNotEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.dashboard_favorites),
                                    style    = MaterialTheme.typography.labelMedium,
                                    color    = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                                )
                            }
                            items(favoriteRows, key = { "fav_${it.code}" }) { row ->
                                RateRow(
                                    row = row,
                                    isFavorite = true,
                                    onToggleFavorite = { viewModel.toggleFavorite(row.code) },
                                    onAlarm = { viewModel.showAlarmDialog(row) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            }
                        }

                        if (otherRows.isNotEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.dashboard_all_rates),
                                    style    = MaterialTheme.typography.labelMedium,
                                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                                )
                            }
                            items(otherRows, key = { it.code }) { row ->
                                RateRow(
                                    row = row,
                                    isFavorite = false,
                                    onToggleFavorite = { viewModel.toggleFavorite(row.code) },
                                    onAlarm = { viewModel.showAlarmDialog(row) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RateRow(
    row: CurrencyRow,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAlarm: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val trendColor = when {
        row.trend > 0 -> if (isDark) TrendUpDark else TrendUp
        row.trend < 0 -> if (isDark) TrendDownDark else TrendDown
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = row.flag, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                Text(text = row.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(text = row.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }
        Text(text = formatRate(row.rateToTry), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = trendColor, modifier = Modifier.padding(horizontal = 8.dp))
        IconButton(onClick = onAlarm, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.NotificationsNone, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
            Icon(if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder, null, modifier = Modifier.size(20.dp), tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
        }
    }
}

@Composable
private fun BaseCurrencySelector(
    selected: String,
    favorites: Set<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedCard(
        onClick  = { showDialog = true },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.dashboard_base), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(
                SUPPORTED_CURRENCIES.find { it.code == selected }?.flag ?: "",
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 8.dp, end = 6.dp)
            )
            Text(selected, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null)
        }
    }

    if (showDialog) {
        BaseCurrencyDialog(
            current = selected,
            favorites = favorites,
            onDismiss = { showDialog = false },
            onSelect = {
                onSelect(it)
                showDialog = false
            }
        )
    }
}

@Composable
private fun BaseCurrencyDialog(
    current: String,
    favorites: Set<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val localeKey = LocalConfiguration.current.locales.toLanguageTags()
    val filtered = remember(query, favorites, localeKey) {
        val q = query.trim()
        val matches = if (q.isBlank()) {
            SUPPORTED_CURRENCIES
        } else {
            SUPPORTED_CURRENCIES.filter {
                it.code.contains(q, ignoreCase = true) ||
                    it.name.contains(q, ignoreCase = true) ||
                    context.currencyName(it.code).contains(q, ignoreCase = true)
            }
        }
        val favoriteMatches = matches.filter { it.code in favorites }
        val otherMatches = matches.filter { it.code !in favorites }
        favoriteMatches + otherMatches
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxHeight(0.85f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.dashboard_base_currency),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    placeholder = { Text(stringResource(R.string.settings_search_currency)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )
                LazyColumn {
                    items(filtered, key = { it.code }) { info ->
                        ListItem(
                            headlineContent = { Text(info.code) },
                            supportingContent = { Text(currencyName(info.code)) },
                            leadingContent = { Text(info.flag, fontSize = 20.sp) },
                            trailingContent = { if (info.code == current) Icon(Icons.Default.Check, null) },
                            modifier = Modifier.clickable { onSelect(info.code) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmDialog(
    row: CurrencyRow,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean) -> Unit
) {
    var thresholdInput by remember { mutableStateOf("") }
    var isAbove by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.alarm_title, "${row.flag} ${row.code}")
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.alarm_current_rate, formatRate(row.rateToTry)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                OutlinedTextField(
                    value = thresholdInput,
                    onValueChange = { thresholdInput = it },
                    label = { Text(stringResource(R.string.alarm_target)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isAbove, onClick = { isAbove = true }, label = { Text(stringResource(R.string.alarm_when_above)) })
                    FilterChip(selected = !isAbove, onClick = { isAbove = false }, label = { Text(stringResource(R.string.alarm_when_below)) })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val threshold = thresholdInput.replace(",", ".").toDoubleOrNull()
                if (threshold != null) onConfirm(threshold, isAbove)
            }, enabled = thresholdInput.isNotBlank()) { Text(stringResource(R.string.alarm_set)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

private fun formatRate(rate: Double): String {
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    fmt.minimumFractionDigits = 4
    fmt.maximumFractionDigits = 4
    return fmt.format(rate)
}

@Composable
private fun formatRelativeTime(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    val minutes = (diff / 60_000).toInt()
    return when {
        minutes < 1  -> stringResource(R.string.dashboard_just_now)
        minutes < 60 -> pluralStringResource(R.plurals.minutes_ago, minutes, minutes)
        else -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
    }
}
