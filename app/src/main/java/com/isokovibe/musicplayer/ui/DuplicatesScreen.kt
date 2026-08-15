package com.isokovibe.musicplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isokovibe.musicplayer.data.DuplicateGroup
import com.isokovibe.musicplayer.data.Song

/**
 * Lists tracks that appear more than once, grouped by title + artist, with
 * each copy's folder and duration shown so the user can tell a genuine
 * re-download from a legitimately different version (live cut, radio edit).
 *
 * Deliberately read-only for now: deleting means writing to the user's own
 * music files, which needs scoped-storage delete requests and is
 * irreversible if it gets it wrong. That lands alongside the tag editor,
 * where the same file-write machinery is being built and can be tested as
 * one piece rather than bolted on here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(
    duplicates: List<DuplicateGroup>,
    onSongClick: (Song) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicates") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (duplicates.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No duplicates found — every track in your library has a distinct title and artist.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                val copies = duplicates.sumOf { it.songs.size - 1 }
                Text(
                    "${duplicates.size} tracks appear more than once — " +
                        "$copies extra ${if (copies == 1) "copy" else "copies"}. " +
                        "Tap any entry to play it and check which one you want to keep.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                )
            }
            items(duplicates, key = { it.title + it.artist }) { group ->
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        "${group.title} · ${group.artist}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    group.songs.forEach { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSongClick(song) }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    song.folderPath.ifEmpty { "Unknown folder" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    song.album,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                formatDuration(song.durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
