package com.isokovibe.musicplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isokovibe.musicplayer.data.Playlist
import com.isokovibe.musicplayer.data.SmartPlaylist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    smartPlaylistCounts: Map<SmartPlaylist, Int>,
    onSmartPlaylistClick: (SmartPlaylist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    // Without this, the FAB positions itself relative to this Scaffold's own
    // bounds — which extend all the way to the bottom of the screen, right
    // behind the outer bottom nav bar/mini player/ad banner. That's what was
    // making the + button unreachable/invisible.
    Scaffold(
        modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New playlist")
            }
        }
    ) { fabPadding ->
        val combinedPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = fabPadding.calculateBottomPadding()
        )
        // Smart playlists are always present — they're derived, not created —
        // so this list is never truly empty; the "no playlists yet" hint is
        // now scoped to the user's own playlists section.
        LazyColumn(contentPadding = combinedPadding) {
            item {
                Text(
                    "Smart playlists",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                )
            }
            items(SmartPlaylist.entries.toList(), key = { it.name }) { smart ->
                val count = smartPlaylistCounts[smart] ?: 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSmartPlaylistClick(smart) }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = if (count > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(smart.label, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (count == 0) smart.description else "$count · ${smart.description}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }
            item {
                Text(
                    "Your playlists",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                )
            }

            if (playlists.isEmpty()) {
                item {
                    Text(
                        "Tap + to create one, or add songs to a playlist from your library.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(playlists, key = { it.id }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaylistClick(playlist) }
                            .padding(horizontal = 8.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(playlist.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${playlist.songIds.size} songs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreatePlaylist(name.trim())
                            showCreateDialog = false
                        }
                    },
                    enabled = name.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }
}
