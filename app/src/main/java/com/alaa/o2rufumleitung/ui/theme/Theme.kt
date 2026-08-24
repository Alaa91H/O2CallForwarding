package com.alaa.o2rufumleitung.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = SeedBlue,
    background = Color(0xFFF6F6F8),
    surface = Color(0xFFF6F6F8)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DC0FF),
    background = Color(0xFF19191C),
    surface = Color(0xFF19191C)
)

/**
 * App theme. Follows the system light/dark setting by default, and uses
 * Material You dynamic color on Android 12+ (falling back to the static
 * [SeedBlue] palette on older versions or if turned off).
 */
@Composable
fun O2RufumleitungTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
