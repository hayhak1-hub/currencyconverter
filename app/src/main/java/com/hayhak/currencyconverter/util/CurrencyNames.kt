package com.hayhak.currencyconverter.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.hayhak.currencyconverter.domain.model.currencyByCode
import java.util.Locale

fun Context.currencyName(code: String): String {
    val id = resources.getIdentifier(
        "currency_${code.lowercase(Locale.US)}",
        "string",
        packageName
    )
    if (id != 0) return getString(id)
    return currencyByCode(code)?.name ?: code
}

@Composable
fun currencyName(code: String): String = LocalContext.current.currencyName(code)
