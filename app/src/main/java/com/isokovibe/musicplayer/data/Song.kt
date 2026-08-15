package com.isokovibe.musicplayer.data

import android.net.Uri

/**
 * A single track read from the device's MediaStore.
 *
 * The grouping fields (albumId/artistId/folderPath/genre) exist so the
 * Albums / Artists / Genres / Folders browse views can be derived from the
 * one already-loaded song list rather than issuing separate MediaStore
 * queries. That matters for correctness as much as speed: the library
 * filters (minimum duration, WhatsApp voice notes) are applied during the
 * scan, so anything grouped off this list inherits them automatically. A
 * separate album query would happily surface albums made entirely of
 * tracks the user asked to hide.
 *
 * All of them are defaulted so older call sites that build a Song by hand
 * keep compiling.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val albumArtUri: Uri?,
    val albumId: Long = 0L,
    val artistId: Long = 0L,
    /** Absolute path of the containing directory, or "" when unknown. */
    val folderPath: String = "",
    /** Just the directory's own name, for display. */
    val folderName: String = "",
    /** Only populated on API 30+, where MediaStore exposes a genre column. */
    val genre: String? = null,
    /** Seconds since epoch, as MediaStore reports it. Used by smart playlists. */
    val dateAddedEpochSec: Long = 0L,
    /** Track number within its album; 0 when the tag is missing. */
    val trackNumber: Int = 0
)
