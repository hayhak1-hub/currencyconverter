package com.hayhak.currencyconverter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES
import com.hayhak.currencyconverter.domain.model.quotePerUnit
import com.hayhak.currencyconverter.util.currencyName
import com.hayhak.currencyconverter.util.formatMoney

@Composable
fun CrossRateMatrix(
    selectedCodes: List<String>,
    rates: Map<String, Double>,
    onPairClick: (from: String, to: String) -> Unit,
    onUpdateCodes: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSelectDialog by remember { mutableStateOf(false) }
    val horizontalScrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.matrix_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = { showSelectDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.matrix_select_currencies), style = MaterialTheme.typography.labelMedium)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Column {
                Row {
                    Box(
                        modifier = Modifier
                            .size(76.dp, 44.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("1 / →", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    selectedCodes.forEach { colCode ->
                        val flag = SUPPORTED_CURRENCIES.find { it.code == colCode }?.flag ?: ""
                        Box(
                            modifier = Modifier
                                .size(88.dp, 44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$flag $colCode",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                selectedCodes.forEach { rowCode ->
                    val rowFlag = SUPPORTED_CURRENCIES.find { it.code == rowCode }?.flag ?: ""
                    Row {
                        Box(
                            modifier = Modifier
                                .size(76.dp, 44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$rowFlag $rowCode",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        selectedCodes.forEach { colCode ->
                            val isSame = rowCode == colCode
                            val crossRate = if (isSame) 1.0 else quotePerUnit(rowCode, colCode, rates)
                            val cellBg = if (isSame)
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            else
                                MaterialTheme.colorScheme.surface

                            Box(
                                modifier = Modifier
                                    .size(88.dp, 44.dp)
                                    .background(cellBg)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    .clickable(enabled = !isSame && crossRate != null) {
                                        onPairClick(rowCode, colCode)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSame) {
                                    Text(
                                        "1.000",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                    )
                                } else {
                                    Text(
                                        text = crossRate?.let { formatMoney(it, colCode) } ?: "—",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSelectDialog) {
        MatrixCurrencySelectDialog(
            selectedCodes = selectedCodes,
            onDismiss = { showSelectDialog = false },
            onUpdate = { newCodes ->
                onUpdateCodes(newCodes)
                showSelectDialog = false
            }
        )
    }
}

@Composable
private fun MatrixCurrencySelectDialog(
    selectedCodes: List<String>,
    onDismiss: () -> Unit,
    onUpdate: (List<String>) -> Unit
) {
    var currentSelected by remember { mutableStateOf(selectedCodes.toSet()) }
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current

    val filtered = remember(query) {
        val q = query.trim()
        if (q.isBlank()) SUPPORTED_CURRENCIES
        else SUPPORTED_CURRENCIES.filter {
            it.code.contains(q, ignoreCase = true) ||
            it.name.contains(q, ignoreCase = true) ||
            context.currencyName(it.code).contains(q, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.matrix_select_currencies),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    placeholder = { Text(stringResource(R.string.settings_search_currency)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered, key = { it.code }) { info ->
                        val checked = info.code in currentSelected
                        ListItem(
                            headlineContent = { Text(info.code, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(context.currencyName(info.code)) },
                            leadingContent = { Text(info.flag, fontSize = 20.sp) },
                            trailingContent = {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        currentSelected = if (checked) currentSelected - info.code else currentSelected + info.code
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                currentSelected = if (checked) currentSelected - info.code else currentSelected + info.code
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onUpdate(currentSelected.toList()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = currentSelected.size >= 2
                ) {
                    Text(stringResource(R.string.converter_done))
                }
            }
        }
    }
}
