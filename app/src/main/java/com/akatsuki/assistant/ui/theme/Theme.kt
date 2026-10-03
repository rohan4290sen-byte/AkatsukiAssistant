package com.akatsuki.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AkatsukiRed,
    onPrimary = AkatsukiTextPrimary,
    primaryContainer = AkatsukiRedDim,
    onPrimaryContainer = AkatsukiRedGlow,
    surface = AkatsukiSurface,
    onSurface = AkatsukiTextPrimary,
    background = AkatsukiBlack,
    onBackground = AkatsukiTextPrimary
)

@Composable
fun AkatsukiAssistantTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
