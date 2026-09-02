package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = LoriFrostedPrimaryDark,
    onPrimary = LoriFrostedOnPrimaryDark,
    primaryContainer = LoriFrostedPrimaryContainerDark,
    onPrimaryContainer = LoriFrostedOnPrimaryContainerDark,
    secondary = LoriFrostedSecondaryDark,
    onSecondary = LoriFrostedOnSecondaryDark,
    secondaryContainer = LoriFrostedSecondaryContainerDark,
    onSecondaryContainer = LoriFrostedOnSecondaryContainerDark,
    tertiary = LoriFrostedTertiaryLight,
    onTertiary = LoriFrostedOnTertiaryLight,
    background = LoriFrostedDarkBackground,
    onBackground = LoriFrostedDarkOnBackground,
    surface = LoriFrostedDarkSurface,
    onSurface = LoriFrostedDarkOnSurface,
    surfaceVariant = LoriFrostedDarkSurfaceVariant,
    onSurfaceVariant = LoriFrostedDarkOnSurfaceVariant,
    outline = LoriFrostedDarkOutline,
    error = LoriError
)

private val LightColorScheme = lightColorScheme(
    primary = LoriFrostedPrimaryLight,
    onPrimary = LoriFrostedOnPrimaryLight,
    primaryContainer = LoriFrostedPrimaryContainerLight,
    onPrimaryContainer = LoriFrostedOnPrimaryContainerLight,
    secondary = LoriFrostedSecondaryLight,
    onSecondary = LoriFrostedOnSecondaryLight,
    secondaryContainer = LoriFrostedSecondaryContainerLight,
    onSecondaryContainer = LoriFrostedOnSecondaryContainerLight,
    tertiary = LoriFrostedTertiaryLight,
    onTertiary = LoriFrostedOnTertiaryLight,
    tertiaryContainer = LoriFrostedTertiaryContainerLight,
    onTertiaryContainer = LoriFrostedOnTertiaryContainerLight,
    background = LoriFrostedLightBackground,
    onBackground = LoriFrostedLightOnBackground,
    surface = LoriFrostedLightSurface,
    onSurface = LoriFrostedLightOnSurface,
    surfaceVariant = LoriFrostedLightSurfaceVariant,
    onSurfaceVariant = LoriFrostedLightOnSurfaceVariant,
    outline = LoriFrostedLightOutline,
    error = LoriError
)

@Composable
fun LoriTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = LoriTheme(darkTheme, dynamicColor, content)
