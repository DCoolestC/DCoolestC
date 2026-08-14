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
    primaryContainer = VibeContainerLight,
    onPrimaryContainer = VibeOnContainerLight,
    inversePrimary = VibeRedBright,
    secondary = VibeRedDeep,
    onSecondary = VibeWhite,
    secondaryContainer = VibeContainerLight,
    onSecondaryContainer = VibeOnContainerLight,
    tertiary = VibeRedBright,
    onTertiary = VibeWhite,
    tertiaryContainer = VibeContainerLight,
    onTertiaryContainer = VibeOnContainerLight,
    background = VibeBackgroundLight,
    onBackground = VibeBlack,
    surface = VibeSurfaceLight,
    onSurface = VibeBlack,
    surfaceVariant = VibeSurfaceVariantLight,
    onSurfaceVariant = VibeBlack,
    surfaceTint = VibeRed,
    outline = VibeOutlineLight,
    outlineVariant = VibeOutlineVariantLight,
    inverseSurface = VibeBlack,
    inverseOnSurface = VibeWhite
)

private val DarkColors = darkColorScheme(
    primary = VibeRed,
    onPrimary = VibeWhite,
    primaryContainer = VibeContainerDark,
    onPrimaryContainer = VibeOnContainerDark,
    inversePrimary = VibeRedDeep,
    secondary = VibeRedBright,
    onSecondary = VibeBlack,
    secondaryContainer = VibeContainerDark,
    onSecondaryContainer = VibeOnContainerDark,
    tertiary = VibeRedBright,
    onTertiary = VibeBlack,
    tertiaryContainer = VibeContainerDark,
    onTertiaryContainer = VibeOnContainerDark,
    background = VibeBlack,
    onBackground = VibeWhite,
    surface = VibeSurfaceDark,
    onSurface = VibeWhite,
    surfaceVariant = VibeSurfaceVariantDark,
    onSurfaceVariant = VibeWhite,
    surfaceTint = VibeRed,
    outline = VibeOutlineDark,
    outlineVariant = VibeOutlineVariantDark,
    inverseSurface = VibeWhite,
    inverseOnSurface = VibeBlack
)

/**
 * iSokoVibe's brand identity is bold red on black/white — deliberately not
 * derived from Material You/dynamic color, so the app always reads as
 * iSokoVibe rather than tinting itself to whatever wallpaper the phone has.
 * Every ColorScheme role is set explicitly (not just primary/surface) so
 * Material3 never falls back to its purple baseline palette for the roles
 * that get left unset — that's what was showing up as a lilac tint on
 * selected nav items and chips.
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
