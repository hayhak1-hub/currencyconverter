package com.hayhak.currencyconverter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayhak.currencyconverter.ui.navigation.CurrencyNavGraph
import com.hayhak.currencyconverter.ui.theme.CurrencyConverterTheme
import com.hayhak.currencyconverter.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by themeViewModel.dynamicColor.collectAsStateWithLifecycle()

            CurrencyConverterTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    CurrencyNavGraph(themeViewModel = themeViewModel)
                }
            }
        }
    }
}
