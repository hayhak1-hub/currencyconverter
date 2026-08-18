package com.hayhak.currencyconverter.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.ui.converter.ConverterScreen
import com.hayhak.currencyconverter.ui.dashboard.DashboardScreen
import com.hayhak.currencyconverter.ui.history.HistoryScreen
import com.hayhak.currencyconverter.ui.settings.HelpScreen
import com.hayhak.currencyconverter.ui.settings.PrivacyPolicyScreen
import com.hayhak.currencyconverter.ui.settings.SettingsScreen
import com.hayhak.currencyconverter.ui.statistics.StatisticsScreen
import com.hayhak.currencyconverter.ui.theme.ThemeMode
import com.hayhak.currencyconverter.ui.theme.ThemeViewModel

sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector) {
    object Converter  : Screen("converter",  R.string.nav_converter,  Icons.Default.SwapHoriz)
    object Dashboard  : Screen("dashboard",  R.string.nav_rates,      Icons.Default.ShowChart)
    object Statistics : Screen("statistics", R.string.nav_statistics, Icons.Default.BarChart)
    object History    : Screen("history",    R.string.nav_history,    Icons.Default.History)
    object Settings   : Screen("settings",   R.string.nav_settings,   Icons.Default.Settings)
}

private val screens = listOf(
    Screen.Converter,
    Screen.Dashboard,
    Screen.Statistics,
    Screen.History,
    Screen.Settings
)

private val subRoutes = setOf("settings/help", "settings/privacy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyNavGraph(
    themeViewModel: ThemeViewModel,
    @Suppress("UNUSED_PARAMETER") windowSizeClass: Any? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()

    val currentScreen = screens.find { s ->
        currentDestination?.hierarchy?.any { it.route == s.route } == true
    } ?: Screen.Converter

    val isSubScreen = currentDestination?.route in subRoutes

    Scaffold(
        topBar = {
            if (!isSubScreen) {
                CenterAlignedTopAppBar(
                title = { Text(stringResource(currentScreen.labelRes)) },
                actions = {
                    IconButton(onClick = {
                        themeViewModel.setTheme(when (themeMode) {
                            ThemeMode.SYSTEM -> ThemeMode.LIGHT
                            ThemeMode.LIGHT  -> ThemeMode.DARK
                            ThemeMode.DARK   -> ThemeMode.AMOLED
                            ThemeMode.AMOLED -> ThemeMode.SYSTEM
                        })
                    }) {
                        Icon(
                            imageVector = when (themeMode) {
                                ThemeMode.SYSTEM -> Icons.Default.WbAuto
                                ThemeMode.LIGHT  -> Icons.Default.LightMode
                                ThemeMode.DARK   -> Icons.Default.DarkMode
                                ThemeMode.AMOLED -> Icons.Default.Contrast
                            },
                            contentDescription = stringResource(R.string.cd_theme)
                        )
                    }
                }
            )
            }
        },
        bottomBar = {
            if (!isSubScreen) {
            NavigationBar {
                screens.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, null) },
                        label = { Text(stringResource(screen.labelRes)) }
                    )
                }
            }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Converter.route
            ) {
                composable(Screen.Converter.route)  { ConverterScreen() }
                composable(Screen.Dashboard.route)  { DashboardScreen() }
                composable(Screen.Statistics.route) { StatisticsScreen() }
                composable(Screen.History.route)    { HistoryScreen() }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        themeViewModel = themeViewModel,
                        onNavigateToHelp = { navController.navigate("settings/help") },
                        onNavigateToPrivacy = { navController.navigate("settings/privacy") }
                    )
                }
                composable("settings/help") {
                    HelpScreen(onBack = { navController.popBackStack() })
                }
                composable("settings/privacy") {
                    PrivacyPolicyScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
