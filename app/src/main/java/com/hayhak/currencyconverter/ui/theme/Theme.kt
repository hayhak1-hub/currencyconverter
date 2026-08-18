package com.hayhak.currencyconverter.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

// ── Koyu tema (gece mavisi) ─────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary              = Blue80,
    onPrimary            = Color(0xFF003149),
    primaryContainer     = BluePrimCont,
    onPrimaryContainer   = BluePrimContOn,
    secondary            = Cyan80,
    onSecondary          = Color(0xFF003640),
    secondaryContainer   = CyanSecCont,
    onSecondaryContainer = CyanSecContOn,
    tertiary             = LightBlue80,
    onTertiary           = Color(0xFF0D2D4A),
    tertiaryContainer    = LightBlueTerCont,
    onTertiaryContainer  = LightBlueTerContOn,
    background           = DarkBg,
    onBackground         = DarkOnSurface,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVar,
    onSurfaceVariant     = DarkOnSurfaceVar,
    outline              = Color(0xFF4A6880),
    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
)

// ── AMOLED Siyah (DarkColorScheme + tam siyah yüzeyler) ────────────
private val AmoledColorScheme = DarkColorScheme.copy(
    background     = AmoledBg,
    surface        = AmoledSurface,
    surfaceVariant = AmoledSurfaceVar,
)

// ── Açık tema ───────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary              = Blue40,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFD1E4FF),
    onPrimaryContainer   = Color(0xFF001C3B),
    secondary            = BlueGrey40,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFCDE7F4),
    onSecondaryContainer = Color(0xFF0B1E27),
    tertiary             = Teal40,
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFF9EF2E4),
    onTertiaryContainer  = Color(0xFF00201C),
    background           = Color(0xFFF5F7FA),
    onBackground         = Color(0xFF1A1C1E),
    surface              = Color(0xFFF5F7FA),
    onSurface            = Color(0xFF1A1C1E),
    surfaceVariant       = Color(0xFFDEE3EA),
    onSurfaceVariant     = Color(0xFF41474E),
    outline              = Color(0xFF71787E),
    error                = Color(0xFFBA1A1A),
    onError              = Color.White,
)

@Composable
fun CurrencyConverterTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.LIGHT                  -> false
        ThemeMode.SYSTEM                 -> systemDark
    }

    val colorScheme = when {
        // Material You — Android 12+
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            // AMOLED modunda dynamic color'ın üstüne siyah yüzeyler uygula
            if (themeMode == ThemeMode.AMOLED) base.copy(
                background     = AmoledBg,
                surface        = AmoledSurface,
                surfaceVariant = AmoledSurfaceVar,
            ) else base
        }
        themeMode == ThemeMode.AMOLED -> AmoledColorScheme
        darkTheme                     -> DarkColorScheme
        else                          -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor     = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars     = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
