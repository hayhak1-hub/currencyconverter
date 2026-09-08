package com.hayhak.currencyconverter.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WbAuto
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
import com.hayhak.currencyconverter.util.AnalyticsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector) {
    object Converter  : Screen("converter",  R.string.nav_converter,  Icons.Default.SwapHoriz)
    object Dashboard  : Screen("dashboard",  R.string.nav_rates,      Icons.AutoMirrored.Filled.ShowChart)
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current

    LaunchedEffect(currentDestination?.route) {
        currentDestination?.route?.let { AnalyticsHelper.logScreen(context, it) }
    }

    val currentScreen = screens.find { s ->
        currentDestination?.hierarchy?.any { it.route == s.route } == true
    } ?: Screen.Converter

    val isSubScreen = currentDestination?.route in subRoutes

    fun navigateTo(screen: Screen) {
        scope.launch { drawerState.close() }
        navController.navigate(screen.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val drawerOpening = drawerState.targetValue == DrawerValue.Open
    val drawerProgress by animateFloatAsState(
        targetValue = if (drawerOpening) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "drawerProgress"
    )
    val menuIconRotation by animateFloatAsState(
        targetValue = if (drawerOpening) 90f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "menuIconRotation"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isSubScreen,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .graphicsLayer {
                        val dir = if (layoutDirection == LayoutDirection.Rtl) 1f else -1f
                        translationX = dir * (1f - drawerProgress) * with(density) { 18.dp.toPx() }
                        alpha = 0.55f + 0.45f * drawerProgress
                    }
            ) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)
                )
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                screens.forEachIndexed { index, screen ->
                    var itemVisible by remember(screen.route) { mutableStateOf(false) }
                    LaunchedEffect(drawerOpening) {
                        if (drawerOpening) {
                            delay(40L + index * 38L)
                            itemVisible = true
                        } else {
                            delay((screens.lastIndex - index).coerceAtLeast(0) * 22L)
                            itemVisible = false
                        }
                    }
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.15f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "drawerIconScale"
                    )
                    AnimatedVisibility(
                        visible = itemVisible,
                        enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(
                                animationSpec = tween(220, easing = FastOutSlowInEasing),
                                initialOffsetX = { if (layoutDirection == LayoutDirection.Rtl) it / 4 else -it / 4 }
                            ),
                        exit = slideOutHorizontally(
                            animationSpec = tween(160),
                            targetOffsetX = { if (layoutDirection == LayoutDirection.Rtl) it / 5 else -it / 5 }
                        )
                    ) {
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    stringResource(screen.labelRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            icon = {
                                Icon(
                                    screen.icon,
                                    contentDescription = null,
                                    modifier = Modifier.scale(iconScale)
                                )
                            },
                            selected = selected,
                            onClick = { navigateTo(screen) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.graphicsLayer {
                val t = drawerProgress
                val push = with(density) { 12.dp.toPx() } * t
                scaleX = 1f - 0.035f * t
                scaleY = 1f - 0.035f * t
                translationX = if (layoutDirection == LayoutDirection.Rtl) -push else push
                alpha = 1f - 0.06f * t
            },
            topBar = {
                if (!isSubScreen) {
                    TopAppBar(
                        title = {
                            Text(
                                stringResource(currentScreen.labelRes),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.cd_menu),
                                    modifier = Modifier.graphicsLayer { rotationZ = menuIconRotation }
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                themeViewModel.setTheme(
                                    when (themeMode) {
                                        ThemeMode.SYSTEM -> ThemeMode.LIGHT
                                        ThemeMode.LIGHT -> ThemeMode.DARK
                                        ThemeMode.DARK -> ThemeMode.AMOLED
                                        ThemeMode.AMOLED -> ThemeMode.SYSTEM
                                    }
                                )
                            }) {
                                Icon(
                                    imageVector = when (themeMode) {
                                        ThemeMode.SYSTEM -> Icons.Default.WbAuto
                                        ThemeMode.LIGHT -> Icons.Default.LightMode
                                        ThemeMode.DARK -> Icons.Default.DarkMode
                                        ThemeMode.AMOLED -> Icons.Default.Contrast
                                    },
                                    contentDescription = stringResource(R.string.cd_theme)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Converter.route
                ) {
                    composable(Screen.Converter.route) { ConverterScreen() }
                    composable(Screen.Dashboard.route) { DashboardScreen() }
                    composable(Screen.Statistics.route) { StatisticsScreen() }
                    composable(Screen.History.route) { HistoryScreen() }
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
}
