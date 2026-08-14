# iSokoVibe Music Player

A local music player for Android, built with Kotlin, Jetpack Compose, and
Media3 (ExoPlayer).

## What's in this MVP

- Scans the device's music library via `MediaStore` (no network, no
  external catalog)
- Background playback through a `MediaSessionService`, so audio keeps
  playing when the app isn't in the foreground, with lock-screen /
  notification / Bluetooth controls handled automatically by Media3
- Compose UI:
  - **Library** — scrollable list of every track on the device
  - **Mini player** — docked at the bottom while browsing
  - **Now Playing** — full-screen player with seek bar, play/pause,
    skip, shuffle, and repeat (off → all → one)
- Runtime permission handling for `READ_MEDIA_AUDIO` (Android 13+) /
  `READ_EXTERNAL_STORAGE` (older)
- Branding pulled from the real iSokoVibe logo/brand art (see below) —
  the app intentionally does **not** use Material You dynamic color, so
  it always reads as iSokoVibe rather than tinting to the phone's
  wallpaper

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
against a worst-case circular mask so nothing gets clipped. If the actual
website turns out to use a different palette or wordmark, swap
`app/src/main/java/com/isokovibe/musicplayer/ui/theme/Color.kt` and the
`mipmap-*/ic_launcher_foreground.png` assets — everything else reads from
those.

## Project layout

```
app/src/main/java/com/isokovibe/musicplayer/
├── MainActivity.kt         # Compose entry point, nav host, permission gate
├── MainViewModel.kt        # Bridges library state + playback state to the UI
├── data/
│   ├── Song.kt              # Track model
│   └── MusicRepository.kt   # MediaStore query
├── playback/
│   ├── MusicService.kt      # MediaSessionService hosting ExoPlayer
│   └── PlaybackController.kt# MediaController wrapper exposed as StateFlow
└── ui/
    ├── LibraryScreen.kt
    ├── NowPlayingScreen.kt
    ├── theme/                # Color.kt, Theme.kt, Type.kt
    └── components/MiniPlayer.kt
```

## Building

Open the project root in Android Studio (Koala or newer) — it will
generate the Gradle wrapper and sync automatically. To build from the
command line instead:

```bash
gradle wrapper --gradle-version 8.7   # one-time, generates gradlew
./gradlew assembleDebug
```

> **Note:** this scaffold was generated in a sandboxed environment without
> network access to Google's Maven repository (`dl.google.com`) or an
> installed Android SDK, so the build could not be compiled/verified here.
> Open it in Android Studio (or CI with normal network access) to resolve
> dependencies and confirm it builds — the code follows standard,
> well-established Media3 + Compose patterns, but please do a first build
> before relying on it.

## Roadmap / suggested next steps

Roughly in priority order — see the feature discussion in this PR/commit
for the fuller list:

1. Playlists (create/edit, M3U import-export) + favorites
2. Search/filter and sort options (artist, album, genre, recently added)
3. Equalizer + ReplayGain volume normalization
4. Sleep timer, playback speed, A-B repeat
5. Lyrics (`.lrc` sync)
6. Home-screen & lock-screen widgets
7. Android Auto support (the `MediaSessionService` groundwork is already
   in place for this)
8. Tag/metadata editor
9. Listening stats / "recently played" smart playlist
10. Actual launcher icon artwork (the current one is a placeholder vector)
