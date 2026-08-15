package com.isokovibe.musicplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.isokovibe.musicplayer.data.StoredAudioEffects
import com.isokovibe.musicplayer.playback.EqualizerCapabilities

private val REVERB_PRESETS = listOf(
    "Off", "Small room", "Medium room", "Large room", "Medium hall", "Large hall", "Plate"
)

/**
 * Equalizer and audio effects.
 *
 * The band sliders are generated from what the device reports rather than a
 * fixed count — Android makes no guarantee here, and most hardware exposes
 * five bands where desktop equalizers would show ten. Presenting ten
 * sliders on a five-band device would mean five of them silently doing
 * nothing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    settings: StoredAudioEffects,
    capabilities: EqualizerCapabilities,
    onSettingsChange: (StoredAudioEffects) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!capabilities.available) {
            Box(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "This device doesn't expose an equalizer to apps, or another app is " +
                        "currently holding it. Playback works normally — only the effects " +
                        "below are unavailable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Equalizer", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${capabilities.bandCount} bands on this device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.equalizerEnabled,
                        onCheckedChange = { onSettingsChange(settings.copy(equalizerEnabled = it)) }
                    )
                }
            }

            if (capabilities.presetNames.isNotEmpty()) {
                item {
                    Text(
                        "Presets",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = settings.presetIndex < 0,
                            onClick = { onSettingsChange(settings.copy(presetIndex = -1)) },
                            label = { Text("Custom") },
                            enabled = settings.equalizerEnabled
                        )
                    }
                }
                items(capabilities.presetNames.size) { index ->
                    val name = capabilities.presetNames[index]
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                        FilterChip(
                            selected = settings.presetIndex == index,
                            onClick = { onSettingsChange(settings.copy(presetIndex = index)) },
                            label = { Text(name) },
                            enabled = settings.equalizerEnabled
                        )
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }
            item {
                Text(
                    if (settings.presetIndex >= 0) {
                        "Bands (switch to Custom to adjust)"
                    } else {
                        "Bands"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
            items(capabilities.bandCount) { band ->
                val level = settings.bandLevels.getOrElse(band) { 0 }
                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatFrequency(capabilities.centerFrequencies.getOrElse(band) { 0 }),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "${if (level > 0) "+" else ""}${level / 100} dB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = level.toFloat(),
                        onValueChange = { newLevel ->
                            // Editing a band means you're no longer on a preset.
                            val levels = MutableList(capabilities.bandCount) {
                                settings.bandLevels.getOrElse(it) { 0 }
                            }
                            levels[band] = newLevel.toInt()
                            onSettingsChange(
                                settings.copy(bandLevels = levels, presetIndex = -1)
                            )
                        },
                        valueRange = capabilities.minLevel.toFloat()..capabilities.maxLevel.toFloat(),
                        enabled = settings.equalizerEnabled
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }
            item {
                EffectSlider(
                    label = "Bass boost",
                    value = settings.bassBoost,
                    onValueChange = { onSettingsChange(settings.copy(bassBoost = it)) }
                )
            }
            item {
                EffectSlider(
                    label = "Virtualizer (stereo width)",
                    value = settings.virtualizer,
                    onValueChange = { onSettingsChange(settings.copy(virtualizer = it)) }
                )
            }
            item {
                EffectSlider(
                    label = "Volume boost",
                    value = settings.loudnessGain,
                    maxValue = 2000,
                    unitSuffix = { "${it / 100} dB" },
                    onValueChange = { onSettingsChange(settings.copy(loudnessGain = it)) }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }
            item {
                Text(
                    "Reverb",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
            items(REVERB_PRESETS.size) { index ->
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                    FilterChip(
                        selected = settings.reverbPreset == index,
                        onClick = { onSettingsChange(settings.copy(reverbPreset = index)) },
                        label = { Text(REVERB_PRESETS[index]) }
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Skip silence", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Automatically shortens silent passages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.skipSilence,
                        onCheckedChange = { onSettingsChange(settings.copy(skipSilence = it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EffectSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    maxValue: Int = 1000,
    unitSuffix: (Int) -> String = { "${it / 10}%" }
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (value == 0) "Off" else unitSuffix(value),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..maxValue.toFloat()
        )
    }
}

private fun formatFrequency(hz: Int): String =
    if (hz >= 1000) "${hz / 1000} kHz" else "$hz Hz"
