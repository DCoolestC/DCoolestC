# iSokoVibe Music Player

A local music player for Android, built with Kotlin, Jetpack Compose, and
Media3 (ExoPlayer).

## What's implemented

- Scans the device's music library via `MediaStore` (no network, no
  external catalog)
- Background playback through a `MediaSessionService`, so audio keeps
  playing when the app isn't in the foreground, with lock-screen /
  notification / Bluetooth controls handled automatically by Media3
- Persistent branded chrome — a top bar with the iSokoVibe mark + name on
  every top-level screen, bottom navigation (Library / Playlists /
  Settings), and a docked mini player — so the brand is visible everywhere,
  not just on one screen
- Branded album-art fallback: a music-note tile instead of a blank square
  when a track has no usable embedded art (the legacy
  `content://media/external/audio/albumart` URI Android hands back often
  fails to resolve, so this is the common case, not the exception)
- **Library** — search, sort (title/artist/album/duration), and a
  Favorites filter chip, on top of the scrollable track list
- **Favorites** — heart-toggle on any track, from the library list or Now
  Playing
- **Playlists** — create, rename, delete, add/remove tracks, play a
  playlist from any track or from the top; persisted locally via DataStore
- **Now Playing** — seek bar, play/pause, skip, shuffle, repeat
  (off → all → one), favorite toggle, sleep timer, playback speed
- **Sleep timer** — 15/30/45/60 minutes, pauses playback and counts down
  live; adjustable from Settings or Now Playing's overflow menu
- **Playback speed** — 0.75x–2x, from Settings or Now Playing
- **Settings** — theme mode (Vibe/dark default, Light, Follow system),
  playback speed, sleep timer, about section, and an honest "coming soon"
  list for what's not built yet
- Runtime permission handling for `READ_MEDIA_AUDIO` (Android 13+) /
  `READ_EXTERNAL_STORAGE` (older)
- Branding pulled from the real iSokoVibe logo/brand art (see below), and
  the app **defaults to the brand's dark red-on-black look** regardless of
  system theme — Material You dynamic color is intentionally not used, so
  the app always reads as iSokoVibe rather than tinting to the phone's
  wallpaper

## Deferred — needs device-level testing to get right

These didn't make this pass because they need real hardware/emulator
verification I can't safely fake from a CI-only build loop — shipping a
silently-broken version would be worse than being upfront that they're not
done yet:

- **Equalizer / bass boost / ReplayGain** — requires wiring
  `android.media.audiofx.Equalizer` to the ExoPlayer instance's audio
  session *across* the `MediaController`/`MediaSession` process boundary
  (a custom session command, since the base `Player` API doesn't expose
  `audioSessionId`). Architecturally straightforward but easy to get
  subtly wrong in a way that only shows up on a real device.
- **Synced lyrics (`.lrc`)**
- **Home-screen / lock-screen widgets** (Glance)
- **Android Auto** (the `MediaSessionService` groundwork is already in
  place for this)
- **Tag/metadata editor**
- **Listening stats**
- **A-B repeat**
- A real Play Store–size (512×512) icon export

## Branding

iSokoVibe.com.ng was unreachable from the sandbox this was built in (the
environment's network egress is allowlisted and the domain wasn't on it),
so the palette and launcher icon were built directly from the logo/brand
art files provided in chat, with colors sampled pixel-for-pixel rather
than eyeballed:

| Token | Hex | Sampled from |
|---|---|---|
| `VibeRed` | `#D50000` | the circle in the "iSo" mark |
| `VibeRedBright` | `#FF0008` | headphone band / music notes in the promo art |
| `VibeRedDeep` | `#AA0000` | promo-art background red |
| `VibeBlack` | `#000000` | icon background |

The launcher icon (`mipmap-anydpi-v26/ic_launcher.xml` + per-density
PNGs) is generated straight from the provided logo file — a solid black
adaptive background with the logo inset as the foreground layer, checked
against a worst-case circular mask so nothing gets clipped. A separate,
tightly-cropped copy (`drawable-nodpi/ic_isokovibe_logo.png`) is used for
the in-app top bar and Settings' about section, since the adaptive-icon
foreground has masking padding baked in that makes it look too small at
badge size.

If the actual website turns out to use a different palette or wordmark,
swap `app/src/main/java/com/isokovibe/musicplayer/ui/theme/Color.kt` and
the logo PNGs — everything else reads from those.

## Project layout

```
app/src/main/java/com/isokovibe/musicplayer/
├── MainActivity.kt          # Compose entry point, bottom-nav + branded chrome, nav host
├── MainViewModel.kt         # Library/search/sort/favorites/playlists/playback state
├── data/
│   ├── Song.kt               # Track model
│   ├── MusicRepository.kt    # MediaStore query
│   ├── Playlist.kt           # Playlist model (serializable)
│   ├── SortOption.kt
│   ├── ThemeMode.kt
│   └── UserDataRepository.kt # DataStore-backed favorites/playlists/theme
├── playback/
│   ├── MusicService.kt       # MediaSessionService hosting ExoPlayer
│   └── PlaybackController.kt # MediaController wrapper: queue, speed, sleep timer
└── ui/
    ├── LibraryScreen.kt
    ├── PlaylistsScreen.kt
    ├── PlaylistDetailScreen.kt
    ├── SettingsScreen.kt
    ├── NowPlayingScreen.kt
    ├── theme/                 # Color.kt, Theme.kt, Type.kt
    └── components/            # BrandTopBar, AlbumArt, MiniPlayer, AddToPlaylistDialog
```

## Building

Open the project root in Android Studio (Koala or newer) — it will
generate the Gradle wrapper and sync automatically. To build from the
command line instead:

```bash
gradle wrapper --gradle-version 8.7   # one-time, generates gradlew
./gradlew assembleDebug
```

CI (`.github/workflows/build-apk.yml`) builds a debug APK on every push
to a `claude/**` or `main` branch and publishes it to a GitHub Release, so
you don't need a local Android SDK to get an installable build.

## Roadmap / suggested next steps

1. Equalizer + ReplayGain volume normalization
2. Synced lyrics (`.lrc`)
3. Home-screen & lock-screen widgets
4. Android Auto support
5. Tag/metadata editor
6. Listening stats / "recently played" smart playlist
7. A-B repeat
8. M3U playlist import/export
9. A real Play Store–size (512×512) icon export
