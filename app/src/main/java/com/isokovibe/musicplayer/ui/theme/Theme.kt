package com.isokovibe.musicplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.isokovibe.musicplayer.data.ColorSkin
import com.isokovibe.musicplayer.data.FontCombination
import com.isokovibe.musicplayer.data.FontSizeScale
import com.isokovibe.musicplayer.data.FontWeightPreference
import com.isokovibe.musicplayer.data.ThemeMode

private data class SkinPalette(
    val accent: Color,
    val accentBright: Color,
    val accentDeep: Color,
    val containerLight: Color,
    val onContainerLight: Color,
    val containerDark: Color,
    val onContainerDark: Color
)

private val skinPalettes = mapOf(
    ColorSkin.VIBE_RED to SkinPalette(
        VibeRed, VibeRedBright, VibeRedDeep,
        VibeContainerLight, VibeOnContainerLight, VibeContainerDark, VibeOnContainerDark
    ),
    ColorSkin.OCEAN_BLUE to SkinPalette(
        SkinBlue, SkinBlueBright, SkinBlueDeep,
        SkinBlueContainerLight, SkinBlueOnContainerLight, SkinBlueContainerDark, SkinBlueOnContainerDark
    ),
    ColorSkin.EMERALD_GREEN to SkinPalette(
        SkinGreen, SkinGreenBright, SkinGreenDeep,
        SkinGreenContainerLight, SkinGreenOnContainerLight, SkinGreenContainerDark, SkinGreenOnContainerDark
    ),
    ColorSkin.ROYAL_PURPLE to SkinPalette(
        SkinPurple, SkinPurpleBright, SkinPurpleDeep,
        SkinPurpleContainerLight, SkinPurpleOnContainerLight, SkinPurpleContainerDark, SkinPurpleOnContainerDark
    ),
    ColorSkin.SUNSET_AMBER to SkinPalette(
        SkinAmber, SkinAmberBright, SkinAmberDeep,
        SkinAmberContainerLight, SkinAmberOnContainerLight, SkinAmberContainerDark, SkinAmberOnContainerDark
    )
)

/**
 * Builds a full [ColorScheme] for [skin] — every role set explicitly, the
 * same way the original single-skin Vibe Red scheme was, so Material3 never
 * falls back to its purple baseline palette for a role a skin leaves unset.
 */
private fun buildColorScheme(skin: ColorSkin, dark: Boolean): ColorScheme {
    val p = skinPalettes.getValue(skin)
    return if (dark) {
        darkColorScheme(
            primary = p.accent,
            onPrimary = VibeWhite,
            primaryContainer = p.containerDark,
            onPrimaryContainer = p.onContainerDark,
            inversePrimary = p.accentDeep,
            secondary = p.accentBright,
            onSecondary = VibeBlack,
            secondaryContainer = p.containerDark,
            onSecondaryContainer = p.onContainerDark,
            tertiary = p.accentBright,
            onTertiary = VibeBlack,
            tertiaryContainer = p.containerDark,
            onTertiaryContainer = p.onContainerDark,
            background = VibeBlack,
            onBackground = VibeWhite,
            surface = VibeSurfaceDark,
            onSurface = VibeWhite,
            surfaceVariant = VibeSurfaceVariantDark,
            onSurfaceVariant = VibeWhite,
            surfaceTint = p.accent,
            outline = VibeOutlineDark,
            outlineVariant = VibeOutlineVariantDark,
            inverseSurface = VibeWhite,
            inverseOnSurface = VibeBlack
        )
    } else {
        lightColorScheme(
            primary = p.accent,
            onPrimary = VibeWhite,
            primaryContainer = p.containerLight,
            onPrimaryContainer = p.onContainerLight,
            inversePrimary = p.accentBright,
            secondary = p.accentDeep,
            onSecondary = VibeWhite,
            secondaryContainer = p.containerLight,
            onSecondaryContainer = p.onContainerLight,
            tertiary = p.accentBright,
            onTertiary = VibeWhite,
            tertiaryContainer = p.containerLight,
            onTertiaryContainer = p.onContainerLight,
            background = VibeBackgroundLight,
            onBackground = VibeBlack,
            surface = VibeSurfaceLight,
            onSurface = VibeBlack,
            surfaceVariant = VibeSurfaceVariantLight,
            onSurfaceVariant = VibeBlack,
            surfaceTint = p.accent,
            outline = VibeOutlineLight,
            outlineVariant = VibeOutlineVariantLight,
            inverseSurface = VibeBlack,
            inverseOnSurface = VibeWhite
        )
    }
}

/**
 * iSokoVibe's brand identity is bold red on black/white by default —
 * deliberately not derived from Material You/dynamic color — but Settings
 * lets users swap the accent to one of four other standard skins, pick a
 * font combination/size/weight, all while keeping this same explicit-role
 * construction so nothing ever falls back to Material3's purple baseline.
 * Defaults to the brand's dark look regardless of system setting; users can
 * switch to Light or Follow System in Settings.
 */
@Composable
fun IsokoVibeTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    colorSkin: ColorSkin = ColorSkin.VIBE_RED,
    fontCombination: FontCombination = FontCombination.ROBOTO_OPEN_SANS,
    fontSizeScale: FontSizeScale = FontSizeScale.DEFAULT,
    fontWeightPreference: FontWeightPreference = FontWeightPreference.DEFAULT,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = buildColorScheme(colorSkin, darkTheme)
    val typography = isokoVibeTypography(fontCombination, fontSizeScale, fontWeightPreference)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
