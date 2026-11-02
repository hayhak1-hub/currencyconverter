package com.hayhak.currencyconverter.ui.alerts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.RateAlarm
import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES
import com.hayhak.currencyconverter.ui.components.AlarmDirectionChips
import com.hayhak.currencyconverter.ui.components.CurrencySelector
import com.hayhak.currencyconverter.domain.model.getFlagEmoji

@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = hiltViewModel(),
    onOpenPair: (String, String) -> Unit = { _, _ -> },
    prefill: Pair<String, String>? = null,
    onPrefillConsumed: () -> Unit = {}
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<RateAlarm?>(null) }
    var adding by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(prefill) {
        if (prefill != null) adding = true
    }

    if (adding) {
        AddAlarmDialog(
            initialFrom = prefill?.first,
            initialTo = prefill?.second,
            onDismiss = {
                adding = false
                onPrefillConsumed()
            },
            onSave = { base, target, value, isAbove, repeating, percent ->
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.add(base, target, value, isAbove, repeating, percent)
                adding = false
                onPrefillConsumed()
            }
        )
    }

    editing?.let { alarm ->
        EditAlarmDialog(
            alarm = alarm,
            onDismiss = { editing = null },
            onSave = { updated ->
                viewModel.update(updated)
                editing = null
            }
        )
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (alarms.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.alerts_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = { viewModel.toggle(alarm) },
                        onEdit = { editing = alarm },
                        onDelete = { viewModel.remove(alarm.id) },
                        onOpen = { onOpenPair(alarm.baseCode, alarm.targetCode) }
                    )
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
        FloatingActionButton(
            onClick = { adding = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.alerts_add))
        }
    }
}

@Composable
private fun AddAlarmDialog(
    onDismiss: () -> Unit,
    onSave: (base: String, target: String, value: Double, isAbove: Boolean, repeating: Boolean, percent: Boolean) -> Unit,
    initialFrom: String? = null,
    initialTo: String? = null
) {
    var base by remember {
        mutableStateOf(SUPPORTED_CURRENCIES.find { it.code == initialFrom } ?: SUPPORTED_CURRENCIES.first { it.code == "USD" })
    }
    var target by remember {
        mutableStateOf(SUPPORTED_CURRENCIES.find { it.code == initialTo } ?: SUPPORTED_CURRENCIES.first { it.code == "EUR" })
    }
    var thresholdInput by remember { mutableStateOf("") }
    var isAbove by remember { mutableStateOf(true) }
    var repeating by remember { mutableStateOf(false) }
    var percentMode by remember { mutableStateOf(false) }
    val value = thresholdInput.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.alerts_add)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !percentMode,
                        onClick = { percentMode = false },
                        label = { Text(stringResource(R.string.alarm_mode_level)) }
                    )
                    FilterChip(
                        selected = percentMode,
                        onClick = { percentMode = true },
                        label = { Text(stringResource(R.string.alarm_mode_percent)) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencySelector(
                        label = stringResource(R.string.converter_from),
                        currency = base,
                        onSelect = { base = it },
                        modifier = Modifier.weight(1f)
                    )
                    CurrencySelector(
                        label = stringResource(R.string.converter_to),
                        currency = target,
                        onSelect = { target = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = thresholdInput,
                    onValueChange = { thresholdInput = it },
                    label = { Text(stringResource(if (percentMode) R.string.alarm_percent else R.string.alarm_target)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                AlarmDirectionChips(
                    isAbove = isAbove,
                    onChange = { isAbove = it },
                    aboveRes = if (percentMode) R.string.alarm_when_rises else R.string.alarm_when_above,
                    belowRes = if (percentMode) R.string.alarm_when_falls else R.string.alarm_when_below
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = repeating, onCheckedChange = { repeating = it })
                    Text(stringResource(R.string.alarm_repeating), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { value?.let { onSave(base.code, target.code, it, isAbove, repeating, percentMode) } },
                enabled = value != null && base.code != target.code
            ) { Text(stringResource(R.string.alarm_set)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun AlarmCard(
    alarm: RateAlarm,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit
) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        // Texts get the full width; the controls sit in their own row so long translations and
        // large font sizes never squeeze the pair name into several lines.
        Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (alarm.isEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.padding(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${getFlagEmoji(alarm.baseCode)} ${alarm.baseCode} / ${getFlagEmoji(alarm.targetCode)} ${alarm.targetCode}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (alarm.isPercent) {
                        stringResource(
                            if (alarm.isAbove) R.string.alarm_percent_up_line else R.string.alarm_percent_down_line,
                            "%.1f".format(alarm.percent)
                        )
                    } else {
                        stringResource(
                            if (alarm.isAbove) R.string.alarm_when_above else R.string.alarm_when_below
                        ) + " ${"%.4f".format(alarm.threshold)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (alarm.repeating) {
                    Text(
                        stringResource(R.string.alarm_repeating),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(checked = alarm.isEnabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.alerts_edit))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.alerts_delete))
            }
        }
        }
    }
}

@Composable
private fun EditAlarmDialog(
    alarm: RateAlarm,
    onDismiss: () -> Unit,
    onSave: (RateAlarm) -> Unit
) {
    // Locale-aware, so "50.0000" becomes "50" (or "50,25") instead of "50," where the separator is a comma.
    var percentMode by remember { mutableStateOf(alarm.isPercent) }
    var thresholdInput by remember {
        mutableStateOf(
            java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault()).apply {
                minimumFractionDigits = 0
                maximumFractionDigits = 4
                isGroupingUsed = false
            }.format(if (alarm.isPercent) alarm.percent else alarm.threshold)
        )
    }
    var isAbove by remember { mutableStateOf(alarm.isAbove) }
    var repeating by remember { mutableStateOf(alarm.repeating) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    R.string.alarm_title,
                    "${getFlagEmoji(alarm.baseCode)} ${alarm.baseCode}/${alarm.targetCode}"
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !percentMode,
                        onClick = { percentMode = false },
                        label = { Text(stringResource(R.string.alarm_mode_level)) }
                    )
                    FilterChip(
                        selected = percentMode,
                        onClick = { percentMode = true },
                        label = { Text(stringResource(R.string.alarm_mode_percent)) }
                    )
                }
                OutlinedTextField(
                    value = thresholdInput,
                    onValueChange = { thresholdInput = it },
                    label = { Text(stringResource(if (percentMode) R.string.alarm_percent else R.string.alarm_target)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                AlarmDirectionChips(
                    isAbove = isAbove,
                    onChange = { isAbove = it },
                    aboveRes = if (percentMode) R.string.alarm_when_rises else R.string.alarm_when_above,
                    belowRes = if (percentMode) R.string.alarm_when_falls else R.string.alarm_when_below
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = repeating, onCheckedChange = { repeating = it })
                    Text(stringResource(R.string.alarm_repeating), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val threshold = thresholdInput.replace(",", ".").toDoubleOrNull() ?: return@Button
                    onSave(
                        alarm.copy(
                            threshold = if (percentMode) 0.0 else threshold,
                            percent = if (percentMode) threshold else 0.0,
                            kind = if (percentMode) RateAlarm.KIND_PERCENT else RateAlarm.KIND_THRESHOLD,
                            isAbove = isAbove,
                            repeating = repeating,
                            lastFiredAt = 0L
                        )
                    )
                },
                enabled = thresholdInput.replace(",", ".").toDoubleOrNull() != null
            ) { Text(stringResource(R.string.alarm_set)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
