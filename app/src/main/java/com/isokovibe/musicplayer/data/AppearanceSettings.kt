package com.isokovibe.musicplayer.data

/** Which bundled font family(s) drive the UI's text. */
enum class FontCombination(val label: String) {
    ROBOTO("Roboto"),
    ROBOTO_OPEN_SANS("Roboto + Open Sans"),
    OPEN_SANS("Open Sans")
}

/** A global multiplier applied on top of every text style's size. */
enum class FontSizeScale(val label: String, val multiplier: Float) {
    SMALL("Small", 0.9f),
    DEFAULT("Default", 1f),
    LARGE("Large", 1.15f)
}

/** Shifts the bold-title/light-detail weight hierarchy up or down a notch. */
enum class FontWeightPreference(val label: String) {
    LIGHT("Light"),
    DEFAULT("Default"),
    BOLD("Bold")
}

/** Accent color skins. Vibe Red is the brand default; the other four are
 *  standard alternates so users aren't stuck with one look. */
enum class ColorSkin(val label: String) {
    VIBE_RED("Vibe Red"),
    OCEAN_BLUE("Ocean Blue"),
    EMERALD_GREEN("Emerald Green"),
    ROYAL_PURPLE("Royal Purple"),
    SUNSET_AMBER("Sunset Amber")
}

/** Minimum track length the library scanner will import. Anything shorter
 *  (voice memos, message clips, stingers) is skipped. */
enum class MinTrackDuration(val label: String, val seconds: Int) {
    OFF("Off", 0),
    SEC_15("15s", 15),
    SEC_30("30s", 30),
    SEC_45("45s", 45),
    SEC_60("60s", 60),
    SEC_90("90s", 90)
}
