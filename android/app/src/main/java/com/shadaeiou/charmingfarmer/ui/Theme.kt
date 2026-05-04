package com.shadaeiou.charmingfarmer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F7A2F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE6A8),
    onPrimaryContainer = Color(0xFF1B2C0A),
    secondary = Color(0xFFB07A2A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color(0xFF2A1A05),
    tertiary = Color(0xFF7B5E3F),
    background = Color(0xFFFFF8E9),
    onBackground = Color(0xFF2B2418),
    surface = Color(0xFFFFFBEE),
    onSurface = Color(0xFF2B2418),
    surfaceVariant = Color(0xFFEBE2C9),
    onSurfaceVariant = Color(0xFF55503D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8CC7C),
    onPrimary = Color(0xFF1F3408),
    primaryContainer = Color(0xFF324D17),
    onPrimaryContainer = Color(0xFFC9E5A0),
    secondary = Color(0xFFE9B86E),
    onSecondary = Color(0xFF3A2606),
    secondaryContainer = Color(0xFF55390F),
    onSecondaryContainer = Color(0xFFFFE0B2),
    tertiary = Color(0xFFD3B997),
    background = Color(0xFF1A1810),
    onBackground = Color(0xFFEDE6D2),
    surface = Color(0xFF221F15),
    onSurface = Color(0xFFEDE6D2),
    surfaceVariant = Color(0xFF3A3625),
    onSurfaceVariant = Color(0xFFC9C2A8),
)

@Composable
fun FarmerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
