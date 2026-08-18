package com.hayhak.currencyconverter.ui.settings

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.hayhak.currencyconverter.R
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val assetPath = remember(context) {
        privacyPolicyAssetFor(currentAppLocale(context))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_privacy_policy)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            factory = { ctx ->
                WebView(ctx).apply {
                    @SuppressLint("SetJavaScriptEnabled")
                    settings.javaScriptEnabled = false
                    webViewClient = WebViewClient()
                    loadUrl("file:///android_asset/$assetPath")
                }
            }
        )
    }
}

private fun currentAppLocale(context: android.content.Context): Locale {
    val config = context.resources.configuration
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.locales[0] ?: Locale.getDefault()
    } else {
        @Suppress("DEPRECATION")
        config.locale ?: Locale.getDefault()
    }
}

internal fun privacyPolicyAssetFor(locale: Locale): String {
    val lang = locale.language.lowercase(Locale.ROOT).let {
        if (it == "in") "id" else it
    }
    val preferred = when (lang) {
        "de", "tr", "fr", "es", "ru", "ar", "hi", "id", "vi", "pt", "zh", "pl", "uk", "ja", "ko", "it", "th", "bn", "ur" ->
            "privacy-policy-$lang.html"
        else -> "privacy-policy.html"
    }
    return if (preferred in KNOWN_PRIVACY_ASSETS) preferred else "privacy-policy.html"
}

private val KNOWN_PRIVACY_ASSETS = setOf(
    "privacy-policy.html",
    "privacy-policy-de.html",
    "privacy-policy-tr.html",
)
