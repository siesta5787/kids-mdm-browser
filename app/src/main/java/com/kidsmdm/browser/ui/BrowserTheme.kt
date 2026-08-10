package com.kidsmdm.browser.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Fixed color scheme (not wallpaper-derived dynamic color) - predictable branding over Material
// You dynamism, per the plan. Placeholder palette; swap for real branding before release.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    secondary = Color(0xFF00897B),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFF80CBC4),
)

/**
 * Strictly follows the system dark-mode setting via [isSystemInDarkTheme] - there is no in-app
 * light/dark toggle anywhere in this app, by design (see the plan's Architecture section).
 */
@Composable
fun BrowserTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
