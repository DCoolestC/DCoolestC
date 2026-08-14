package com.isokovibe.musicplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.isokovibe.musicplayer.R

// Bundled Roboto (Apache 2.0) so the app always renders in Roboto — not
// whatever default sans-serif an OEM skin (Samsung/Xiaomi/etc.) substitutes
// for the system font.
val RobotoFamily = FontFamily(
    Font(R.font.roboto_light, FontWeight.Light),
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_bold, FontWeight.Bold),
    Font(R.font.roboto_black, FontWeight.Black)
)

// Start from Material3's default type scale (sizes/line-heights/spacing)
// and swap every role onto Roboto, so buttons/chips/labels get it too —
// not just the handful of styles this app sets explicitly.
private val baseline = Typography()

// Heavier weights on the display/title styles to echo the bold, chunky
// poster-style wordmark used in iSokoVibe's brand art.
val IsokoVibeTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = RobotoFamily),
    displayMedium = baseline.displayMedium.copy(fontFamily = RobotoFamily),
    displaySmall = baseline.displaySmall.copy(fontFamily = RobotoFamily),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = RobotoFamily),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = RobotoFamily),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = RobotoFamily),
    titleLarge = baseline.titleLarge.copy(fontFamily = RobotoFamily, fontWeight = FontWeight.Black, fontSize = 26.sp),
    titleMedium = baseline.titleMedium.copy(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp
    ),
    titleSmall = baseline.titleSmall.copy(fontFamily = RobotoFamily),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = RobotoFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = RobotoFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = RobotoFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = RobotoFamily),
    labelMedium = baseline.labelMedium.copy(fontFamily = RobotoFamily),
    labelSmall = baseline.labelSmall.copy(fontFamily = RobotoFamily, fontWeight = FontWeight.Bold)
)
