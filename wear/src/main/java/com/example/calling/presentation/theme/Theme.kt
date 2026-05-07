package com.example.calling.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

private val AuroraCyan = Color(0xFF4FE8D4)
private val IonViolet = Color(0xFF9A8BFF)
private val VoidSurface = Color(0xFF0B101A)
private val MistOnVoid = Color(0xFFEAF2FF)
private val SteelMuted = Color(0xFF7C889E)

private val CallingColorScheme =
    ColorScheme(
        primary = AuroraCyan,
        onPrimary = Color(0xFF001413),
        primaryContainer = Color(0xFF003D38),
        onPrimaryContainer = AuroraCyan,
        secondary = IonViolet,
        onSecondary = Color(0xFF1A103D),
        secondaryContainer = Color(0xFF2B2255),
        onSecondaryContainer = MistOnVoid,
        surfaceContainerLow = Color(0xFF080C14),
        surfaceContainer = VoidSurface,
        surfaceContainerHigh = Color(0xFF131A28),
        onSurface = MistOnVoid,
        onSurfaceVariant = SteelMuted,
        outline = Color(0xFF2C3548),
        outlineVariant = Color(0xFF1B2433),
        background = Color(0xFF020306),
        onBackground = MistOnVoid,
    )

@Composable
fun CallingTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CallingColorScheme, content = content)
}
