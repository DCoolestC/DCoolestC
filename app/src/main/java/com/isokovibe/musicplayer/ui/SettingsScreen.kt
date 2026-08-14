package com.isokovibe.musicplayer.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.isokovibe.musicplayer.BuildConfig
import com.isokovibe.musicplayer.R
import com.isokovibe.musicplayer.data.ColorSkin
import com.isokovibe.musicplayer.data.FontCombination
import com.isokovibe.musicplayer.data.FontSizeScale
import com.isokovibe.musicplayer.data.FontWeightPreference
import com.isokovibe.musicplayer.data.MinTrackDuration
import com.isokovibe.musicplayer.data.ThemeMode
import com.isokovibe.musicplayer.ui.theme.SkinAmber
import com.isokovibe.musicplayer.ui.theme.SkinBlue
import com.isokovibe.musicplayer.ui.theme.SkinGreen
import com.isokovibe.musicplayer.ui.theme.SkinPurple
import com.isokovibe.musicplayer.ui.theme.VibeBlack
import com.isokovibe.musicplayer.ui.theme.VibeRed
import com.isokovibe.musicplayer.ui.theme.VibeRedDeep
import com.isokovibe.musicplayer.ui.theme.VibeSurfaceDark
import com.isokovibe.musicplayer.ui.theme.VibeSurfaceLight
import com.isokovibe.musicplayer.ui.theme.VibeWhite

private val SLEEP_TIMER_OPTIONS = listOf(0, 15, 30, 45, 60)
private val PLAYBACK_SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
private val MINI_PLAYER_COLOR_PRESETS: List<Pair<String, Color?>> = listOf(
    "Default" to null,
    "Black" to VibeBlack,
    "Dark" to VibeSurfaceDark,
    "Red" to VibeRed,
    "Deep red" to VibeRedDeep,
    "White" to VibeWhite,
    "Light" to VibeSurfaceLight
)
private val COLOR_SKIN_SWATCH: Map<ColorSkin, Color> = mapOf(
    ColorSkin.VIBE_RED to VibeRed,
    ColorSkin.OCEAN_BLUE to SkinBlue,
    ColorSkin.EMERALD_GREEN to SkinGreen,
    ColorSkin.ROYAL_PURPLE to SkinPurple,
    ColorSkin.SUNSET_AMBER to SkinAmber
)

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    playbackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    sleepTimerRemainingMs: Long?,
    onSetSleepTimer: (Int) -> Unit,
    colorSkin: ColorSkin,
    onColorSkinChange: (ColorSkin) -> Unit,
    fontCombination: FontCombination,
    onFontCombinationChange: (FontCombination) -> Unit,
    fontSizeScale: FontSizeScale,
    onFontSizeScaleChange: (FontSizeScale) -> Unit,
    fontWeightPreference: FontWeightPreference,
    onFontWeightPreferenceChange: (FontWeightPreference) -> Unit,
    miniPlayerColorArgb: Int?,
    onMiniPlayerColorChange: (Int?) -> Unit,
    minTrackDuration: MinTrackDuration,
    onMinTrackDurationChange: (MinTrackDuration) -> Unit,
    excludeWhatsAppVoiceNotes: Boolean,
    onExcludeWhatsAppVoiceNotesChange: (Boolean) -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current

    LazyColumn(contentPadding = contentPadding) {
        item { SectionHeader("Appearance") }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }
        }

        item { SectionDivider() }
        item { SectionHeader("Color skin") }
        item {
            LazyRow(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ColorSkin.entries.toList()) { skin ->
                    val swatch = COLOR_SKIN_SWATCH.getValue(skin)
                    val isSelected = colorSkin == skin
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { onColorSkinChange(skin) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = VibeWhite)
                            }
                        }
                        Text(skin.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        item { SectionDivider() }
        item { SectionHeader("Fonts") }
        item {
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(
                    "Combination",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FontCombination.entries.forEach { combo ->
                        FilterChip(
                            selected = fontCombination == combo,
                            onClick = { onFontCombinationChange(combo) },
                            label = { Text(combo.label) }
                        )
                    }
                }

                Text(
                    "Size",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FontSizeScale.entries.forEach { scale ->
                        FilterChip(
                            selected = fontSizeScale == scale,
                            onClick = { onFontSizeScaleChange(scale) },
                            label = { Text(scale.label) }
                        )
                    }
                }

                Text(
                    "Weight",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FontWeightPreference.entries.forEach { weight ->
                        FilterChip(
                            selected = fontWeightPreference == weight,
                            onClick = { onFontWeightPreferenceChange(weight) },
                            label = { Text(weight.label) }
                        )
                    }
                }
            }
        }

        item { SectionDivider() }
        item { SectionHeader("Playback speed") }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PLAYBACK_SPEEDS.forEach { speed ->
                    FilterChip(
                        selected = playbackSpeed == speed,
                        onClick = { onPlaybackSpeedChange(speed) },
                        label = { Text("${speed}x") }
                    )
                }
            }
        }

        item { SectionDivider() }
        item { SectionHeader("Sleep timer") }
        item {
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                if (sleepTimerRemainingMs != null) {
                    val minutesLeft = (sleepTimerRemainingMs / 60_000L) + 1
                    Text(
                        "Playback will pause in $minutesLeft min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SLEEP_TIMER_OPTIONS.forEach { minutes ->
                        FilterChip(
                            selected = false,
                            onClick = { onSetSleepTimer(minutes) },
                            label = { Text(if (minutes == 0) "Off" else "${minutes}m") }
                        )
                    }
                }
            }
        }

        item { SectionDivider() }
        item { SectionHeader("Mini player color") }
        item {
            LazyRow(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(MINI_PLAYER_COLOR_PRESETS) { (label, color) ->
                    val isSelected = miniPlayerColorArgb == (color?.toArgb())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color ?: MaterialTheme.colorScheme.surface)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { onMiniPlayerColorChange(color?.toArgb()) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = if (color == null || color == VibeWhite || color == VibeSurfaceLight)
                                        MaterialTheme.colorScheme.primary else VibeWhite
                                )
                            }
                        }
                        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        item { SectionDivider() }
        item { SectionHeader("Library") }
        item {
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(
                    "Skip clips shorter than",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MinTrackDuration.entries.forEach { duration ->
                        FilterChip(
                            selected = minTrackDuration == duration,
                            onClick = { onMinTrackDurationChange(duration) },
                            label = { Text(duration.label) }
                        )
                    }
                }
                SettingsSwitchRow(
                    title = "Skip WhatsApp voice notes",
                    subtitle = "Keeps voice messages/PTT clips out of your library, even if the device tags them as music",
                    checked = excludeWhatsAppVoiceNotes,
                    onCheckedChange = onExcludeWhatsAppVoiceNotesChange,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        item { SectionDivider() }
        item { SectionHeader("Coming soon") }
        items(
            listOf(
                "Equalizer & bass boost",
                "Synced lyrics (.lrc)",
                "Home-screen widgets",
                "Android Auto",
                "Tag editor"
            )
        ) { feature ->
            Text(
                "•  $feature",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        item { SectionDivider() }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_isokovibe_logo),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp))
                )
                Text(
                    "iSokoVibe Music Player",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    modifier = Modifier.padding(top = 16.dp),
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://isokovibe.com.ng"))
                            )
                        } catch (_: ActivityNotFoundException) {
                            // No browser available — nothing sensible to do, so just ignore.
                        }
                    }
                ) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = null)
                    Text(" Visit iSokoVibe.com.ng")
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
}
