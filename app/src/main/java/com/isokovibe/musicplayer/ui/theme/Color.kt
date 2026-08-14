package com.isokovibe.musicplayer.ui.theme

import androidx.compose.ui.graphics.Color

// Sampled directly from the iSokoVibe.com.ng logo and brand art —
// bold red on black, the way the site presents itself. This is the
// default color skin ("Vibe Red"); Settings offers four more below.
val VibeRed = Color(0xFFD50000)       // circle in the "iSo" mark
val VibeRedBright = Color(0xFFFF0008) // headphone band / notes accent
val VibeRedDeep = Color(0xFFAA0000)   // promo-art background red
val VibeBlack = Color(0xFF000000)     // icon background
val VibeWhite = Color(0xFFFFFFFF)

val VibeSurfaceDark = Color(0xFF161616)
val VibeSurfaceVariantDark = Color(0xFF262626)
val VibeOutlineDark = Color(0xFF3D3D3D)
val VibeOutlineVariantDark = Color(0xFF4D4D4D)

val VibeBackgroundLight = Color(0xFFFFFFFF)
val VibeSurfaceLight = Color(0xFFF7F5F5)
val VibeSurfaceVariantLight = Color(0xFFEDE3E3)
val VibeOutlineLight = Color(0xFFD8CACA)
val VibeOutlineVariantLight = Color(0xFFE3D5D5)

// "Container" tones for every ColorScheme role — Material3 auto-derives
// these from a purple baseline palette when they're left unset, which is
// what was leaking through as a lilac tint on selected nav items and chips.
// Keeping every skin's containers hand-picked here closes that gap for all
// five, not just the default.
val VibeContainerLight = Color(0xFFFFDAD4)     // soft red tint
val VibeOnContainerLight = Color(0xFF410001)
val VibeContainerDark = Color(0xFF930000)      // deep red tint
val VibeOnContainerDark = Color(0xFFFFDAD4)

// Ocean Blue skin
val SkinBlue = Color(0xFF0066FF)
val SkinBlueBright = Color(0xFF4D94FF)
val SkinBlueDeep = Color(0xFF003C99)
val SkinBlueContainerLight = Color(0xFFD6E4FF)
val SkinBlueOnContainerLight = Color(0xFF001A41)
val SkinBlueContainerDark = Color(0xFF00429E)
val SkinBlueOnContainerDark = Color(0xFFD6E4FF)

// Emerald Green skin
val SkinGreen = Color(0xFF00A651)
val SkinGreenBright = Color(0xFF34D172)
val SkinGreenDeep = Color(0xFF00703A)
val SkinGreenContainerLight = Color(0xFFB6F2C9)
val SkinGreenOnContainerLight = Color(0xFF002109)
val SkinGreenContainerDark = Color(0xFF00522A)
val SkinGreenOnContainerDark = Color(0xFFB6F2C9)

// Royal Purple skin
val SkinPurple = Color(0xFF7C3AED)
val SkinPurpleBright = Color(0xFFA875FF)
val SkinPurpleDeep = Color(0xFF4C1D95)
val SkinPurpleContainerLight = Color(0xFFEADDFF)
val SkinPurpleOnContainerLight = Color(0xFF22005D)
val SkinPurpleContainerDark = Color(0xFF4F2CA8)
val SkinPurpleOnContainerDark = Color(0xFFEADDFF)

// Sunset Amber skin
val SkinAmber = Color(0xFFFF8F00)
val SkinAmberBright = Color(0xFFFFB74D)
val SkinAmberDeep = Color(0xFFB35F00)
val SkinAmberContainerLight = Color(0xFFFFDDB0)
val SkinAmberOnContainerLight = Color(0xFF2A1700)
val SkinAmberContainerDark = Color(0xFF8A5300)
val SkinAmberOnContainerDark = Color(0xFFFFDDB0)
