package com.example.bananasball.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BananaYellow = Color(0xFFFDE100)
private val BananaNavy = Color(0xFF002D62)
private val BananaGold = Color(0xFFFFC72C)

private val LightColorScheme = lightColorScheme(
    primary = BananaYellow,
    onPrimary = Color.Black,
    primaryContainer = BananaGold,
    onPrimaryContainer = Color.Black,
    secondary = BananaNavy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = BananaNavy,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = BananaYellow,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF332900),
    onPrimaryContainer = BananaYellow,
    secondary = Color(0xFF1E3A8A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFF93C5FD),
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
    error = Color(0xFFEF4444),
    onError = Color.White
)

@Composable
fun BananasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
