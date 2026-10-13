package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    primaryContainer = SurfaceCardElevated,
    onPrimaryContainer = CyberCyan,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = SurfaceCard,
    onSecondaryContainer = TextPrimary,
    tertiary = GlyphTriangleGreen,
    background = MidnightBackground,
    onBackground = TextPrimary,
    surface = DeepSurface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle
)

@Composable
fun CustomControlsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
