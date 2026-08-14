package com.isokovibe.musicplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isokovibe.musicplayer.LibraryUiState
import com.isokovibe.musicplayer.R
import com.isokovibe.musicplayer.data.Playlist
import com.isokovibe.musicplayer.data.SortOption
import com.isokovibe.musicplayer.data.Song
import com.isokovibe.musicplayer.ui.components.AddToPlaylistDialog
import com.isokovibe.musicplayer.ui.components.AlbumArt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    searchQuery: String,
    sortOption: SortOption,
    showFavoritesOnly: Boolean,
    favorites: Set<Long>,
    playlists: List<Playlist>,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onToggleFavoritesOnly: (Boolean) -> Unit,
    onSongClick: (Song) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (Playlist, Song) -> Unit,
    onCreatePlaylistAndAdd: (String, Song) -> Unit,
    onRequestPermission: () -> Unit,
    onRescan: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    var songForPlaylistPicker by remember { mutableStateOf<Song?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        if (!state.permissionRequired) {
            LibraryControls(
                searchQuery = searchQuery,
                sortOption = sortOption,
                showFavoritesOnly = showFavoritesOnly,
                onSearchQueryChange = onSearchQueryChange,
                onSortOptionChange = onSortOptionChange,
                onToggleFavoritesOnly = onToggleFavoritesOnly,
                onRescan = onRescan
            )
        }

        // Only the bottom inset is left for the scrolling list — the top inset
        // was already applied to this Column, above the search bar/chips.
        val listPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())

        when {
            state.permissionRequired -> PermissionRequiredState(onRequestPermission, Modifier.weight(1f))
            state.isLoading -> LoadingState(Modifier.weight(1f))
            state.songs.isEmpty() && searchQuery.isBlank() && !showFavoritesOnly ->
                EmptyState(Modifier.weight(1f))
            state.songs.isEmpty() -> NoResultsState(Modifier.weight(1f))
            else -> SongList(
                songs = state.songs,
                favorites = favorites,
                onSongClick = onSongClick,
                onToggleFavorite = onToggleFavorite,
                onAddToPlaylistClick = { songForPlaylistPicker = it },
                contentPadding = listPadding,
                modifier = Modifier.weight(1f)
            )
        }
    }

    songForPlaylistPicker?.let { song ->
        AddToPlaylistDialog(
            playlists = playlists,
            onAddToExisting = { playlist ->
                onAddToPlaylist(playlist, song)
                songForPlaylistPicker = null
            },
            onCreateAndAdd = { name ->
                onCreatePlaylistAndAdd(name, song)
                songForPlaylistPicker = null
            },
            onDismiss = { songForPlaylistPicker = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryControls(
    searchQuery: String,
    sortOption: SortOption,
    showFavoritesOnly: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onToggleFavoritesOnly: (Boolean) -> Unit,
    onRescan: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search your library", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = true
            )
            IconButton(onClick = onRescan) {
                Icon(Icons.Filled.Refresh, contentDescription = "Scan library")
            }
        }
        LazyRow(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = showFavoritesOnly,
                    onClick = { onToggleFavoritesOnly(!showFavoritesOnly) },
                    label = { Text("Favorites") },
                    leadingIcon = {
                        Icon(
                            if (showFavoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null
                        )
                    }
                )
            }
            items(SortOption.entries.toList()) { option ->
                FilterChip(
                    selected = sortOption == option,
                    onClick = { onSortOptionChange(option) },
                    label = { Text(option.label) }
                )
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PermissionRequiredState(onRequestPermission: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.permission_rationale),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = onRequestPermission, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.grant_permission))
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.empty_library_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.empty_library_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun NoResultsState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = "No songs match your search or filters.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    favorites: Set<Long>,
    onSongClick: (Song) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(songs, key = { it.id }) { song ->
            SongRow(
                song = song,
                isFavorite = song.id in favorites,
                onClick = { onSongClick(song) },
                onToggleFavorite = { onToggleFavorite(song.id) },
                onAddToPlaylist = { onAddToPlaylistClick(song) }
            )
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArt(uri = song.albumArtUri, modifier = Modifier.size(48.dp))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} · ${song.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatDuration(song.durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Add to playlist") },
                    onClick = {
                        menuExpanded = false
                        onAddToPlaylist()
                    }
                )
            }
        }
    }
}
