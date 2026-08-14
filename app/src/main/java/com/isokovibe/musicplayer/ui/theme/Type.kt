package com.isokovibe.musicplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.isokovibe.musicplayer.R
import com.isokovibe.musicplayer.data.FontCombination
import com.isokovibe.musicplayer.data.FontSizeScale
import com.isokovibe.musicplayer.data.FontWeightPreference

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

// Bundled Open Sans (Apache 2.0) — the Light ("book") cut is the workhorse
// here, meant for smaller secondary text where Roboto's light weight reads
// a bit heavy. Regular/SemiBold/Bold are included too so anything that asks
// for a heavier weight (titles, when Open Sans is used throughout) still
// resolves to a real cut instead of a synthesized fake-bold.
val OpenSansFamily = FontFamily(
    Font(R.font.open_sans_light, FontWeight.Light),
    Font(R.font.open_sans_regular, FontWeight.Normal),
    Font(R.font.open_sans_semibold, FontWeight.SemiBold),
    Font(R.font.open_sans_bold, FontWeight.Bold)
)

// Start from Material3's default type scale (line-heights/spacing) and
// swap every role onto the chosen font(s).
private val baseline = Typography()

private data class WeightProfile(
    val titleLarge: FontWeight,
    val titleMedium: FontWeight,
    val bodyLarge: FontWeight,
    val bodyMedium: FontWeight,
    val bodySmall: FontWeight,
    val labelSmall: FontWeight
)

// Three weight profiles, all keeping the same idea — primary text (titles,
// song/playlist names) bold, secondary text (artist, album, counts,
// subtitles) small and light — just shifted a notch lighter or heavier
// depending on what Settings > Fonts > Weight is set to.
private val weightProfiles = mapOf(
    FontWeightPreference.LIGHT to WeightProfile(
        titleLarge = FontWeight.Bold,
        titleMedium = FontWeight.Bold,
        bodyLarge = FontWeight.Medium,
        bodyMedium = FontWeight.Light,
        bodySmall = FontWeight.Light,
        labelSmall = FontWeight.Medium
    ),
    FontWeightPreference.DEFAULT to WeightProfile(
        titleLarge = FontWeight.Black,
        titleMedium = FontWeight.ExtraBold,
        bodyLarge = FontWeight.Bold,
        bodyMedium = FontWeight.Normal,
        bodySmall = FontWeight.Normal,
        labelSmall = FontWeight.Bold
    ),
    FontWeightPreference.BOLD to WeightProfile(
        titleLarge = FontWeight.Black,
        titleMedium = FontWeight.Black,
        bodyLarge = FontWeight.Black,
        bodyMedium = FontWeight.Medium,
        bodySmall = FontWeight.Medium,
        labelSmall = FontWeight.Black
    )
)

/**
 * Builds the app's [Typography] from three independent Settings choices:
 * which font(s) to use, an overall size multiplier, and a weight profile.
 * Titles always use the "heavier" font of the combination; body/label text
 * uses the "lighter" one — so picking Roboto + Open Sans puts Roboto on
 * headers and Open Sans' 300 book weight on everything smaller.
 */
fun isokoVibeTypography(
    combination: FontCombination = FontCombination.ROBOTO_OPEN_SANS,
    sizeScale: FontSizeScale = FontSizeScale.DEFAULT,
    weightPreference: FontWeightPreference = FontWeightPreference.DEFAULT
): Typography {
    val titleFamily = if (combination == FontCombination.OPEN_SANS) OpenSansFamily else RobotoFamily
    val bodyFamily = if (combination == FontCombination.ROBOTO) RobotoFamily else OpenSansFamily
    val w = weightProfiles.getValue(weightPreference)
    val s = sizeScale.multiplier

    return Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = titleFamily),
        displayMedium = baseline.displayMedium.copy(fontFamily = titleFamily),
        displaySmall = baseline.displaySmall.copy(fontFamily = titleFamily),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = titleFamily),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = titleFamily),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = titleFamily),
        titleLarge = baseline.titleLarge.copy(fontFamily = titleFamily, fontWeight = w.titleLarge, fontSize = 24.sp * s),
        titleMedium = baseline.titleMedium.copy(fontFamily = titleFamily, fontWeight = w.titleMedium, fontSize = 17.sp * s),
        titleSmall = baseline.titleSmall.copy(fontFamily = titleFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp * s),
        // Primary text — song titles, playlist names, section headers' content.
        bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFamily, fontWeight = w.bodyLarge, fontSize = 15.sp * s),
        // Secondary text — artist/album, subtitles, descriptions. Small and light.
        bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFamily, fontWeight = w.bodyMedium, fontSize = 13.sp * s),
        bodySmall = baseline.bodySmall.copy(fontFamily = bodyFamily, fontWeight = w.bodySmall, fontSize = 11.sp * s),
        labelLarge = baseline.labelLarge.copy(fontFamily = bodyFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp * s),
        labelMedium = baseline.labelMedium.copy(fontFamily = bodyFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp * s),
        labelSmall = baseline.labelSmall.copy(fontFamily = bodyFamily, fontWeight = w.labelSmall, fontSize = 10.sp * s)
    )
}
