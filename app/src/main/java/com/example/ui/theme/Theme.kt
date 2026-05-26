package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CyberColorScheme = darkColorScheme(
    primary = RetroPink,
    secondary = NeonCyan,
    tertiary = BrightYellow,
    background = CyberDark,
    surface = CyberCard,
    onPrimary = CyberDark,
    onSecondary = CyberDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for driving environment
    dynamicColor: Boolean = false, // Use our custom handcrafted Synthwave theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}
