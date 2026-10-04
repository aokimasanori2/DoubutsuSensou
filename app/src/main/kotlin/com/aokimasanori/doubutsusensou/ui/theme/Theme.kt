package com.aokimasanori.doubutsusensou.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF496647),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4EBDD),
    onPrimaryContainer = Color(0xFF263F25),
    secondary = Color(0xFF805536),
    secondaryContainer = Color(0xFFF3E2CD),
    onSecondaryContainer = Color(0xFF4F321E),
    background = Color(0xFFFAF8F2),
    surface = Color(0xFFFAF8F2),
    onBackground = Color(0xFF2B3428),
    onSurface = Color(0xFF2B3428),
    onSurfaceVariant = Color(0xFF566052),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB6D0A8),
    onPrimary = Color(0xFF243C21),
    primaryContainer = Color(0xFF354B31),
    onPrimaryContainer = Color(0xFFD8E8CF),
    secondaryContainer = Color(0xFF51402F),
    onSecondaryContainer = Color(0xFFF3E2CD),
    background = Color(0xFF191E17),
    surface = Color(0xFF191E17),
    onBackground = Color(0xFFE1E6DB),
    onSurface = Color(0xFFE1E6DB),
    onSurfaceVariant = Color(0xFFC0C9B8),
)

@Composable
fun DoubutsuSensouTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
