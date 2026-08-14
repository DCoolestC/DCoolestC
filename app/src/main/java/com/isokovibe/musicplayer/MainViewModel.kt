package com.isokovibe.musicplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isokovibe.musicplayer.data.CustomAd
import com.isokovibe.musicplayer.data.CustomAdsRepository
import com.isokovibe.musicplayer.data.DEFAULT_CUSTOM_ADS_FEED_URL
import com.isokovibe.musicplayer.data.MusicRepository
import com.isokovibe.musicplayer.data.Playlist
import com.isokovibe.musicplayer.data.Song
import com.isokovibe.musicplayer.data.SortOption
import com.isokovibe.musicplayer.data.TEST_BANNER_AD_UNIT_ID
import com.isokovibe.musicplayer.data.ThemeMode
import com.isokovibe.musicplayer.data.UserDataRepository
import com.isokovibe.musicplayer.playback.PlaybackController
import com.isokovibe.musicplayer.playback.PlaybackUiState
import com.isokovibe.musicplayer.push.PushNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val isLoading: Boolean = true,
    val permissionRequired: Boolean = false,
    val songs: List<Song> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val userData = UserDataRepository(application)
    private val customAdsRepository = CustomAdsRepository()
    private val playback = PlaybackController(application, viewModelScope)

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    private val _permissionRequired = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    private val _showFavoritesOnly = MutableStateFlow(false)

    val searchQuery: StateFlow<String> = _searchQuery
    val sortOption: StateFlow<SortOption> = _sortOption
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly

    val favorites: StateFlow<Set<Long>> =
        userData.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val playlists: StateFlow<List<Playlist>> =
        userData.playlists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val themeMode: StateFlow<ThemeMode> =
        userData.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DARK)
    val animateAlbumArt: StateFlow<Boolean> =
        userData.animateAlbumArt.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val miniPlayerColor: StateFlow<Int?> =
        userData.miniPlayerColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val showAds: StateFlow<Boolean> =
        userData.showAds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val adUnitId: StateFlow<String> =
        userData.adUnitId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TEST_BANNER_AD_UNIT_ID)
    val notificationsEnabled: StateFlow<Boolean> =
        userData.notificationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val useCustomAds: StateFlow<Boolean> =
        userData.useCustomAds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val customAdsFeedUrl: StateFlow<String> =
        userData.customAdsFeedUrl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_CUSTOM_ADS_FEED_URL)

    private val _customAds = MutableStateFlow<List<CustomAd>>(emptyList())
    val customAds: StateFlow<List<CustomAd>> = _customAds.asStateFlow()
    private val _customAdsError = MutableStateFlow<String?>(null)
    val customAdsError: StateFlow<String?> = _customAdsError.asStateFlow()

    val playbackState: StateFlow<PlaybackUiState> = playback.uiState

    private data class FilterInputs(
        val allSongs: List<Song>,
        val query: String,
        val sort: SortOption,
        val favoritesOnly: Boolean,
        val favorites: Set<Long>
    )

    private val filterInputs = combine(
        _allSongs, _searchQuery, _sortOption, _showFavoritesOnly, favorites
    ) { all, query, sort, favOnly, favs -> FilterInputs(all, query, sort, favOnly, favs) }

    val libraryUiState: StateFlow<LibraryUiState> = combine(
        filterInputs, _isLoading, _permissionRequired
    ) { inputs, loading, permissionRequired ->
        val filtered = inputs.allSongs
            .filter { !inputs.favoritesOnly || it.id in inputs.favorites }
            .filter {
                inputs.query.isBlank() ||
                    it.title.contains(inputs.query, ignoreCase = true) ||
                    it.artist.contains(inputs.query, ignoreCase = true) ||
                    it.album.contains(inputs.query, ignoreCase = true)
            }
            .let { list ->
                when (inputs.sort) {
                    SortOption.TITLE -> list.sortedBy { it.title.lowercase() }
                    SortOption.ARTIST -> list.sortedBy { it.artist.lowercase() }
                    SortOption.ALBUM -> list.sortedBy { it.album.lowercase() }
                    SortOption.DURATION -> list.sortedByDescending { it.durationMs }
                }
            }
        LibraryUiState(isLoading = loading, permissionRequired = permissionRequired, songs = filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    init {
        playback.connect()
        // Keeps the FCM topic subscription in sync with the persisted
        // preference, including on cold start (e.g. after a reinstall).
        viewModelScope.launch {
            userData.notificationsEnabled.collect { enabled -> PushNotificationManager.setSubscribed(enabled) }
        }
        viewModelScope.launch {
            _customAds.value = userData.cachedCustomAds.first() // show cached ads instantly, then refresh
            refreshCustomAds()
        }
    }

    /** Re-fetches the operator's own ads from WordPress. Safe to call anytime — failures are silent. */
    fun refreshCustomAds() {
        viewModelScope.launch {
            val feedUrl = userData.customAdsFeedUrl.first()
            val result = runCatching { customAdsRepository.fetchAds(feedUrl) }
            result.onSuccess { ads ->
                _customAds.value = ads
                _customAdsError.value = null
                userData.cacheCustomAds(ads)
            }.onFailure { error ->
                _customAdsError.value = error.message ?: "Couldn't load ads"
            }
        }
    }

    fun onPermissionGranted() {
        _permissionRequired.value = false
        viewModelScope.launch {
            _isLoading.value = true
            _allSongs.value = repository.loadLibrary()
            _isLoading.value = false
        }
    }

    fun onPermissionDenied() {
        _permissionRequired.value = true
        _isLoading.value = false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun setShowFavoritesOnly(show: Boolean) {
        _showFavoritesOnly.value = show
    }

    fun playSong(song: Song, queue: List<Song> = libraryUiState.value.songs) {
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playback.playQueue(queue, index)
    }

    fun playPlaylist(playlist: Playlist, startSongId: Long? = null) {
        val songs = playlist.songIds.mapNotNull { id -> _allSongs.value.firstOrNull { it.id == id } }
        if (songs.isEmpty()) return
        val startIndex = startSongId?.let { id -> songs.indexOfFirst { it.id == id } }?.coerceAtLeast(0) ?: 0
        playback.playQueue(songs, startIndex)
    }

    fun togglePlayPause() = playback.togglePlayPause()
    fun skipToNext() = playback.skipToNext()
    fun skipToPrevious() = playback.skipToPrevious()
    fun seekTo(positionMs: Long) = playback.seekTo(positionMs)
    fun toggleShuffle() = playback.toggleShuffle()
    fun cycleRepeatMode() = playback.cycleRepeatMode()
    fun setPlaybackSpeed(speed: Float) = playback.setPlaybackSpeed(speed)
    fun startSleepTimer(minutes: Int) = playback.startSleepTimer(minutes)
    fun cancelSleepTimer() = playback.cancelSleepTimer()

    fun songById(id: Long?): Song? = _allSongs.value.firstOrNull { it.id == id }

    fun songsForPlaylist(playlist: Playlist): List<Song> =
        playlist.songIds.mapNotNull { id -> _allSongs.value.firstOrNull { it.id == id } }

    fun toggleFavorite(songId: Long) = viewModelScope.launch { userData.toggleFavorite(songId) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { userData.setThemeMode(mode) }
    fun setAnimateAlbumArt(enabled: Boolean) = viewModelScope.launch { userData.setAnimateAlbumArt(enabled) }
    fun setMiniPlayerColor(colorArgb: Int?) = viewModelScope.launch { userData.setMiniPlayerColor(colorArgb) }
    fun setShowAds(enabled: Boolean) = viewModelScope.launch { userData.setShowAds(enabled) }
    fun setAdUnitId(id: String) = viewModelScope.launch { userData.setAdUnitId(id) }
    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { userData.setNotificationsEnabled(enabled) }
    fun setUseCustomAds(enabled: Boolean) = viewModelScope.launch { userData.setUseCustomAds(enabled) }
    fun setCustomAdsFeedUrl(url: String) = viewModelScope.launch {
        userData.setCustomAdsFeedUrl(url)
        refreshCustomAds()
    }
    fun createPlaylist(name: String) = viewModelScope.launch { userData.createPlaylist(name) }
    fun deletePlaylist(id: String) = viewModelScope.launch { userData.deletePlaylist(id) }
    fun renamePlaylist(id: String, name: String) = viewModelScope.launch { userData.renamePlaylist(id, name) }
    fun addSongToPlaylist(playlistId: String, songId: Long) =
        viewModelScope.launch { userData.addSongToPlaylist(playlistId, songId) }
    fun removeSongFromPlaylist(playlistId: String, songId: Long) =
        viewModelScope.launch { userData.removeSongFromPlaylist(playlistId, songId) }

    fun createPlaylistAndAddSong(name: String, songId: Long) = viewModelScope.launch {
        val created = userData.createPlaylist(name)
        userData.addSongToPlaylist(created.id, songId)
    }

    override fun onCleared() {
        playback.release()
        super.onCleared()
    }
}
