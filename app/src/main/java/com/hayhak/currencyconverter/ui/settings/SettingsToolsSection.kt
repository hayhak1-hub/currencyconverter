package com.hayhak.currencyconverter.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.QuietHours
import com.hayhak.currencyconverter.util.formatMinuteOfDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsToolsSection(viewModel: SettingsToolsViewModel = hiltViewModel()) {
    val spread by viewModel.spread.collectAsStateWithLifecycle()
    val quiet by viewModel.quiet.collectAsStateWithLifecycle()
    val digest by viewModel.digest.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var spreadText by rememberSaveable { mutableStateOf<String?>(null) }
    var pickingStart by rememberSaveable { mutableStateOf(false) }
    var pickingEnd by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                val json = viewModel.exportJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) } != null
            }
            toast(context, if (ok) R.string.settings_backup_exported else R.string.settings_backup_failed)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                json != null && viewModel.importJson(json)
            }
            if (ok) spreadText = null
            toast(context, if (ok) R.string.settings_backup_imported else R.string.settings_backup_failed)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.settings_spread), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.settings_spread_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
        OutlinedTextField(
            value = spreadText ?: formatSpread(spread),
            onValueChange = { raw ->
                spreadText = raw
                raw.replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..20.0 }?.let(viewModel::setSpread)
            },
            label = { Text(stringResource(R.string.settings_spread_percent)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_quiet_hours), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.settings_quiet_hours_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            Switch(
                checked = quiet.enabled,
                onCheckedChange = { viewModel.setQuiet(quiet.copy(enabled = it)) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pickingStart = true }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_quiet_from) + " " + formatMinuteOfDay(quiet.startMinute))
            }
            OutlinedButton(onClick = { pickingEnd = true }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_quiet_until) + " " + formatMinuteOfDay(quiet.endMinute))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_digest), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.settings_digest_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            Switch(checked = digest, onCheckedChange = viewModel::setDigest)
        }

        Text(
            stringResource(R.string.settings_backup),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            stringResource(R.string.settings_backup_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { exportLauncher.launch("currency-converter-backup.json") }) {
                Text(stringResource(R.string.settings_backup_export))
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                Text(stringResource(R.string.settings_backup_import))
            }
        }
    }

    if (pickingStart) {
        MinutePickerDialog(
            title = stringResource(R.string.settings_quiet_from),
            minute = quiet.startMinute,
            onDismiss = { pickingStart = false },
            onConfirm = { viewModel.setQuiet(quiet.copy(startMinute = it)); pickingStart = false }
        )
    }
    if (pickingEnd) {
        MinutePickerDialog(
            title = stringResource(R.string.settings_quiet_until),
            minute = quiet.endMinute,
            onDismiss = { pickingEnd = false },
            onConfirm = { viewModel.setQuiet(quiet.copy(endMinute = it)); pickingEnd = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinutePickerDialog(
    title: String,
    minute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = minute.coerceIn(0, 23 * 60) / 60,
        initialMinute = minute.coerceIn(0, 23 * 60) % 60,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text(stringResource(R.string.converter_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

private fun formatSpread(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private fun toast(context: android.content.Context, message: Int) {
    Toast.makeText(context, context.getString(message), Toast.LENGTH_SHORT).show()
}
