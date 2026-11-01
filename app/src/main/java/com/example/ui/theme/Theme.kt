package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudioColorScheme = lightColorScheme(
    primary = ElectricIndigo,
    onPrimary = PureWhite,
    primaryContainer = ElectricIndigoLight,
    onPrimaryContainer = StudioSlate900,
    secondary = StudioSlate800,
    onSecondary = PureWhite,
    secondaryContainer = StudioSlate200,
    onSecondaryContainer = StudioSlate900,
    tertiary = AmberInstrument,
    onTertiary = StudioSlate900,
    background = StudioAlabaster,
    onBackground = StudioSlate900,
    surface = PureWhite,
    onSurface = StudioSlate900,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = StudioSlate700,
    error = CrimsonDanger,
    onError = PureWhite
)

@Composable
fun SmartBoardProTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioColorScheme,
        typography = Typography,
        content = content
    )
}
