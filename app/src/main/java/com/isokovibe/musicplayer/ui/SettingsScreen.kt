package com.isokovibe.musicplayer.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.isokovibe.musicplayer.BuildConfig
import com.isokovibe.musicplayer.R
import com.isokovibe.musicplayer.data.ThemeMode

private val SLEEP_TIMER_OPTIONS = listOf(0, 15, 30, 45, 60)
private val PLAYBACK_SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    playbackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    sleepTimerRemainingMs: Long?,
    onSetSleepTimer: (Int) -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current

    LazyColumn(contentPadding = contentPadding) {
        item { SectionHeader("Appearance") }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
        item { SectionHeader("Playback speed") }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
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
        item { SectionHeader("Coming soon") }
        items(
            listOf(
                "Equalizer & bass boost",
                "Synced lyrics (.lrc)",
                "Home-screen widgets",
                "Android Auto",
                "Tag editor",
                "Listening stats"
            )
        ) { feature ->
            Text(
                "•  $feature",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        item { SectionDivider() }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
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
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
}
