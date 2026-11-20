package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SahajPdfLightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = WhiteBackground,
    primaryContainer = LightBlueHeader,
    onPrimaryContainer = DeepInk,
    secondary = RoyalBlueDark,
    onSecondary = WhiteBackground,
    secondaryContainer = LightBlueTint,
    onSecondaryContainer = DeepInk,
    tertiary = SkyBlueAccent,
    onTertiary = DeepInk,
    background = WhiteBackground,
    onBackground = DeepInk,
    surface = WhiteBackground,
    onSurface = DeepInk,
    surfaceVariant = SoftSurface,
    onSurfaceVariant = MutedInk,
    surfaceContainerLowest = WhiteBackground,
    surfaceContainerLow = SoftSurface,
    surfaceContainer = LightBlueTint,
    surfaceContainerHigh = LightBlueHeader,
    outline = SubtleBorder,
    outlineVariant = LightBlueHeaderBorder,
    error = ErrorCrimson,
    onError = WhiteBackground,
    errorContainer = ErrorCrimsonContainer,
    onErrorContainer = ErrorCrimson
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SahajPdfTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SahajPdfLightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
