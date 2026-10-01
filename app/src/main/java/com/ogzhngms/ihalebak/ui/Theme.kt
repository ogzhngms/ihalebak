package com.ogzhngms.ihalebak.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Navy and white for a sober, official look, with amber kept for "soon" and red for cancelled.
private val Light = lightColorScheme(
    primary = Color(0xFF1F3A68),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6F5),
    onPrimaryContainer = Color(0xFF0B1F3F),
    secondary = Color(0xFF3B6EA8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EBF6),
    onSecondaryContainer = Color(0xFF14325A),
    tertiary = Color(0xFF9A5B00),
    tertiaryContainer = Color(0xFFFFE8C2),
    onTertiaryContainer = Color(0xFF4A2B00),
    background = Color(0xFFF5F7FB),
    onBackground = Color(0xFF16202E),
    surface = Color.White,
    onSurface = Color(0xFF16202E),
    surfaceVariant = Color(0xFFE9EEF5),
    onSurfaceVariant = Color(0xFF4F5B6B),
    surfaceContainer = Color(0xFFEFF3F8),
    surfaceContainerHigh = Color(0xFFE9EEF5),
    outline = Color(0xFFB8C3D1),
    outlineVariant = Color(0xFFD7DEE8),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFADBD8),
    onErrorContainer = Color(0xFF5F1410),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA9C4F0),
    onPrimary = Color(0xFF0B1F3F),
    primaryContainer = Color(0xFF243F6B),
    onPrimaryContainer = Color(0xFFDCE6F5),
    secondary = Color(0xFF9DBCE3),
    onSecondary = Color(0xFF0E2A4D),
    secondaryContainer = Color(0xFF203752),
    onSecondaryContainer = Color(0xFFDCE6F5),
    tertiary = Color(0xFFF5C26B),
    tertiaryContainer = Color(0xFF4F3300),
    onTertiaryContainer = Color(0xFFFFE8C2),
    background = Color(0xFF0E1622),
    onBackground = Color(0xFFE3E9F1),
    surface = Color(0xFF142030),
    onSurface = Color(0xFFE3E9F1),
    surfaceVariant = Color(0xFF1E2C3F),
    onSurfaceVariant = Color(0xFFA9B6C7),
    surfaceContainer = Color(0xFF17243A),
    surfaceContainerHigh = Color(0xFF1E2C3F),
    outline = Color(0xFF4A5A70),
    outlineVariant = Color(0xFF2C3B50),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF5F1410),
    onErrorContainer = Color(0xFFFADBD8),
)

@Composable
fun IhaleBakTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
