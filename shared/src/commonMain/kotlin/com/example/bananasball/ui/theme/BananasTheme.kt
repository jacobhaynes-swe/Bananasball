package com.example.bananasball.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BananaYellow = Color(0xFFFDE100)
private val BananaNavy = Color(0xFF002D62)

private val LightColorScheme = lightColorScheme(
    primary = BananaYellow,
    onPrimary = Color.Black,
    secondary = BananaNavy,
    onSecondary = Color.White,
    background = Color(0xFFF5F5F5)
)

private val DarkColorScheme = darkColorScheme(
    primary = BananaYellow,
    onPrimary = Color.Black,
    secondary = BananaNavy,
    onSecondary = Color.White
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
