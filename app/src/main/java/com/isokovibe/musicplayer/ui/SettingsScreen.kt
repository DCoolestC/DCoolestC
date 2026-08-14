package com.isokovibe.musicplayer.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.material3.OutlinedTextField
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.isokovibe.musicplayer.BuildConfig
import com.isokovibe.musicplayer.R
import com.isokovibe.musicplayer.data.ThemeMode
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    playbackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    sleepTimerRemainingMs: Long?,
    onSetSleepTimer: (Int) -> Unit,
    animateAlbumArt: Boolean,
    onAnimateAlbumArtChange: (Boolean) -> Unit,
    miniPlayerColorArgb: Int?,
    onMiniPlayerColorChange: (Int?) -> Unit,
    showAds: Boolean,
    onShowAdsChange: (Boolean) -> Unit,
    adUnitId: String,
    onAdUnitIdChange: (String) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    useCustomAds: Boolean,
    onUseCustomAdsChange: (Boolean) -> Unit,
    customAdsFeedUrl: String,
    onCustomAdsFeedUrlChange: (String) -> Unit,
    customAdsCount: Int,
    customAdsError: String?,
    onRefreshCustomAds: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val notificationPermission = if (Build.VERSION.SDK_INT >= 33) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

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
        item { SectionHeader("Now Playing art") }
        item {
            SettingsSwitchRow(
                title = "Spin album art while playing",
                subtitle = "Vinyl-style rotation, like an equalizer for the artwork",
                checked = animateAlbumArt,
                onCheckedChange = onAnimateAlbumArtChange
            )
        }

        item { SectionDivider() }
        item { SectionHeader("Mini player color") }
        item {
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
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
        item { SectionHeader("Your own ads") }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingsSwitchRow(
                    title = "Show my own ads",
                    subtitle = "Local business / affiliate banners, managed from iSokoVibe.com.ng — takes priority over AdMob below when there's at least one",
                    checked = useCustomAds,
                    onCheckedChange = onUseCustomAdsChange
                )
                if (useCustomAds) {
                    var editedFeedUrl by remember(customAdsFeedUrl) { mutableStateOf(customAdsFeedUrl) }
                    OutlinedTextField(
                        value = editedFeedUrl,
                        onValueChange = { editedFeedUrl = it },
                        label = { Text("Ads feed URL") },
                        supportingText = { Text("The iSokoVibe Custom Ads WordPress plugin's endpoint.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onCustomAdsFeedUrlChange(editedFeedUrl.trim()) },
                            enabled = editedFeedUrl.isNotBlank() && editedFeedUrl != customAdsFeedUrl
                        ) { Text("Save") }
                        Button(onClick = onRefreshCustomAds) { Text("Refresh now") }
                    }
                    Text(
                        when {
                            customAdsError != null -> "Couldn't load ads: $customAdsError"
                            customAdsCount == 0 -> "No ads loaded yet — add some from wp-admin → App Ads on iSokoVibe.com.ng, or Refresh above if you just added one."
                            else -> "$customAdsCount ad${if (customAdsCount == 1) "" else "s"} loaded."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (customAdsError != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        item { SectionDivider() }
        item { SectionHeader("AdMob") }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingsSwitchRow(
                    title = "Show ad banner",
                    subtitle = "Sticky banner at the bottom of the app — used when there's no ad of your own to show",
                    checked = showAds,
                    onCheckedChange = onShowAdsChange
                )
                if (showAds) {
                    var editedAdUnitId by remember(adUnitId) { mutableStateOf(adUnitId) }
                    OutlinedTextField(
                        value = editedAdUnitId,
                        onValueChange = { editedAdUnitId = it },
                        label = { Text("AdMob ad unit ID") },
                        supportingText = { Text("Defaults to Google's test unit. Paste your real AdMob ad unit ID here once you have one.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Button(
                        onClick = { onAdUnitIdChange(editedAdUnitId.trim()) },
                        enabled = editedAdUnitId.isNotBlank() && editedAdUnitId != adUnitId,
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("Save ad unit ID") }
                }
            }
        }

        item { SectionDivider() }
        item { SectionHeader("Notifications") }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingsSwitchRow(
                    title = "Notify me about new music",
                    subtitle = "Alerts when new tracks are posted on iSokoVibe.com.ng",
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        onNotificationsEnabledChange(enabled)
                        // notificationPermission is null pre-API 33, where no runtime
                        // permission is needed, so the safe call below is just a no-op there.
                        if (enabled && notificationPermission?.status?.isGranted == false) {
                            notificationPermission?.launchPermissionRequest()
                        }
                    }
                )
                Text(
                    "Needs a one-time setup on iSokoVibe's side to actually send these — ask Claude to wire it up once you've decided how.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
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
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
}
