package com.isokovibe.musicplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Heavier weights on the display/title styles to echo the bold, chunky
// poster-style wordmark used in iSokoVibe's brand art.
val IsokoVibeTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp)
)
