package com.isokovibe.musicplayer.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the local audio library from [MediaStore]. No network, no external
 * catalog — just what's already on the device.
 */
class MusicRepository(private val context: Context) {

    /**
     * @param minDurationMs tracks shorter than this are skipped entirely —
     *   the "skip short clips" library setting. 0 disables the filter.
     * @param excludeWhatsAppVoiceNotes skips anything that looks like a
     *   WhatsApp voice note (by folder or filename pattern), even if the
     *   device miscategorized it as music.
     * @param excludedFolders absolute directory paths to skip. Anything at
     *   or below one of these paths is left out of the library.
     */
    suspend fun loadLibrary(
        minDurationMs: Long = 0L,
        excludeWhatsAppVoiceNotes: Boolean = true,
        excludedFolders: Set<String> = emptySet()
    ): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        // GENRE only exists as a media column from API 30 on. Asking for it
        // on older devices throws rather than returning nulls, so it's added
        // conditionally and read back through a guarded column index.
        val supportsGenre = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.ARTIST_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATA)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.TRACK)
            if (supportsGenre) add(MediaStore.Audio.Media.GENRE)
        }.toTypedArray()

        // Only real, non-trashed music tracks — filters out ringtones/notifications/etc.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                // These are read with getColumnIndex rather than …OrThrow because not
                // every device/URI combination is guaranteed to return them. DATA is
                // deprecated but still populated on every API level this app supports;
                // DISPLAY_NAME is the modern equivalent for the filename alone.
                val artistIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val trackCol = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val genreCol = if (supportsGenre) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else {
                    -1
                }

                while (cursor.moveToNext()) {
                    val durationMs = cursor.getLong(durationCol)
                    if (durationMs < minDurationMs) continue

                    val path = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val displayName = if (nameCol >= 0) cursor.getString(nameCol) else null
                    if (excludeWhatsAppVoiceNotes && isWhatsAppVoiceNote(path, displayName)) continue

                    val folderPath = path?.let { File(it).parent }.orEmpty()
                    if (isInExcludedFolder(folderPath, excludedFolders)) continue

                    val id = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    songs += Song(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Unknown title",
                        artist = cursor.getString(artistCol) ?: "Unknown artist",
                        album = cursor.getString(albumCol) ?: "Unknown album",
                        durationMs = durationMs,
                        contentUri = contentUri,
                        albumArtUri = albumArtUri,
                        albumId = albumId,
                        artistId = if (artistIdCol >= 0) cursor.getLong(artistIdCol) else 0L,
                        folderPath = folderPath,
                        folderName = folderPath.takeIf { it.isNotEmpty() }
                            ?.let { File(it).name }.orEmpty(),
                        genre = if (genreCol >= 0) cursor.getString(genreCol) else null,
                        dateAddedEpochSec = if (dateAddedCol >= 0) cursor.getLong(dateAddedCol) else 0L,
                        // MediaStore encodes disc number in the thousands place
                        // (disc 2 track 5 arrives as 2005), so fold it back down.
                        trackNumber = if (trackCol >= 0) cursor.getInt(trackCol) % 1000 else 0
                    )
                }
            }

        songs
    }

    /**
     * Heuristic match for WhatsApp voice notes: either the file lives under
     * a "WhatsApp ... Voice Note(s)" folder, or its name follows WhatsApp's
     * own PTT-/AUD-...-WA naming convention. Some devices index these as
     * regular music, which is exactly what this is meant to catch.
     */
    private fun isWhatsAppVoiceNote(path: String?, displayName: String?): Boolean {
        val lowerPath = path?.lowercase().orEmpty()
        val lowerName = displayName?.lowercase().orEmpty()
        val inWhatsAppVoiceFolder = lowerPath.contains("whatsapp") && lowerPath.contains("voice note")
        val looksLikeVoiceNoteFile = lowerName.startsWith("ptt-") ||
            (lowerName.startsWith("aud-") && lowerName.contains("-wa"))
        return inWhatsAppVoiceFolder || (lowerPath.contains("whatsapp") && looksLikeVoiceNoteFile)
    }

    /** True when [folderPath] is one of [excluded] or sits inside one of them. */
    private fun isInExcludedFolder(folderPath: String, excluded: Set<String>): Boolean {
        if (folderPath.isEmpty() || excluded.isEmpty()) return false
        return excluded.any { folderPath == it || folderPath.startsWith("$it/") }
    }
}
