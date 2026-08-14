package com.isokovibe.musicplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = VibePrimary,
    secondary = VibeAccent,
    background = VibeBackground,
    surface = VibeBackground
)

private val DarkColors = darkColorScheme(
    primary = VibePrimaryDark,
    secondary = VibeAccent,
    background = VibeBackgroundDark,
    surface = VibeSurfaceDark,
    onBackground = VibeOnDark,
    onSurface = VibeOnDark
)

@Composable
fun IsokoVibeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = IsokoVibeTypography,
        content = content
    )
}
