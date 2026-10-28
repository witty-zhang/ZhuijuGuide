package com.lzzhang.zhuijuguide.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val LightColors = lightColorScheme(
    primary = Color(0xFF116B60),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC6F0E9),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF7A5900),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDF9B),
    onSecondaryContainer = Color(0xFF261A00),
    tertiary = Color(0xFF4D5F86),
    background = Color(0xFFF7FAF9),
    onBackground = Color(0xFF18211F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF18211F),
    surfaceVariant = Color(0xFFE5ECE9),
    onSurfaceVariant = Color(0xFF3F4946),
    outline = Color(0xFF6F7976),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF74D8C6),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFFC6F0E9),
    secondary = Color(0xFFF4BF5B),
    onSecondary = Color(0xFF402D00),
    secondaryContainer = Color(0xFF5C4300),
    onSecondaryContainer = Color(0xFFFFDF9B),
    tertiary = Color(0xFFB6C6F2),
    background = Color(0xFF101513),
    onBackground = Color(0xFFDDE5E2),
    surface = Color(0xFF161D1B),
    onSurface = Color(0xFFDDE5E2),
    surfaceVariant = Color(0xFF2A3532),
    onSurfaceVariant = Color(0xFFBEC9C5),
    outline = Color(0xFF89938F),
    error = Color(0xFFFFB4AB),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

@Composable
fun ZhuijuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        content = content,
    )
}
