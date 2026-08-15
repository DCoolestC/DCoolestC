package com.isokovibe.musicplayer.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.userDataStore by preferencesDataStore(name = "isokovibe_user_data")

/**
 * Everything the app remembers locally between launches: theme/appearance
 * choices, favorites, playlists, mini player color, library filters, and
 * lightweight play stats. No account, no server — just DataStore.
 */
class UserDataRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_SKIN = stringPreferencesKey("color_skin")
        val FONT_COMBINATION = stringPreferencesKey("font_combination")
        val FONT_SIZE_SCALE = stringPreferencesKey("font_size_scale")
        val FONT_WEIGHT_PREFERENCE = stringPreferencesKey("font_weight_preference")
        val FAVORITES = stringPreferencesKey("favorites")
        val PLAYLISTS = stringPreferencesKey("playlists")
        val MINI_PLAYER_COLOR = intPreferencesKey("mini_player_color")
        val MIN_TRACK_DURATION = stringPreferencesKey("min_track_duration")
        val EXCLUDE_WHATSAPP_VOICE_NOTES = booleanPreferencesKey("exclude_whatsapp_voice_notes")
        val EXCLUDED_FOLDERS = stringPreferencesKey("excluded_folders")
        val PLAY_COUNTS = stringPreferencesKey("play_counts")
        val LAST_PLAYED = stringPreferencesKey("last_played")
        val SAVED_QUEUE = stringPreferencesKey("saved_queue")
        val BOOKMARKS = stringPreferencesKey("bookmarks")
        val AUDIO_EFFECTS = stringPreferencesKey("audio_effects")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val themeMode: Flow<ThemeMode> = context.userDataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { raw -> runCatching { ThemeMode.valueOf(raw) }.getOrNull() }
            ?: ThemeMode.DARK
    }

    val colorSkin: Flow<ColorSkin> = context.userDataStore.data.map { prefs ->
        prefs[Keys.COLOR_SKIN]?.let { raw -> runCatching { ColorSkin.valueOf(raw) }.getOrNull() }
            ?: ColorSkin.VIBE_RED
    }

    val fontCombination: Flow<FontCombination> = context.userDataStore.data.map { prefs ->
        prefs[Keys.FONT_COMBINATION]?.let { raw -> runCatching { FontCombination.valueOf(raw) }.getOrNull() }
            ?: FontCombination.ROBOTO_OPEN_SANS
    }

    val fontSizeScale: Flow<FontSizeScale> = context.userDataStore.data.map { prefs ->
        prefs[Keys.FONT_SIZE_SCALE]?.let { raw -> runCatching { FontSizeScale.valueOf(raw) }.getOrNull() }
            ?: FontSizeScale.DEFAULT
    }

    val fontWeightPreference: Flow<FontWeightPreference> = context.userDataStore.data.map { prefs ->
        prefs[Keys.FONT_WEIGHT_PREFERENCE]?.let { raw -> runCatching { FontWeightPreference.valueOf(raw) }.getOrNull() }
            ?: FontWeightPreference.DEFAULT
    }

    val favorites: Flow<Set<Long>> = context.userDataStore.data.map { prefs ->
        prefs[Keys.FAVORITES]?.let { raw ->
            runCatching { json.decodeFromString<List<Long>>(raw).toSet() }.getOrNull()
        } ?: emptySet()
    }

    val playlists: Flow<List<Playlist>> = context.userDataStore.data.map { prefs ->
        prefs[Keys.PLAYLISTS]?.let { raw ->
            runCatching { json.decodeFromString<List<Playlist>>(raw) }.getOrNull()
        } ?: emptyList()
    }

    /** Mini player background color, stored as an ARGB int; null means "use the theme default". */
    val miniPlayerColor: Flow<Int?> = context.userDataStore.data.map { prefs -> prefs[Keys.MINI_PLAYER_COLOR] }

    val minTrackDuration: Flow<MinTrackDuration> = context.userDataStore.data.map { prefs ->
        prefs[Keys.MIN_TRACK_DURATION]?.let { raw -> runCatching { MinTrackDuration.valueOf(raw) }.getOrNull() }
            ?: MinTrackDuration.OFF
    }

    val excludeWhatsAppVoiceNotes: Flow<Boolean> = context.userDataStore.data.map { prefs ->
        prefs[Keys.EXCLUDE_WHATSAPP_VOICE_NOTES] ?: true
    }

    /** Absolute directory paths kept out of the library entirely. */
    val excludedFolders: Flow<Set<String>> = context.userDataStore.data.map { prefs ->
        prefs[Keys.EXCLUDED_FOLDERS]?.let { raw ->
            runCatching { json.decodeFromString<List<String>>(raw).toSet() }.getOrNull()
        } ?: emptySet()
    }

    val playCounts: Flow<Map<Long, Int>> = context.userDataStore.data.map { prefs ->
        prefs[Keys.PLAY_COUNTS]?.let { raw ->
            runCatching { json.decodeFromString<Map<Long, Int>>(raw) }.getOrNull()
        } ?: emptyMap()
    }

    val lastPlayed: Flow<Map<Long, Long>> = context.userDataStore.data.map { prefs ->
        prefs[Keys.LAST_PLAYED]?.let { raw ->
            runCatching { json.decodeFromString<Map<Long, Long>>(raw) }.getOrNull()
        } ?: emptyMap()
    }

    /** The queue as it was when the app was last closed, for resuming. */
    val savedQueue: Flow<SavedQueue?> = context.userDataStore.data.map { prefs ->
        prefs[Keys.SAVED_QUEUE]?.let { raw ->
            runCatching { json.decodeFromString<SavedQueue>(raw) }.getOrNull()
        }
    }

    /** Per-track resume points, for tracks long enough to be worth it. */
    val bookmarks: Flow<Map<Long, Long>> = context.userDataStore.data.map { prefs ->
        prefs[Keys.BOOKMARKS]?.let { raw ->
            runCatching { json.decodeFromString<Map<Long, Long>>(raw) }.getOrNull()
        } ?: emptyMap()
    }

    suspend fun saveQueueState(songIds: List<Long>, index: Int, positionMs: Long) {
        context.userDataStore.edit {
            it[Keys.SAVED_QUEUE] = json.encodeToString(SavedQueue(songIds, index, positionMs))
        }
    }

    suspend fun setBookmark(songId: Long, positionMs: Long) {
        context.userDataStore.edit { prefs ->
            val current = prefs[Keys.BOOKMARKS]?.let {
                runCatching { json.decodeFromString<Map<Long, Long>>(it) }.getOrNull()
            } ?: emptyMap()
            prefs[Keys.BOOKMARKS] = json.encodeToString(current + (songId to positionMs))
        }
    }

    suspend fun clearBookmark(songId: Long) {
        context.userDataStore.edit { prefs ->
            val current = prefs[Keys.BOOKMARKS]?.let {
                runCatching { json.decodeFromString<Map<Long, Long>>(it) }.getOrNull()
            } ?: return@edit
            prefs[Keys.BOOKMARKS] = json.encodeToString(current - songId)
        }
    }

    /** Equalizer / bass / virtualizer / reverb / loudness / skip-silence. */
    val audioEffects: Flow<StoredAudioEffects> = context.userDataStore.data.map { prefs ->
        prefs[Keys.AUDIO_EFFECTS]?.let { raw ->
            runCatching { json.decodeFromString<StoredAudioEffects>(raw) }.getOrNull()
        } ?: StoredAudioEffects()
    }

    suspend fun setAudioEffects(settings: StoredAudioEffects) {
        context.userDataStore.edit { it[Keys.AUDIO_EFFECTS] = json.encodeToString(settings) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.userDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setColorSkin(skin: ColorSkin) {
        context.userDataStore.edit { it[Keys.COLOR_SKIN] = skin.name }
    }

    suspend fun setFontCombination(combination: FontCombination) {
        context.userDataStore.edit { it[Keys.FONT_COMBINATION] = combination.name }
    }

    suspend fun setFontSizeScale(scale: FontSizeScale) {
        context.userDataStore.edit { it[Keys.FONT_SIZE_SCALE] = scale.name }
    }

    suspend fun setFontWeightPreference(preference: FontWeightPreference) {
        context.userDataStore.edit { it[Keys.FONT_WEIGHT_PREFERENCE] = preference.name }
    }

    suspend fun setMiniPlayerColor(colorArgb: Int?) {
        context.userDataStore.edit {
            if (colorArgb == null) it.remove(Keys.MINI_PLAYER_COLOR) else it[Keys.MINI_PLAYER_COLOR] = colorArgb
        }
    }

    suspend fun setMinTrackDuration(duration: MinTrackDuration) {
        context.userDataStore.edit { it[Keys.MIN_TRACK_DURATION] = duration.name }
    }

    suspend fun setExcludeWhatsAppVoiceNotes(exclude: Boolean) {
        context.userDataStore.edit { it[Keys.EXCLUDE_WHATSAPP_VOICE_NOTES] = exclude }
    }

    /** Adds [path] to the exclusion list, or removes it if already there. */
    suspend fun toggleExcludedFolder(path: String) {
        context.userDataStore.edit { prefs ->
            val current = prefs[Keys.EXCLUDED_FOLDERS]?.let {
                runCatching { json.decodeFromString<List<String>>(it).toMutableSet() }.getOrNull()
            } ?: mutableSetOf()
            if (!current.add(path)) current.remove(path)
            prefs[Keys.EXCLUDED_FOLDERS] = json.encodeToString(current.toList())
        }
    }

    suspend fun toggleFavorite(songId: Long) {
        context.userDataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES]?.let {
                runCatching { json.decodeFromString<List<Long>>(it).toMutableSet() }.getOrNull()
            } ?: mutableSetOf()
            if (!current.add(songId)) current.remove(songId)
            prefs[Keys.FAVORITES] = json.encodeToString(current.toList())
        }
    }

    suspend fun createPlaylist(name: String): Playlist {
        val newPlaylist = Playlist(id = UUID.randomUUID().toString(), name = name)
        updatePlaylists { it + newPlaylist }
        return newPlaylist
    }

    suspend fun deletePlaylist(id: String) {
        updatePlaylists { list -> list.filterNot { it.id == id } }
    }

    suspend fun renamePlaylist(id: String, newName: String) {
        updatePlaylists { list -> list.map { if (it.id == id) it.copy(name = newName) else it } }
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: Long) {
        updatePlaylists { list ->
            list.map {
                if (it.id == playlistId && songId !in it.songIds) it.copy(songIds = it.songIds + songId) else it
            }
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        updatePlaylists { list ->
            list.map {
                if (it.id == playlistId) it.copy(songIds = it.songIds.filterNot { id -> id == songId }) else it
            }
        }
    }

    /** Bumps [songId]'s play count and last-played timestamp — powers the
     *  Recently played / Most played sort options. */
    suspend fun recordPlay(songId: Long) {
        context.userDataStore.edit { prefs ->
            val counts = prefs[Keys.PLAY_COUNTS]?.let {
                runCatching { json.decodeFromString<Map<Long, Int>>(it) }.getOrNull()
            } ?: emptyMap()
            prefs[Keys.PLAY_COUNTS] = json.encodeToString(counts + (songId to ((counts[songId] ?: 0) + 1)))

            val last = prefs[Keys.LAST_PLAYED]?.let {
                runCatching { json.decodeFromString<Map<Long, Long>>(it) }.getOrNull()
            } ?: emptyMap()
            prefs[Keys.LAST_PLAYED] = json.encodeToString(last + (songId to System.currentTimeMillis()))
        }
    }

    private suspend fun updatePlaylists(transform: (List<Playlist>) -> List<Playlist>) {
        context.userDataStore.edit { prefs ->
            val current = prefs[Keys.PLAYLISTS]?.let {
                runCatching { json.decodeFromString<List<Playlist>>(it) }.getOrNull()
            } ?: emptyList()
            prefs[Keys.PLAYLISTS] = json.encodeToString(transform(current))
        }
    }
}
