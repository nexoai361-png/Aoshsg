package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Forces VS Code Dark Scheme everywhere
private val DarkColorScheme = darkColorScheme(
    primary = VsCodeStatusBar,
    secondary = VsCodePurple,
    tertiary = VsCodeTeal,
    background = VsCodeBackground,
    surface = VsCodeSidebar,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = VsCodeTextPrimary,
    onSurface = VsCodeTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme by default
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve VS Code aesthetic
    content: @Composable () -> Unit,
) {
    // We enforce our specific VS Code Dark theme colors
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
