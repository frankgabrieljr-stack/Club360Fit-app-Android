package com.club360fit.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BurgundyPrimary,
    onPrimary = OnBurgundy,
    primaryContainer = BurgundyLight,
    onPrimaryContainer = White,
    secondary = BurgundyLight,
    onSecondary = OnBurgundy,
    background = BackgroundLight,
    onBackground = OnSurface,
    surface = SurfaceLight,
    onSurface = OnSurface,
    outline = Outline
)

@Composable
fun Club360FitTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
