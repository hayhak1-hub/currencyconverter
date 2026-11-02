package com.hayhak.currencyconverter.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.util.PlayStoreHelper

@Composable
fun RatePromptDialog(
    onOk: () -> Unit,
    onLater: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onLater,
        icon = { Icon(Icons.Default.Star, contentDescription = null) },
        title = { Text(stringResource(R.string.rate_prompt_title)) },
        text = { Text(stringResource(R.string.rate_prompt_message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    PlayStoreHelper.openListing(context)
                    onOk()
                }
            ) {
                Text(stringResource(R.string.rate_prompt_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text(stringResource(R.string.rate_prompt_later))
            }
        }
    )
}
