package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = CoralSpark,
    onPrimary = OnCoralSpark,
    primaryContainer = CoralSparkContainer,
    onPrimaryContainer = Color(0xFFFFDAD2),
    secondary = ElectricViolet,
    onSecondary = OnElectricViolet,
    secondaryContainer = ElectricVioletContainer,
    onSecondaryContainer = Color(0xFFE4DFFF),
    tertiary = WarmGold,
    onTertiary = OnWarmGold,
    tertiaryContainer = WarmGoldContainer,
    onTertiaryContainer = Color(0xFFFFE5B0),
    background = StudioObsidian,
    onBackground = StudioTextPrimary,
    surface = StudioSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = StudioTextSecondary,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceHigh,
    outline = StudioOutline,
    outlineVariant = Color(0xFF2E2A40),
    error = StudioError,
    onError = Color(0xFF3E0010),
    errorContainer = StudioErrorContainer,
    onErrorContainer = Color(0xFFFFD9DF)
)

private val StudioLightColorScheme = lightColorScheme(
    primary = CoralSparkLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD2),
    onPrimaryContainer = Color(0xFF420C00),
    secondary = ElectricVioletLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E0FF),
    onSecondaryContainer = Color(0xFF1A0F66),
    tertiary = WarmGoldLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE9C2),
    onTertiaryContainer = Color(0xFF331F00),
    background = LightCanvasBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLow = Color(0xFFF4F1EC),
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceVariant,
    outline = LightOutline,
    outlineVariant = Color(0xFFE6E1EC),
    error = Color(0xFFD32F43),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD9),
    onErrorContainer = Color(0xFF41000B)
)

val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PosterStudioTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = StudioShapes,
        content = content
    )
}
