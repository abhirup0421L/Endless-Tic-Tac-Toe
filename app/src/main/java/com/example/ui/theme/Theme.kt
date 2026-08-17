package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PlayerXBlue,
    onPrimary = Color.White,
    secondary = PlayerORed,
    onSecondary = Color.White,
    tertiary = GameYellowDark,
    background = GameYellowBackground,
    onBackground = TextDark,
    surface = Color.White,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFFEF08A),
    onSurfaceVariant = TextDarkSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = PlayerXBlueLight,
    onPrimary = Color.White,
    secondary = PlayerORedLight,
    onSecondary = Color.White,
    tertiary = GameYellowVibrant,
    background = GameYellowBackground,
    onBackground = TextDark,
    surface = BoardCardBg,
    onSurface = TextLight,
    surfaceVariant = CellDarkBg,
    onSurfaceVariant = TextLightSecondary
)

@Composable
fun EndlessTTTTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = GameYellowBackground.toArgb()
            window.navigationBarColor = GameYellowBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
