package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ApexDuskDarkColorScheme = darkColorScheme(
    primary = SunsetAmber,
    onPrimary = DuskVoid,
    secondary = NitroCyan,
    onSecondary = DuskVoid,
    tertiary = HorizonMagenta,
    onTertiary = TextPrimary,
    background = DuskVoid,
    onBackground = TextPrimary,
    surface = DuskIndigo,
    onSurface = TextPrimary,
    surfaceVariant = DuskViolet,
    onSurfaceVariant = TextSecondary,
    error = CrimsonBrake,
    onError = TextPrimary,
)

@Composable
fun ApexDuskTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ApexDuskDarkColorScheme,
        typography = ApexDuskTypography,
        content = content,
    )
}
