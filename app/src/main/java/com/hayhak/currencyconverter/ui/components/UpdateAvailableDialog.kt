package com.hayhak.currencyconverter.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hayhak.currencyconverter.BuildConfig
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.util.PlayStoreHelper
import com.hayhak.currencyconverter.util.PlayUpdateInfo

@Composable
fun UpdateAvailableDialog(
    @Suppress("UNUSED_PARAMETER") updateInfo: PlayUpdateInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
        title = { Text(stringResource(R.string.update_dialog_title)) },
        text = {
            Text(
                stringResource(R.string.update_dialog_message, BuildConfig.VERSION_NAME),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    PlayStoreHelper.openListing(context)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.update_dialog_positive))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.update_dialog_later))
            }
        },
    )
}
