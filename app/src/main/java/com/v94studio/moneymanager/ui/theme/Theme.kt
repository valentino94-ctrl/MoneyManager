package com.v94studio.moneymanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.v94studio.moneymanager.ui.settings.ThemeMode

@Immutable
data class GradientColors(
    val top: Color = Color.Unspecified,
    val bottom: Color = Color.Unspecified,
    val container: Color = Color.Unspecified
)

val LocalGradientColors = staticCompositionLocalOf { GradientColors() }

private val LightColors = lightColorScheme(
    primary = BrandPurple,
    onPrimary = Color.White,
    secondary = BrandPurpleLight,
    onSecondary = Color.White,
    background = FintechBackground,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Lavender100,
    onSurfaceVariant = FintechSecondary,
    outline = OutlineSoft,
    outlineVariant = OutlineSoft,
    tertiary = AccentGold
)

private val DarkColors = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    secondary = PurpleSecondary,
    onSecondary = Color.White,
    background = DarkBg,
    onBackground = DarkOn,
    surface = DarkSurface,
    onSurface = DarkOn,
    surfaceVariant = PurpleDark,
    onSurfaceVariant = DarkSecondary,
    outline = PurpleDark,
    outlineVariant = PurpleDark,
    tertiary = AccentGold
)

private val LightGradientColors = GradientColors(
    top = FintechBackground,
    bottom = Color(0xFFF0EFFF),
    container = FintechBackground
)

private val DarkGradientColors = GradientColors(
    top = Color(0xFF1B1525),
    bottom = Color(0xFF241B34),
    container = Color(0xFF1B1525)
)

@Composable
fun MoneyManagerTheme(
    themeMode: ThemeMode,
    spendingStatus: Int = 0,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    
    val colors = if (useDark) DarkColors else LightColors
    
    val baseGradients = if (useDark) DarkGradientColors else LightGradientColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDark
        }
    }
    
    // Dynamic Gradient based on spending status (0: Normal, 1: Warning, 2: Alert)
    val gradients = remember(baseGradients, spendingStatus, useDark) {
        when (spendingStatus) {
            2 -> { // Over budget: Warm/Alert tone
                if (useDark) {
                    GradientColors(
                        top = Color(0xFF2D1B1B), // Deep red tint
                        bottom = Color(0xFF1B1525),
                        container = Color(0xFF1B1525)
                    )
                } else {
                    GradientColors(
                        top = Color(0xFFFFF5F5), // Soft red tint
                        bottom = Color(0xFFF6F1F7),
                        container = Color(0xFFF6F1F7)
                    )
                }
            }
            1 -> { // Near limit: Warning/Yellow tone
                if (useDark) {
                    GradientColors(
                        top = Color(0xFF2D2A1B), // Deep yellow tint
                        bottom = Color(0xFF1B1525),
                        container = Color(0xFF1B1525)
                    )
                } else {
                    GradientColors(
                        top = Color(0xFFFFFDF5), // Soft yellow tint
                        bottom = Color(0xFFF6F1F7),
                        container = Color(0xFFF6F1F7)
                    )
                }
            }
            else -> baseGradients
        }
    }

    CompositionLocalProvider(
        LocalGradientColors provides gradients
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = MoneyTypography,
            content = content
        )
    }
}
