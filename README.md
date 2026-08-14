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
- **Library** — search, sort (title/artist/album/duration/**recently
  played**/**most played**), a Favorites filter chip, and a manual **Scan
  library** button (next to search) to re-query `MediaStore` on demand —
  for when you've dropped new files on the device and don't want to
  relaunch the app
- **Library filters** — skip clips shorter than a chosen length (15s–90s,
  or off) and/or skip anything that looks like a **WhatsApp voice note**
  (by folder or `PTT-`/`AUD-...-WA` filename pattern — on by default, some
  devices index these as regular music). Both are in Settings > Library
  and re-scan the library immediately when changed
- **Favorites** — heart-toggle on any track, from the library list or Now
  Playing
- **Playlists** — create, rename, delete, add/remove tracks, play a
  playlist from any track or from the top; persisted locally via DataStore
- **Now Playing** — seek bar, play/pause, skip, shuffle, repeat
  (off → all → one), favorite toggle, sleep timer, playback speed, a
  marquee-scrolling title for long track names, and an **Up next** sheet
  showing the rest of the current queue (tap any track to jump to it)
- **Sleep timer** — 15/30/45/60 minutes, pauses playback and counts down
  live; adjustable from Settings or Now Playing's overflow menu
- **Playback speed** — 0.75x–2x, from Settings or Now Playing
- **Haptic feedback** on Play/Pause and Favorite, on both the mini player
  and Now Playing
- **Settings** — theme mode (dark default/Light/Follow system), 5
  selectable **color skins** (Vibe Red, Ocean Blue, Emerald Green, Royal
  Purple, Sunset Amber), font customization (see below), playback speed,
  sleep timer, mini player color, library filters, about section, and an
  honest "coming soon" list for what's not built yet
- Runtime permission handling for `READ_MEDIA_AUDIO` (Android 13+) /
  `READ_EXTERNAL_STORAGE` (older)
- Branding pulled from the real iSokoVibe logo/brand art (see below); the
  app **defaults to the brand's dark red-on-black look** regardless of
  system theme, and Material You dynamic color is intentionally never
  used for any skin, so the app always reads as itself rather than
  tinting to the phone's wallpaper
- **Fonts** — bundled Roboto and Open Sans (both Apache 2.0), so text
  never falls back to whatever an OEM skin substitutes for the system
  font. Open Sans is bundled specifically for its Light "book" (300) cut,
  used on smaller secondary text. Settings > Fonts lets you pick:
  - **Combination** — Roboto, Roboto + Open Sans (default: Roboto on
    titles, Open Sans Light on everything smaller), or Open Sans throughout
  - **Size** — Small/Default/Large, a multiplier over the whole type scale
  - **Weight** — Light/Default/Bold, shifting the bold-title/light-detail
    contrast a notch either way

  Every `Typography` role is set explicitly (not just the ones touched
  directly) so buttons/chips/labels pick up the choice too, and primary
  text (song/playlist titles) is always bold relative to secondary text
  (artist, album, counts) regardless of which weight profile is active
- Every `ColorScheme` role is set explicitly for every skin, not just
  primary/surface — Material3 auto-derives roles left unset from its
  purple baseline palette, which is what was leaking through as a lilac
  tint on selected nav items and chips before this was fixed
- Instant screen transitions (Compose Navigation's default fade animation
  is disabled) for a snappier feel
- Mini player gets a Previous button alongside Play/Pause and Next, its
  background color is customizable from Settings (a few brand-red/black/
  white presets), and the track title marquees when it's too long to fit
- Margins trimmed down across every screen (search bar, song/playlist
  rows, Settings sections) so content runs edge-to-edge instead of
  looking boxed in

## Ads and push notifications — removed from the app for now

The app was getting heavier than it needed to be, so the AdMob banner,
the in-app "your own ads" carousel, the spinning album-art animation,
and push notifications have all been stripped out of the Android app
(no more `play-services-ads`/Firebase SDKs, no ad/notification code
paths, no related Settings sections). The two WordPress plugins are
**left untouched in `wordpress-plugin/`** — since ads are now being
managed entirely from the WordPress side, `isokovibe-custom-ads` is
usable as-is on the site even without the in-app carousel consuming its
feed. `isokovibe-push-notifications` still works as a "send on publish"
plugin, it just has no Android client subscribed to the topic right
now. Both are ready to wire back into the app later if you want them
back — say the word and I'll re-add the app-side integration without
touching the plugins.

## Deferred — needs your input or device-level testing to get right

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

Ads and push notifications aren't in this list because they're not being
built toward right now by design — see "Ads and push notifications" above.

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
│   ├── Song.kt                # Track model
│   ├── MusicRepository.kt     # MediaStore query — duration filter + WhatsApp voice note detection
│   ├── Playlist.kt            # Playlist model (serializable)
│   ├── SortOption.kt          # Title/Artist/Album/Duration/Recently played/Most played
│   ├── ThemeMode.kt
│   ├── AppearanceSettings.kt  # ColorSkin, FontCombination, FontSizeScale, FontWeightPreference, MinTrackDuration
│   └── UserDataRepository.kt  # DataStore-backed prefs: favorites/playlists/appearance/library filters/play stats
├── playback/
│   ├── MusicService.kt        # MediaSessionService hosting ExoPlayer
│   └── PlaybackController.kt  # MediaController wrapper: queue, speed, sleep timer
└── ui/
    ├── LibraryScreen.kt
    ├── PlaylistsScreen.kt
    ├── PlaylistDetailScreen.kt
    ├── SettingsScreen.kt
    ├── NowPlayingScreen.kt       # Includes the "Up next" queue bottom sheet
    ├── theme/                    # Color.kt (5 skins), Theme.kt (builds ColorScheme per skin), Type.kt (builds Typography)
    └── components/                # BrandTopBar, AlbumArt, MiniPlayer, AddToPlaylistDialog

app/src/main/res/font/         # Bundled Roboto + Open Sans TTFs (Apache 2.0)
wordpress-plugin/
├── isokovibe-push-notifications/  # Sends a push on publish
└── isokovibe-custom-ads/          # "App Ads" admin screen + the feed the app polls
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
6. Richer listening stats (charts/trends) — basic recently-played/most-played
   sorting is already shipped, this would be a dedicated stats screen
7. A-B repeat
8. M3U playlist import/export
9. A real Play Store–size (512×512) icon export
10. Drag-to-reorder in the "Up next" queue sheet (currently tap-to-jump only)
