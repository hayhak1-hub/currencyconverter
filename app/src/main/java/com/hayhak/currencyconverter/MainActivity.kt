package com.hayhak.currencyconverter

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayhak.currencyconverter.ui.navigation.CurrencyNavGraph
import com.hayhak.currencyconverter.ui.theme.CurrencyConverterTheme
import com.hayhak.currencyconverter.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()
    private var launchExtras by mutableStateOf<LaunchExtras?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchExtras = LaunchExtras.from(intent)
        addOnNewIntentListener { launchExtras = LaunchExtras.from(it) }

        setContent {
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by themeViewModel.dynamicColor.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)

            CurrencyConverterTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CurrencyNavGraph(
                        themeViewModel = themeViewModel,
                        windowWidth = windowSizeClass.widthSizeClass,
                        launchExtras = launchExtras,
                        onLaunchConsumed = { launchExtras = null }
                    )
                }
            }
        }
    }

    data class LaunchExtras(
        val route: String?,
        val from: String?,
        val target: String?
    ) {
        companion object {
            fun from(intent: Intent?) = LaunchExtras(
                route = intent?.getStringExtra(EXTRA_NAV),
                from = intent?.getStringExtra(EXTRA_FROM),
                target = intent?.getStringExtra(EXTRA_TARGET)
            )
        }
    }

    companion object {
        const val EXTRA_NAV = "nav"
        const val EXTRA_FROM = "from"
        const val EXTRA_TARGET = "target"
        const val NAV_ALERTS = "alerts"
        const val NAV_CONVERTER = "converter"
        const val NAV_METALS = "metals"
    }
}
