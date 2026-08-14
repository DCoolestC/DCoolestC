package com.isokovibe.musicplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.isokovibe.musicplayer.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = VibeRed,
    onPrimary = VibeWhite,
    secondary = VibeRedDeep,
    onSecondary = VibeWhite,
    tertiary = VibeRedBright,
    onTertiary = VibeWhite,
    background = VibeBackgroundLight,
    onBackground = VibeBlack,
    surface = VibeSurfaceLight,
    onSurface = VibeBlack,
    surfaceVariant = VibeSurfaceVariantLight,
    onSurfaceVariant = VibeBlack,
    outline = VibeOutlineLight
)

private val DarkColors = darkColorScheme(
    primary = VibeRed,
    onPrimary = VibeWhite,
    secondary = VibeRedBright,
    onSecondary = VibeBlack,
    tertiary = VibeRedBright,
    onTertiary = VibeBlack,
    background = VibeBlack,
    onBackground = VibeWhite,
    surface = VibeSurfaceDark,
    onSurface = VibeWhite,
    surfaceVariant = VibeSurfaceVariantDark,
    onSurfaceVariant = VibeWhite,
    outline = VibeOutlineDark
)

/**
 * iSokoVibe's brand identity is bold red on black/white — deliberately not
 * derived from Material You/dynamic color, so the app always reads as
 * iSokoVibe rather than tinting itself to whatever wallpaper the phone has.
 * Defaults to the brand's dark look regardless of system setting; users can
 * switch to Light or Follow System in Settings.
 */
@Composable
fun IsokoVibeTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = IsokoVibeTypography,
        content = content
    )
}
