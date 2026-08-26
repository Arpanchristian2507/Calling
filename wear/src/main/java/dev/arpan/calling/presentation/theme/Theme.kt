package dev.arpan.calling.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography

private val BrandGreen = Color(0xFF72AD37)
private val BrandGreenDim = Color(0xFF4A7224)
private val OnBrandDark = Color(0xFF0F1608)
private val Cream = Color(0xFFF2F6EC)
private val CreamMuted = Color(0xFFB4C4A8)
private val ForestDeep = Color(0xFF0A0D07)
private val ForestSurface = Color(0xFF12180E)
private val ForestHigh = Color(0xFF1C2612)
private val WarmAccent = Color(0xFF8A6F4A)
private val OutlineGreen = Color(0xFF3D4A32)

private val CallingColorScheme =
    ColorScheme(
        primary = BrandGreen,
        onPrimary = OnBrandDark,
        primaryContainer = BrandGreenDim,
        onPrimaryContainer = Color(0xFFD8EEB8),
        secondary = WarmAccent,
        onSecondary = Color(0xFFFFF8F0),
        secondaryContainer = Color(0xFF3D2E1C),
        onSecondaryContainer = Color(0xFFE8D4BC),
        surfaceContainerLow = ForestDeep,
        surfaceContainer = ForestSurface,
        surfaceContainerHigh = ForestHigh,
        onSurface = Cream,
        onSurfaceVariant = CreamMuted,
        outline = OutlineGreen,
        outlineVariant = Color(0xFF2A3525),
        background = Color(0xFF060805),
        onBackground = Cream,
    )

private val CallingTypography =
    Typography(
        defaultFontFamily = FontFamily.Serif,
    )

@Composable
fun CallingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CallingColorScheme,
        typography = CallingTypography,
        content = content,
    )
}
