package com.isokovibe.musicplayer.data

import android.net.Uri

/** The browse views available inside the Library screen. */
enum class LibraryTab(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    GENRES("Genres"),
    FOLDERS("Folders")
}

/**
 * Which kind of grouping a detail screen is showing. [key] semantics differ
 * per type — an id for albums/artists, a name for genres, a path for
 * folders — so [songsIn] owns the matching rather than callers doing it.
 */
enum class GroupType { ALBUM, ARTIST, GENRE, FOLDER }

data class AlbumGroup(
    val id: Long,
    val title: String,
    val artist: String,
    val trackCount: Int,
    val artUri: Uri?
)

data class ArtistGroup(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val trackCount: Int,
    val artUri: Uri?
)

data class GenreGroup(
    val name: String,
    val trackCount: Int,
    val artUri: Uri?
)

data class FolderGroup(
    val path: String,
    val name: String,
    val trackCount: Int
)

/**
 * Everything the browse tabs need, derived in one pass so a library change
 * doesn't cost four separate traversals.
 *
 * These are grouped off the *already filtered* song list on purpose — see
 * the note on [Song]. Querying MediaStore's album/artist tables directly
 * would ignore the user's library filters and surface albums built entirely
 * from tracks they asked to hide.
 */
data class LibraryGroups(
    val albums: List<AlbumGroup> = emptyList(),
    val artists: List<ArtistGroup> = emptyList(),
    val genres: List<GenreGroup> = emptyList(),
    val folders: List<FolderGroup> = emptyList()
)

fun List<Song>.buildGroups(): LibraryGroups {
    if (isEmpty()) return LibraryGroups()

    val albums = groupBy { it.albumId }
        .map { (albumId, tracks) ->
            val first = tracks.first()
            AlbumGroup(
                id = albumId,
                title = first.album,
                artist = tracks.map { it.artist }.distinct().singleOrNull() ?: "Various artists",
                trackCount = tracks.size,
                artUri = tracks.firstNotNullOfOrNull { it.albumArtUri }
            )
        }
        .sortedBy { it.title.lowercase() }

    val artists = groupBy { it.artistId }
        .map { (artistId, tracks) ->
            ArtistGroup(
                id = artistId,
                name = tracks.first().artist,
                albumCount = tracks.map { it.albumId }.distinct().size,
                trackCount = tracks.size,
                artUri = tracks.firstNotNullOfOrNull { it.albumArtUri }
            )
        }
        .sortedBy { it.name.lowercase() }

    // Genres are only populated on API 30+, and even there plenty of files
    // carry no genre tag. Untagged tracks are left out entirely rather than
    // collected into an "Unknown" bucket that would dominate the tab.
    val genres = filter { !it.genre.isNullOrBlank() }
        .groupBy { it.genre!! }
        .map { (name, tracks) ->
            GenreGroup(
                name = name,
                trackCount = tracks.size,
                artUri = tracks.firstNotNullOfOrNull { it.albumArtUri }
            )
        }
        .sortedBy { it.name.lowercase() }

    val folders = filter { it.folderPath.isNotEmpty() }
        .groupBy { it.folderPath }
        .map { (path, tracks) ->
            FolderGroup(
                path = path,
                name = tracks.first().folderName.ifEmpty { path },
                trackCount = tracks.size
            )
        }
        .sortedBy { it.name.lowercase() }

    return LibraryGroups(albums, artists, genres, folders)
}

/** Narrows every tab to entries whose name matches [query], so the one
 *  search box at the top of the library works on whichever tab is open. */
fun LibraryGroups.matching(query: String): LibraryGroups {
    if (query.isBlank()) return this
    fun String.hit() = contains(query, ignoreCase = true)
    return LibraryGroups(
        albums = albums.filter { it.title.hit() || it.artist.hit() },
        artists = artists.filter { it.name.hit() },
        genres = genres.filter { it.name.hit() },
        folders = folders.filter { it.name.hit() || it.path.hit() }
    )
}

/**
 * The tracks belonging to one group, in the order that makes sense for it:
 * albums play in track-number order, everything else alphabetically.
 */
fun List<Song>.songsIn(type: GroupType, key: String): List<Song> = when (type) {
    GroupType.ALBUM ->
        filter { it.albumId.toString() == key }.sortedWith(
            compareBy({ it.trackNumber }, { it.title.lowercase() })
        )
    GroupType.ARTIST ->
        filter { it.artistId.toString() == key }.sortedWith(
            compareBy({ it.album.lowercase() }, { it.trackNumber }, { it.title.lowercase() })
        )
    GroupType.GENRE -> filter { it.genre == key }.sortedBy { it.title.lowercase() }
    GroupType.FOLDER -> filter { it.folderPath == key }.sortedBy { it.title.lowercase() }
}
