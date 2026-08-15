package com.isokovibe.musicplayer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isokovibe.musicplayer.data.RemoteAppVersion

/**
 * Prompts the user to install a newer build from the website.
 *
 * The app is distributed as an APK rather than through the Play Store, so
 * there's no store to hand off to — "Update" opens the download page in the
 * browser and the user installs over the top.
 *
 * A required update deliberately has no dismiss button and cannot be
 * cancelled by tapping outside, which is the entire point of the flag; it's
 * also why the plugin's admin page warns against using it casually, since
 * anyone who can't download right then is locked out of their own music.
 */
@Composable
fun UpdatePromptDialog(
    version: RemoteAppVersion,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!version.required) onDismiss() },
        title = {
            Text(
                if (version.required) "Update required" else "Update available"
            )
        },
        text = {
            Column {
                Text(
                    if (version.versionName.isNotBlank()) {
                        "Version ${version.versionName} is available."
                    } else {
                        "A newer version of iSokoVibe Music Player is available."
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                if (version.message.isNotBlank()) {
                    Text(
                        version.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Text(
                    "Opens iSokoVibe.com.ng in your browser. Install it over the " +
                        "top — your playlists and settings are kept.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate) { Text("Update") }
        },
        dismissButton = {
            if (!version.required) {
                TextButton(onClick = onDismiss) { Text("Later") }
            }
        }
    )
}
