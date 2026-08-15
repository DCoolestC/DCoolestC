package com.isokovibe.musicplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isokovibe.musicplayer.data.ColorSkin
import com.isokovibe.musicplayer.data.FontCombination
import com.isokovibe.musicplayer.data.FontSizeScale
import com.isokovibe.musicplayer.data.FontWeightPreference
import com.isokovibe.musicplayer.data.GroupType
import com.isokovibe.musicplayer.data.LibraryGroups
import com.isokovibe.musicplayer.data.LibraryTab
import com.isokovibe.musicplayer.data.MinTrackDuration
import com.isokovibe.musicplayer.data.DEFAULT_APP_CONFIG_URL
import com.isokovibe.musicplayer.data.MusicRepository
import com.isokovibe.musicplayer.data.Playlist
import com.isokovibe.musicplayer.data.RemoteAppVersion
import com.isokovibe.musicplayer.data.RemoteBanner
import com.isokovibe.musicplayer.data.RemoteConfig
import com.isokovibe.musicplayer.data.RemoteConfigRepository
import com.isokovibe.musicplayer.data.Song
import com.isokovibe.musicplayer.data.SortOption
import com.isokovibe.musicplayer.data.ThemeMode
import com.isokovibe.musicplayer.data.UserDataRepository
import com.isokovibe.musicplayer.data.DuplicateGroup
import com.isokovibe.musicplayer.data.SmartPlaylist
import com.isokovibe.musicplayer.data.StoredAudioEffects
import com.isokovibe.musicplayer.data.buildGroups
import com.isokovibe.musicplayer.data.findDuplicates
import com.isokovibe.musicplayer.data.matching
import com.isokovibe.musicplayer.data.resolveSmartPlaylist
import com.isokovibe.musicplayer.data.songsIn
import com.isokovibe.musicplayer.playback.AudioEffectSettings
import com.isokovibe.musicplayer.playback.AudioEngine
import com.isokovibe.musicplayer.playback.EqualizerCapabilities
import com.isokovibe.musicplayer.playback.PlaybackController
import com.isokovibe.musicplayer.playback.PlaybackUiState
import com.isokovibe.musicplayer.remote.AnnouncementWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Bridges the stored (serializable) form to the playback layer's own type,
 *  keeping the data layer free of any dependency on platform audio APIs. */
private fun StoredAudioEffects.toEngineSettings() = AudioEffectSettings(
    equalizerEnabled = equalizerEnabled,
    bandLevels = bandLevels,
    presetIndex = presetIndex,
    bassBoost = bassBoost,
    virtualizer = virtualizer,
    reverbPreset = reverbPreset,
    loudnessGain = loudnessGain,
    skipSilence = skipSilence
)

/** How often the queue/position snapshot is written for resume. */
private const val SAVE_INTERVAL_MS = 5_000L

/** Tracks at least this long get their own per-track resume point. */
private const val BOOKMARK_MIN_DURATION_MS = 10 * 60 * 1000L

data class LibraryUiState(
    val isLoading: Boolean = true,
    val permissionRequired: Boolean = false,
    val songs: List<Song> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val userData = UserDataRepository(application)
    private val remoteConfig = RemoteConfigRepository()
    private val playback = PlaybackController(application, viewModelScope)

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    private val _permissionRequired = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    private val _showFavoritesOnly = MutableStateFlow(false)
    private val _libraryTab = MutableStateFlow(LibraryTab.SONGS)
    private val _albumGridView = MutableStateFlow(true)
    private var permissionGranted = false
    private var controllerReady = false
    private var queueRestored = false

    val searchQuery: StateFlow<String> = _searchQuery
    val sortOption: StateFlow<SortOption> = _sortOption
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly
    val libraryTab: StateFlow<LibraryTab> = _libraryTab
    val albumGridView: StateFlow<Boolean> = _albumGridView

    /**
     * Albums/artists/genres/folders for the browse tabs, rebuilt whenever
     * the library changes and narrowed by the same search box the Songs tab
     * uses. Derived from the full scanned list rather than the Songs tab's
     * filtered view, so toggling "Favorites only" doesn't silently empty
     * out the album grid.
     */
    val libraryGroups: StateFlow<LibraryGroups> = combine(_allSongs, _searchQuery) { songs, query ->
        songs.buildGroups().matching(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryGroups())

    val favorites: StateFlow<Set<Long>> =
        userData.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val playlists: StateFlow<List<Playlist>> =
        userData.playlists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val themeMode: StateFlow<ThemeMode> =
        userData.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DARK)
    val colorSkin: StateFlow<ColorSkin> =
        userData.colorSkin.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ColorSkin.VIBE_RED)
    val fontCombination: StateFlow<FontCombination> =
        userData.fontCombination.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FontCombination.ROBOTO_OPEN_SANS)
    val fontSizeScale: StateFlow<FontSizeScale> =
        userData.fontSizeScale.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FontSizeScale.DEFAULT)
    val fontWeightPreference: StateFlow<FontWeightPreference> =
        userData.fontWeightPreference.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FontWeightPreference.DEFAULT)
    val miniPlayerColor: StateFlow<Int?> =
        userData.miniPlayerColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val minTrackDuration: StateFlow<MinTrackDuration> =
        userData.minTrackDuration.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MinTrackDuration.OFF)
    val excludeWhatsAppVoiceNotes: StateFlow<Boolean> =
        userData.excludeWhatsAppVoiceNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val excludedFolders: StateFlow<Set<String>> =
        userData.excludedFolders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val playCounts: StateFlow<Map<Long, Int>> =
        userData.playCounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val lastPlayed: StateFlow<Map<Long, Long>> =
        userData.lastPlayed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val bookmarks: StateFlow<Map<Long, Long>> =
        userData.bookmarks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val audioEffects: StateFlow<StoredAudioEffects> =
        userData.audioEffects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoredAudioEffects())

    /** What this specific device's equalizer supports — band count and
     *  frequencies vary by hardware, so the UI is built from this. */
    val equalizerCapabilities: StateFlow<EqualizerCapabilities> = AudioEngine.capabilities

    val appConfigUrl: StateFlow<String> =
        userData.appConfigUrl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_APP_CONFIG_URL)
    val remoteUpdatesEnabled: StateFlow<Boolean> =
        userData.remoteUpdatesEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val showRemoteBanners: StateFlow<Boolean> =
        userData.showRemoteBanners.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val cachedConfig: StateFlow<RemoteConfig?> =
        userData.cachedRemoteConfig.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Banners to show, or none when the user has switched them off. */
    val banners: StateFlow<List<RemoteBanner>> =
        combine(cachedConfig, showRemoteBanners) { config, enabled ->
            if (enabled) config?.banners.orEmpty() else emptyList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * The update prompt to show, or null. Filters out versions at or below
     * the installed build, and ones the user already dismissed — except
     * required updates, which reappear until acted on.
     */
    val pendingUpdate: StateFlow<RemoteAppVersion?> =
        combine(cachedConfig, userData.dismissedUpdateCode) { config, dismissed ->
            val version = config?.appVersion ?: return@combine null
            if (version.versionCode <= BuildConfig.VERSION_CODE) return@combine null
            if (!version.required && version.versionCode <= dismissed) return@combine null
            version
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val playbackState: StateFlow<PlaybackUiState> = playback.uiState
    val queue: StateFlow<List<Song>> = playback.queue

    /** How many tracks each smart playlist currently resolves to, for the
     *  subtitles on the Playlists screen. Derived once per library/stats
     *  change rather than recomputed during composition. */
    val smartPlaylistCounts: StateFlow<Map<SmartPlaylist, Int>> =
        combine(_allSongs, playCounts, lastPlayed, favorites) { songs, counts, last, favs ->
            SmartPlaylist.entries.associateWith { kind ->
                resolveSmartPlaylist(kind, songs, counts, last, favs).size
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val duplicateGroups: StateFlow<List<DuplicateGroup>> =
        _allSongs.map { it.findDuplicates() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class FilterInputs(
        val allSongs: List<Song>,
        val query: String,
        val sort: SortOption,
        val favoritesOnly: Boolean,
        val favorites: Set<Long>
    )

    private data class PlayStats(val counts: Map<Long, Int>, val lastPlayed: Map<Long, Long>)

    private val filterInputs = combine(
        _allSongs, _searchQuery, _sortOption, _showFavoritesOnly, favorites
    ) { all, query, sort, favOnly, favs -> FilterInputs(all, query, sort, favOnly, favs) }

    private val playStats = combine(playCounts, lastPlayed) { counts, last -> PlayStats(counts, last) }

    val libraryUiState: StateFlow<LibraryUiState> = combine(
        filterInputs, _isLoading, _permissionRequired, playStats
    ) { inputs, loading, permissionRequired, stats ->
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
                    SortOption.RECENTLY_PLAYED -> list.sortedByDescending { stats.lastPlayed[it.id] ?: 0L }
                    SortOption.MOST_PLAYED -> list.sortedByDescending { stats.counts[it.id] ?: 0 }
                }
            }
        LibraryUiState(isLoading = loading, permissionRequired = permissionRequired, songs = filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    init {
        playback.connect { controllerReady = true; restoreQueueIfReady() }
        // One place to record a play, regardless of whether it came from a
        // manual tap, a skip, or the queue auto-advancing.
        viewModelScope.launch {
            playbackState.map { it.currentSongId }.distinctUntilChanged().collect { id ->
                if (id != null) userData.recordPlay(id)
            }
        }
        startPersistingPlaybackPosition()
        // Push saved effect settings into the audio pipeline, and keep doing
        // so as they change. AudioEngine also replays the last-applied
        // settings whenever a new audio session attaches, which covers the
        // case where the service starts after these have already been read.
        viewModelScope.launch {
            userData.audioEffects.collect { AudioEngine.applySettings(it.toEngineSettings()) }
        }
        // Refresh on launch so banners and the update prompt are current the
        // moment the app opens, rather than waiting for the background check.
        AnnouncementWorker.createChannel(application)
        viewModelScope.launch {
            if (userData.remoteUpdatesEnabled.first()) {
                AnnouncementWorker.schedule(application)
                refreshRemoteConfig()
            }
        }
    }

    /**
     * Periodically snapshots the queue and position so the app can pick up
     * where it left off. Polled on a timer rather than written on every
     * position tick — the position updates four times a second, and putting
     * a DataStore write behind each one would be a lot of disk churn for
     * something only read once per launch.
     */
    private fun startPersistingPlaybackPosition() {
        viewModelScope.launch {
            var lastSavedPosition = -1L
            while (isActive) {
                delay(SAVE_INTERVAL_MS)
                val state = playbackState.value
                val currentQueue = queue.value
                if (currentQueue.isEmpty() || state.currentSongId == null) continue
                if (state.positionMs == lastSavedPosition) continue
                lastSavedPosition = state.positionMs
                userData.saveQueueState(
                    songIds = currentQueue.map { it.id },
                    index = state.currentIndex,
                    positionMs = state.positionMs
                )
                // Long tracks get their own resume point, so coming back to a
                // mix or a podcast-length recording days later doesn't restart it.
                val current = songById(state.currentSongId)
                if (current != null && current.durationMs >= BOOKMARK_MIN_DURATION_MS) {
                    userData.setBookmark(current.id, state.positionMs)
                }
            }
        }
    }

    /**
     * Restores the saved queue once *both* the controller has connected and
     * the library has been scanned — they complete independently, so this is
     * called from each and only acts when the other has already finished.
     */
    private fun restoreQueueIfReady() {
        if (!controllerReady || queueRestored || _allSongs.value.isEmpty()) return
        if (queue.value.isNotEmpty()) return
        queueRestored = true
        viewModelScope.launch {
            val saved = userData.savedQueue.first() ?: return@launch
            val byId = _allSongs.value.associateBy { it.id }
            // Ids that no longer resolve are simply dropped — files get
            // deleted between sessions, and a missing track shouldn't stop
            // the rest of the queue coming back.
            val songs = saved.songIds.mapNotNull { byId[it] }
            if (songs.isEmpty()) return@launch
            val index = saved.index.coerceIn(0, songs.lastIndex)
            playback.restoreQueue(songs, index, saved.positionMs)
        }
    }

    fun onPermissionGranted() {
        _permissionRequired.value = false
        permissionGranted = true
        rescanLibrary()
    }

    fun onPermissionDenied() {
        _permissionRequired.value = true
        _isLoading.value = false
    }

    /** Re-scans MediaStore for tracks — the manual "Scan library" action, and what happens on first
     *  permission grant or a change to the library filter settings. */
    fun rescanLibrary() {
        if (!permissionGranted) return
        viewModelScope.launch {
            _isLoading.value = true
            // Read the persisted values directly rather than the StateFlow
            // snapshots. Those start at their defaults and only catch up once
            // DataStore has emitted, so a cold-start scan used to run with
            // default filters and silently ignore the user's saved settings.
            // Reading here also means the setters below just write and
            // re-scan, with no stale-value window in between.
            _allSongs.value = repository.loadLibrary(
                minDurationMs = userData.minTrackDuration.first().seconds * 1000L,
                excludeWhatsAppVoiceNotes = userData.excludeWhatsAppVoiceNotes.first(),
                excludedFolders = userData.excludedFolders.first()
            )
            _isLoading.value = false
            restoreQueueIfReady()
        }
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

    fun setLibraryTab(tab: LibraryTab) {
        _libraryTab.value = tab
    }

    fun setAlbumGridView(grid: Boolean) {
        _albumGridView.value = grid
    }

    /** Tracks inside one album/artist/genre/folder, for its detail screen. */
    fun songsInGroup(type: GroupType, key: String): List<Song> = _allSongs.value.songsIn(type, key)

    /** Plays an arbitrary list in the order given, optionally starting partway in. */
    fun playSongs(songs: List<Song>, startSongId: Long? = null) {
        if (songs.isEmpty()) return
        val startIndex = startSongId
            ?.let { id -> songs.indexOfFirst { it.id == id }.takeIf { i -> i >= 0 } }
            ?: 0
        // A long track that was left partway through resumes there rather
        // than restarting. Short tracks always start from the top — resuming
        // 40 seconds into a 3-minute song is more annoying than helpful.
        val start = songs[startIndex]
        val resumeAt = if (start.durationMs >= BOOKMARK_MIN_DURATION_MS) {
            bookmarks.value[start.id] ?: 0L
        } else {
            0L
        }
        playback.playQueue(songs, startIndex, resumeAt)
    }

    fun shuffleSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        playback.playQueue(songs.shuffled(), 0)
    }

    fun playGroup(type: GroupType, key: String, startSongId: Long? = null) =
        playSongs(songsInGroup(type, key), startSongId)

    fun shuffleGroup(type: GroupType, key: String) = shuffleSongs(songsInGroup(type, key))

    /**
     * Resolves a smart playlist against the current library and stats.
     * Read from the StateFlow snapshots rather than collected, since this is
     * called from composition on demand rather than driving a subscription.
     */
    fun smartPlaylistSongs(kind: SmartPlaylist): List<Song> = resolveSmartPlaylist(
        kind = kind,
        songs = _allSongs.value,
        playCounts = playCounts.value,
        lastPlayed = lastPlayed.value,
        favorites = favorites.value
    )


    fun toggleExcludedFolder(path: String) = viewModelScope.launch {
        userData.toggleExcludedFolder(path)
        rescanLibrary()
    }

    /** Writes new effect settings; the collector in init pushes them to the
     *  audio pipeline, so there's one path from storage to hardware. */
    fun setAudioEffects(settings: StoredAudioEffects) = viewModelScope.launch {
        userData.setAudioEffects(settings)
    }

    /**
     * Refreshes banners/update info from the site. Failures are swallowed on
     * purpose — no signal or a site that's down is an ordinary state, and
     * the cached config carries on being used.
     */
    fun refreshRemoteConfig() = viewModelScope.launch {
        if (!userData.remoteUpdatesEnabled.first()) return@launch
        remoteConfig.fetch(userData.appConfigUrl.first())
            .onSuccess { userData.cacheRemoteConfig(it) }
    }

    fun setAppConfigUrl(url: String) = viewModelScope.launch {
        userData.setAppConfigUrl(url)
        refreshRemoteConfig()
    }

    fun setRemoteUpdatesEnabled(enabled: Boolean) = viewModelScope.launch {
        userData.setRemoteUpdatesEnabled(enabled)
        if (enabled) {
            AnnouncementWorker.schedule(getApplication())
            refreshRemoteConfig()
        } else {
            AnnouncementWorker.cancel(getApplication())
        }
    }

    fun setShowRemoteBanners(show: Boolean) = viewModelScope.launch {
        userData.setShowRemoteBanners(show)
    }

    /** Stops a non-required update prompt reappearing for that version. */
    fun dismissUpdate(versionCode: Int) = viewModelScope.launch {
        userData.setDismissedUpdateCode(versionCode)
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

    /** Jumps to [index] in the currently loaded queue — used by the Now Playing "Up next" sheet. */
    fun playFromQueue(index: Int) = playback.playFromQueue(index)

    fun moveQueueItem(from: Int, to: Int) = playback.moveQueueItem(from, to)
    fun removeFromQueue(index: Int) = playback.removeFromQueue(index)

    /**
     * Queueing a track when nothing is playing has nothing to append to, so
     * it starts playback instead — otherwise the action would silently do
     * nothing, which reads as a broken button.
     */
    fun addToQueue(songs: List<Song>) {
        if (queue.value.isEmpty()) playSongs(songs) else playback.addToQueue(songs)
    }

    fun playNext(songs: List<Song>) {
        if (queue.value.isEmpty()) playSongs(songs) else playback.playNext(songs)
    }

    fun saveQueueAsPlaylist(name: String) = viewModelScope.launch {
        val current = queue.value
        if (current.isEmpty()) return@launch
        val created = userData.createPlaylist(name)
        current.forEach { userData.addSongToPlaylist(created.id, it.id) }
    }

    fun togglePlayPause() = playback.togglePlayPause()
    fun skipToNext() = playback.skipToNext()
    fun skipToPrevious() = playback.skipToPrevious()
    fun seekTo(positionMs: Long) = playback.seekTo(positionMs)
    fun toggleShuffle() = playback.toggleShuffle()
    fun cycleRepeatMode() = playback.cycleRepeatMode()
    fun setPlaybackSpeed(speed: Float) = playback.setPlaybackSpeed(speed)
    fun startSleepTimer(minutes: Int) = playback.startSleepTimer(minutes)
    fun sleepAtEndOfTrack() = playback.sleepAtEndOfTrack()
    fun cancelSleepTimer() = playback.cancelSleepTimer()
    fun setAbPointA() = playback.setAbPointA()
    fun setAbPointB() = playback.setAbPointB()
    fun clearAbRepeat() = playback.clearAbRepeat()

    fun songById(id: Long?): Song? = _allSongs.value.firstOrNull { it.id == id }

    fun songsForPlaylist(playlist: Playlist): List<Song> =
        playlist.songIds.mapNotNull { id -> _allSongs.value.firstOrNull { it.id == id } }

    fun toggleFavorite(songId: Long) = viewModelScope.launch { userData.toggleFavorite(songId) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { userData.setThemeMode(mode) }
    fun setColorSkin(skin: ColorSkin) = viewModelScope.launch { userData.setColorSkin(skin) }
    fun setFontCombination(combination: FontCombination) = viewModelScope.launch { userData.setFontCombination(combination) }
    fun setFontSizeScale(scale: FontSizeScale) = viewModelScope.launch { userData.setFontSizeScale(scale) }
    fun setFontWeightPreference(preference: FontWeightPreference) =
        viewModelScope.launch { userData.setFontWeightPreference(preference) }
    fun setMiniPlayerColor(colorArgb: Int?) = viewModelScope.launch { userData.setMiniPlayerColor(colorArgb) }

    // Each of these writes first and then re-scans; because the write
    // suspends until it's committed, the scan reads back the new value.
    fun setMinTrackDuration(duration: MinTrackDuration) = viewModelScope.launch {
        userData.setMinTrackDuration(duration)
        rescanLibrary()
    }

    fun setExcludeWhatsAppVoiceNotes(exclude: Boolean) = viewModelScope.launch {
        userData.setExcludeWhatsAppVoiceNotes(exclude)
        rescanLibrary()
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
