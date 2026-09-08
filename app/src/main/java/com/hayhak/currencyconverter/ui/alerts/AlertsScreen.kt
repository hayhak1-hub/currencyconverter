package com.hayhak.currencyconverter.ui.alerts

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.RateAlarm
import com.hayhak.currencyconverter.domain.model.getFlagEmoji

@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = hiltViewModel(),
    onOpenPair: (String, String) -> Unit = { _, _ -> }
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<RateAlarm?>(null) }

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

    if (alarms.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.alerts_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

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
        item { Spacer(Modifier.height(8.dp)) }
    }
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                    stringResource(
                        if (alarm.isAbove) R.string.alarm_when_above else R.string.alarm_when_below
                    ) + " ${"%.4f".format(alarm.threshold)}",
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

@Composable
private fun EditAlarmDialog(
    alarm: RateAlarm,
    onDismiss: () -> Unit,
    onSave: (RateAlarm) -> Unit
) {
    var thresholdInput by remember { mutableStateOf("%.4f".format(alarm.threshold).trimEnd('0').trimEnd('.')) }
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = thresholdInput,
                    onValueChange = { thresholdInput = it },
                    label = { Text(stringResource(R.string.alarm_target)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isAbove,
                        onClick = { isAbove = true },
                        label = { Text(stringResource(R.string.alarm_when_above)) }
                    )
                    FilterChip(
                        selected = !isAbove,
                        onClick = { isAbove = false },
                        label = { Text(stringResource(R.string.alarm_when_below)) }
                    )
                }
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
                            threshold = threshold,
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
