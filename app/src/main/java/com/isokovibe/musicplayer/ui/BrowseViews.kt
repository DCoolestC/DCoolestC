package com.isokovibe.musicplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
// Aliased: LazyVerticalGrid's items() and LazyColumn's items() are distinct
// extensions on different receivers, and both are used in this file.
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isokovibe.musicplayer.data.AlbumGroup
import com.isokovibe.musicplayer.data.ArtistGroup
import com.isokovibe.musicplayer.data.FolderGroup
import com.isokovibe.musicplayer.data.GenreGroup
import com.isokovibe.musicplayer.ui.components.AlbumArt

@Composable
fun AlbumGridView(
    albums: List<AlbumGroup>,
    onAlbumClick: (AlbumGroup) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    if (albums.isEmpty()) {
        BrowseEmptyState("No albums found.", modifier)
        return
    }
    // Adaptive rather than a fixed column count so the grid stays sensible
    // on a small phone and a tablet without a separate layout.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        gridItems(albums, key = { it.id }) { album ->
            Column(
                modifier = Modifier
                    .clickable { onAlbumClick(album) }
                    .padding(4.dp)
            ) {
                AlbumArt(
                    uri = album.artUri,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    shape = RoundedCornerShape(10.dp)
                )
                Text(
                    album.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AlbumListView(
    albums: List<AlbumGroup>,
    onAlbumClick: (AlbumGroup) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    if (albums.isEmpty()) {
        BrowseEmptyState("No albums found.", modifier)
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(albums, key = { it.id }) { album ->
            BrowseRow(
                title = album.title,
                subtitle = "${album.artist} · ${trackLabel(album.trackCount)}",
                artUri = album.artUri,
                fallbackIcon = Icons.Filled.Album,
                onClick = { onAlbumClick(album) }
            )
        }
    }
}

@Composable
fun ArtistListView(
    artists: List<ArtistGroup>,
    onArtistClick: (ArtistGroup) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    if (artists.isEmpty()) {
        BrowseEmptyState("No artists found.", modifier)
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(artists, key = { it.id }) { artist ->
            val albums = if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums"
            BrowseRow(
                title = artist.name,
                subtitle = "$albums · ${trackLabel(artist.trackCount)}",
                artUri = artist.artUri,
                fallbackIcon = Icons.Filled.Person,
                onClick = { onArtistClick(artist) }
            )
        }
    }
}

@Composable
fun GenreListView(
    genres: List<GenreGroup>,
    onGenreClick: (GenreGroup) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    if (genres.isEmpty()) {
        // Worth explaining rather than just saying "none" — on Android 10 and
        // older the platform doesn't expose genres at all, so an empty tab
        // here is expected behaviour rather than a missing-music problem.
        BrowseEmptyState(
            "No genres found. Genre tags need Android 11 or newer, and many " +
                "files simply aren't tagged with one.",
            modifier
        )
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(genres, key = { it.name }) { genre ->
            BrowseRow(
                title = genre.name,
                subtitle = trackLabel(genre.trackCount),
                artUri = genre.artUri,
                fallbackIcon = Icons.Filled.MusicNote,
                onClick = { onGenreClick(genre) }
            )
        }
    }
}

@Composable
fun FolderListView(
    folders: List<FolderGroup>,
    onFolderClick: (FolderGroup) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    if (folders.isEmpty()) {
        BrowseEmptyState("No folders found.", modifier)
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(folders, key = { it.path }) { folder ->
            BrowseRow(
                title = folder.name,
                subtitle = "${trackLabel(folder.trackCount)} · ${folder.path}",
                artUri = null,
                fallbackIcon = Icons.Filled.Folder,
                onClick = { onFolderClick(folder) }
            )
        }
    }
}

@Composable
private fun BrowseRow(
    title: String,
    subtitle: String,
    artUri: android.net.Uri?,
    fallbackIcon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (artUri != null) {
            AlbumArt(uri = artUri, modifier = Modifier.size(48.dp))
        } else {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    fallbackIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BrowseEmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun trackLabel(count: Int) = if (count == 1) "1 track" else "$count tracks"
