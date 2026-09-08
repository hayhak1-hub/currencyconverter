package com.hayhak.currencyconverter.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.CurrencyInfo
import com.hayhak.currencyconverter.domain.model.prioritizedCurrencies
import com.hayhak.currencyconverter.util.currencyName

@Composable
fun CurrencySelector(
    label: String,
    currency: CurrencyInfo,
    onSelect: (CurrencyInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    OutlinedCard(
        onClick = { open = true },
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(currency.flag, fontSize = 20.sp, modifier = Modifier.padding(end = 6.dp))
                Text(currency.code, fontWeight = FontWeight.Bold)
            }
        }
    }
    if (open) {
        Dialog(onDismissRequest = { open = false }) {
            Card(modifier = Modifier.fillMaxHeight(0.85f).fillMaxWidth()) {
                var query by remember { mutableStateOf("") }
                val filtered = remember(query) { prioritizedCurrencies(query) }
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.converter_select),
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        placeholder = { Text(stringResource(R.string.settings_search_currency)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )
                    LazyColumn {
                        items(filtered, key = { it.code }) { c ->
                            ListItem(
                                headlineContent = { Text(c.code) },
                                supportingContent = { Text(currencyName(c.code)) },
                                leadingContent = { Text(c.flag, fontSize = 20.sp) },
                                modifier = Modifier.clickable {
                                    onSelect(c)
                                    open = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
