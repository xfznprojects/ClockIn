package com.clockin.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenPrimaryDark,
    onPrimaryContainer = GreenPrimaryLight,
    secondary = GreenAccent,
    onSecondary = DarkBackground,
    secondaryContainer = GreenPrimaryDark,
    onSecondaryContainer = GreenPrimaryLight,
    tertiary = WarmAccent,
    onTertiary = DarkBackground,
    tertiaryContainer = WarmAccent.copy(alpha = 0.2f),
    onTertiaryContainer = WarmAccent,
    background = DarkBackground,
    onBackground = OnDarkSurface,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceSecondary,
    outline = OnDarkSurfaceSecondary.copy(alpha = 0.45f),
    error = ScanFailRed
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = GreenPrimaryLight,
    onPrimaryContainer = GreenPrimaryDark,
    secondary = GreenPrimary,
    onSecondary = Color.White,
    secondaryContainer = GreenPrimaryLight.copy(alpha = 0.3f),
    onSecondaryContainer = GreenPrimaryDark,
    tertiary = WarmAccent,
    onTertiary = Color.White,
    tertiaryContainer = WarmAccent.copy(alpha = 0.25f),
    onTertiaryContainer = Color(0xFF4E3700),
    background = LightBackground,
    onBackground = OnLightSurface,
    surface = LightSurface,
    onSurface = OnLightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnLightSurfaceSecondary,
    outline = OnLightSurfaceSecondary.copy(alpha = 0.45f),
    error = ScanFailRed
)

private val ClockInShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun ClockInTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ClockInTypography,
        shapes = ClockInShapes,
        content = content
    )
}
