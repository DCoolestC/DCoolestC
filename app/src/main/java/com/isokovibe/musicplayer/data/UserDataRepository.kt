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

/** Google's official AdMob test banner unit — swap for a real one in Settings once you have an AdMob account. */
const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

/**
 * Everything the app remembers locally between launches: theme choice,
 * favorites, playlists, and display/ad/notification preferences. No
 * account, no server — just DataStore.
 */
class UserDataRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FAVORITES = stringPreferencesKey("favorites")
        val PLAYLISTS = stringPreferencesKey("playlists")
        val ANIMATE_ALBUM_ART = booleanPreferencesKey("animate_album_art")
        val MINI_PLAYER_COLOR = intPreferencesKey("mini_player_color")
        val SHOW_ADS = booleanPreferencesKey("show_ads")
        val AD_UNIT_ID = stringPreferencesKey("ad_unit_id")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val USE_CUSTOM_ADS = booleanPreferencesKey("use_custom_ads")
        val CUSTOM_ADS_FEED_URL = stringPreferencesKey("custom_ads_feed_url")
        val CUSTOM_ADS_CACHE = stringPreferencesKey("custom_ads_cache")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val themeMode: Flow<ThemeMode> = context.userDataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { raw -> runCatching { ThemeMode.valueOf(raw) }.getOrNull() }
            ?: ThemeMode.DARK
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

    /** Whether the Now Playing album art spins while a track is playing. */
    val animateAlbumArt: Flow<Boolean> = context.userDataStore.data.map { prefs ->
        prefs[Keys.ANIMATE_ALBUM_ART] ?: true
    }

    /** Mini player background color, stored as an ARGB int; null means "use the theme default". */
    val miniPlayerColor: Flow<Int?> = context.userDataStore.data.map { prefs -> prefs[Keys.MINI_PLAYER_COLOR] }

    val showAds: Flow<Boolean> = context.userDataStore.data.map { prefs -> prefs[Keys.SHOW_ADS] ?: true }

    val adUnitId: Flow<String> = context.userDataStore.data.map { prefs ->
        prefs[Keys.AD_UNIT_ID]?.takeIf { it.isNotBlank() } ?: TEST_BANNER_AD_UNIT_ID
    }

    val notificationsEnabled: Flow<Boolean> = context.userDataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: false
    }

    /** Prefer the operator's own ads (from [customAdsFeedUrl]) over the AdMob banner when both are available. */
    val useCustomAds: Flow<Boolean> = context.userDataStore.data.map { prefs -> prefs[Keys.USE_CUSTOM_ADS] ?: true }

    val customAdsFeedUrl: Flow<String> = context.userDataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_ADS_FEED_URL]?.takeIf { it.isNotBlank() } ?: DEFAULT_CUSTOM_ADS_FEED_URL
    }

    /** Last successfully fetched custom ads, so they still show (stale) if a device is briefly offline. */
    val cachedCustomAds: Flow<List<CustomAd>> = context.userDataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_ADS_CACHE]?.let { raw ->
            runCatching { json.decodeFromString<List<CustomAd>>(raw) }.getOrNull()
        } ?: emptyList()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.userDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAnimateAlbumArt(enabled: Boolean) {
        context.userDataStore.edit { it[Keys.ANIMATE_ALBUM_ART] = enabled }
    }

    suspend fun setMiniPlayerColor(colorArgb: Int?) {
        context.userDataStore.edit {
            if (colorArgb == null) it.remove(Keys.MINI_PLAYER_COLOR) else it[Keys.MINI_PLAYER_COLOR] = colorArgb
        }
    }

    suspend fun setShowAds(enabled: Boolean) {
        context.userDataStore.edit { it[Keys.SHOW_ADS] = enabled }
    }

    suspend fun setAdUnitId(id: String) {
        context.userDataStore.edit { it[Keys.AD_UNIT_ID] = id }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.userDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setUseCustomAds(enabled: Boolean) {
        context.userDataStore.edit { it[Keys.USE_CUSTOM_ADS] = enabled }
    }

    suspend fun setCustomAdsFeedUrl(url: String) {
        context.userDataStore.edit { it[Keys.CUSTOM_ADS_FEED_URL] = url }
    }

    suspend fun cacheCustomAds(ads: List<CustomAd>) {
        context.userDataStore.edit { it[Keys.CUSTOM_ADS_CACHE] = json.encodeToString(ads) }
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

    private suspend fun updatePlaylists(transform: (List<Playlist>) -> List<Playlist>) {
        context.userDataStore.edit { prefs ->
            val current = prefs[Keys.PLAYLISTS]?.let {
                runCatching { json.decodeFromString<List<Playlist>>(it) }.getOrNull()
            } ?: emptyList()
            prefs[Keys.PLAYLISTS] = json.encodeToString(transform(current))
        }
    }
}
