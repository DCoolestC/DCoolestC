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
- **Browse tabs** — Songs / Albums / Artists / Genres / Folders, with a
  grid or list view for albums and one search box that narrows whichever
  tab is open. All four browse tabs drill into a shared detail screen with
  Play and Shuffle. Groups are derived from the already-filtered song list,
  so an album made entirely of tracks you've hidden doesn't appear.
  (Genres need Android 11+, where the platform first exposes genre tags.)
- **Favorites** — heart-toggle on any track, from the library list or Now
  Playing
- **Playlists** — create, rename, delete, add/remove tracks, play a
  playlist from any track or from the top; persisted locally via DataStore
- **Smart playlists** — Favorites, Recently added, Recently played, Most
  played, Never played. Derived from rules rather than stored, so they
  never go stale
- **Duplicate finder** — Settings → Library. Groups by title + artist and
  shows each copy's folder and duration. Read-only: deleting means writing
  to your music files, which is scheduled with the tag editor so the same
  file-write machinery gets built and tested once
- **Queue control** — long-press-drag to reorder, remove individual
  tracks, "Play next" / "Add to queue" from any song's menu, and save the
  current queue as a playlist
- **Resume where you left off** — the queue and position are restored on
  launch, prepared but not auto-played. Tracks 10 minutes or longer also
  get their own resume point, so a long mix picks up where it stopped
- **A-B repeat** — set two points and loop between them; the menu item
  relabels itself to whichever step is next
- **Equalizer & effects** — Settings → Sound. Bands are generated from
  what the device reports (Android promises no fixed count — most hardware
  gives five), plus presets, bass boost, virtualizer, reverb, volume boost
  and skip-silence
- **Now Playing** — seek bar, play/pause, skip, shuffle, repeat
  (off → all → one), favorite toggle, sleep timer, playback speed, a
  marquee-scrolling title for long track names, and an **Up next** sheet
  showing the rest of the current queue (tap to jump, drag to reorder)
- **Sleep timer** — 15/30/45/60 minutes or "at end of this track", with
  the volume easing down over the final 15 seconds rather than cutting out
  mid-bar; adjustable from Settings or Now Playing's overflow menu
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
- **Fonts** — four bundled families, so text never falls back to whatever
  an OEM skin substitutes for the system font: **Roboto** (Apache 2.0),
  **Open Sans** (Apache 2.0), **Montserrat** (OFL-1.1) and **Lato**
  (OFL-1.1). Montserrat is the "solid" face — geometric and heavy, echoing
  the chunky poster lettering in iSokoVibe's brand art. Open Sans and Lato
  are both bundled for their Light "book" (300) cuts, used on smaller
  secondary text. Settings > Fonts lets you pick:
  - **Combination** — 8 options. Pairs ("A + B") put A on titles and a
    light book face on the smaller text, which is where the contrast
    actually reads:

    | Option | Character |
    |---|---|
    | Roboto | Neutral |
    | Roboto + Open Sans *(default)* | Neutral + book |
    | Roboto + Lato | Neutral + book |
    | Open Sans | Book |
    | Lato | Book |
    | Montserrat | Solid |
    | Montserrat + Open Sans | Solid + book |
    | Montserrat + Lato | Solid + book |

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
- Marquee scrolling loops continuously (`iterations = Int.MAX_VALUE`) on
  both the mini player and Now Playing's title/artist. Compose's
  `basicMarquee()` defaults to only 3 passes and then parks the text
  mid-scroll, which reads as the feature being broken on a screen you sit
  and look at
- Compact search field — built on `BasicTextField` rather than Material3's
  `OutlinedTextField`, which enforces a 56dp minimum height that can't be
  overridden. It's 40dp here, with a clear ("×") button once you've typed
- Margins trimmed down across every screen (search bar, song/playlist
  rows, Settings sections) so content runs edge-to-edge instead of
  looking boxed in

## Controlling the app from iSokoVibe.com.ng

Install **`wordpress-plugin/isokovibe-app-control/`** and three things
become controllable from wp-admin without touching the app. Full setup
instructions are in that folder's README; the short version:

| Feature | Where | What it does |
|---|---|---|
| **Announcements** | App Control → New Announcement | Publish a title + body + link and phones show a notification. Only these reach the app — ordinary blog posts never do. |
| **App Update** | App Control → App Update | Set a version code/name/download URL; anything above the installed build prompts users to update from the site. |
| **Banners** | App Control → App Banners | 33:7 promo artwork with a link, placed in the header, footer, or both. Publish to go live, draft to pull. |

Everything is served from one public read-only endpoint,
`/wp-json/isokovibe/v1/app-config`, so a phone makes one request rather
than three and the features can't be read in inconsistent states.

**Announcements arrive by polling, not push.** The app checks roughly
every six hours and whenever it's opened, so delivery is within hours
rather than seconds. Firebase Cloud Messaging would be instant but needs
a Firebase project, a `google-services.json` compiled into the app, and a
service-account key on the server — all of which must be correct before a
single message sends. This needs none of them. FCM can be added alongside
later if the delay becomes a problem.

Users can switch announcements and banners off independently in Settings.

### The older plugins

`isokovibe-push-notifications` and `isokovibe-custom-ads` predate this and
are **superseded by App Control** — they were built when notifications
fired automatically on every post, which turned out not to be what was
wanted. They're left in the repo rather than deleted in case anything on
the site still references them, but App Control is the one to install.

## Signing & updates

New builds install **straight over** an older iSokoVibe build — no need to
uninstall first, and playlists/favorites/settings survive the update.

That works because of two things:

1. **A pinned signing key** (`app/isokovibe-debug.keystore`, wired up in
   `app/build.gradle.kts`). Android refuses to install an APK over an app
   signed by a different key — it reports a signature mismatch, and the
   only way through is to uninstall, which wipes your data with it.
   Gradle's default is to auto-generate a throwaway debug keystore on
   whatever machine is building, and CI runners are wiped between jobs, so
   before this key existed *every single CI build was signed by a
   different key* — hence having to delete the app each time.
2. **An increasing `versionCode`**, taken from the CI run number
   (`-PisokoVersionCode=...`). Android won't install an APK whose version
   code is lower than what's already installed, so each build being
   strictly newer keeps updates one-way and predictable.

> **Before publishing to the Play Store:** the committed key is a *test*
> key — its password is in the build file and the repo is public, so treat
> it as public too. A real release needs a separate keystore that is never
> committed (keep it in GitHub Actions secrets or offline). Losing a
> published app's release key means you can never update that listing
> again, so back it up somewhere durable. Ask me and I'll wire up a
> secrets-based release signing config when you're ready to ship.

## Premium feature roadmap

Everything below is buildable on the current architecture. Pick the ones
you want and I'll build them.

> **Progress so far.** Sections 1 (library structure), 2 (audio engine)
> and 3 (playback & queue) are **built and shipping** — see "What's
> implemented" above for the detail. Still open: §4 lyrics & metadata,
> §5 interface & personalization, §6 reach (widgets/Auto/Wear/cast),
> §7 insight & history, §8 data safety & store readiness.
>
> Four items inside the shipped sections are deliberately still open, each
> for a stated reason rather than an oversight: **crossfade** (needs a
> two-player rework of the service — a non-overlapping fade would be
> mislabelling it), **ReplayGain** (needs per-file tag parsing; the volume
> boost that shipped is a flat gain, not that), **mono/balance** (needs a
> custom `AudioProcessor`), and **duplicate deletion** (writes to your
> music files, so it's grouped with the tag editor). One turned out to
> need no work at all: **pitch-preserving speed** — ExoPlayer's default
> Sonic processor already time-stretches without shifting pitch, so the
> existing speed control has always preserved pitch.

**Effort** is my estimate of how much work a feature is end to end:
**S** = a single session · **M** = a few sessions · **L** = substantial,
worth splitting across several rounds.

**Risk** flags how confidently I can ship it from a CI-only build loop:
**✅** = CI compiling it is genuinely good evidence it works ·
**⚠️** = touches audio hardware, system integration, or timing, so it can
compile clean and still be subtly wrong until you run it on your phone.
I'll say so on ⚠️ items rather than implying they're verified.

### 1. Library structure & browsing — *the biggest gap*

The library is currently one flat, sortable list of songs. Every other
serious player lets you come at your music from several directions, and
this is the single change that would most make the app feel premium.

| Feature | Effort | Risk | Notes |
|---|---|---|---|
| **Albums / Artists / Genres tabs** | M | ✅ | Browse by album or artist with art grids, drill into a detail screen. `MediaStore` already returns the album/artist IDs — nothing new to scan. |
| **Folder browsing** | M | ✅ | Navigate the actual on-disk tree. The one people miss most from file-manager-style players, and it pairs well with the existing library filters. |
| **Album art grid view** | S | ✅ | Toggle between list and grid, with the grid keyed off album art. |
| **Multi-select + batch actions** | M | ✅ | Long-press to select several tracks, then queue/add-to-playlist/delete in one go. |
| **Smart playlists** | M | ✅ | Auto-updating rules — "most played", "added this month", "never played", "favourites by artist X". The play-count/last-played data this needs is already being recorded. |
| **Duplicate finder** | S | ✅ | Group by title+artist+duration and let you clear out re-downloads. |
| **Folder allow/deny list** | S | ✅ | A natural extension of the current "skip short clips / skip WhatsApp voice notes" filters — exclude whole directories (Downloads, Recordings) from the library. |
| **M3U / PLS import & export** | S | ✅ | Playlists currently live only inside the app's own storage. This makes them portable — bring playlists in from another player, or take yours with you. |

### 2. Audio engine & sound quality

The headline gap for anyone who cares about how it *sounds*. All of these
route through ExoPlayer's audio pipeline.

| Feature | Effort | Risk | Notes |
|---|---|---|---|
| **10-band equalizer + presets** | M | ⚠️ | Needs `android.media.audiofx.Equalizer` bound to ExoPlayer's audio session **across the `MediaController`/`MediaSession` process boundary** — the base `Player` API doesn't expose `audioSessionId`, so it takes a custom session command. Straightforward in shape, easy to get subtly wrong in ways only a real device reveals. |
| **Bass boost / virtualizer / reverb** | S | ⚠️ | Same plumbing as the equalizer; cheap to add once that exists. Do these together. |
| **ReplayGain normalization** | M | ⚠️ | Levels out volume between tracks from different sources. Reads the gain tag where present, falls back to measured loudness. |
| **Crossfade between tracks** | M | ⚠️ | Overlapping fade on transition. Genuinely fiddly — interacts with gapless, and getting it wrong produces audible glitches. |
| **Gapless playback** | S | ⚠️ | ExoPlayer largely handles this; mostly about not breaking it in the queue logic and verifying on real files. |
| **Skip silence** | S | ⚠️ | ExoPlayer has built-in support — a near-free win once wired to a setting. |
| **Mono / balance** | S | ⚠️ | Accessibility-oriented, small surface. |
| **Pitch-preserving speed** | S | ⚠️ | Playback speed already works; this keeps voices natural at 1.5×, which matters if you ever play spoken-word content. |

### 3. Playback & queue

| Feature | Effort | Risk | Notes |
|---|---|---|---|
| **Drag-to-reorder the queue** | M | ✅ | The Up Next sheet is tap-to-jump only today. |
| **"Play next" / "Add to queue"** | S | ✅ | Per-track menu actions — a core interaction that's currently missing. |
| **Save queue as playlist** | S | ✅ | One tap from Up Next. |
| **Resume where you left off** | S | ✅ | Persist the queue + position across app restarts, so reopening picks up mid-track. High impact for how "finished" the app feels. |
| **A-B repeat** | S | ✅ | Loop a section — practice/learning use case. |
| **Sleep timer: fade out + finish track** | S | ✅ | "Stop at end of current track" and a gentle volume fade instead of an abrupt cut. |
| **Bookmarks / long-track resume** | M | ✅ | Remembers position per track for anything long-form. |

### 4. Lyrics & metadata

| Feature | Effort | Risk | Notes |
|---|---|---|---|
| **Synced lyrics (`.lrc`)** | M | ✅ | Reads sidecar `.lrc` files and embedded lyrics tags, scrolling in time with playback. Local files only — no network lookup. |
| **Tag editor** | M | ⚠️ | Edit title/artist/album/art, single or batch. ⚠️ because it *writes to the user's files* — needs scoped-storage write permissions handled carefully, and a bug here damages the music library itself. I'd want this tested hard before you trust it. |
| **Album art fetch & embed** | M | ⚠️ | Fill in missing artwork. Any automatic source means network access, which the app deliberately has none of right now — worth a conversation about whether that trade is worth it. |

### 5. Interface & personalization

| Feature | Effort | Risk | Notes |
|---|---|---|---|
| **Multiple Now Playing layouts** | M | ✅ | Two or three player designs to switch between, alongside the existing skin/font settings. |
| **Dynamic color from album art** | S | ✅ | Accent follows the current track's artwork, as an opt-in alongside the 5 fixed skins. |
| **Blurred art backdrop** | S | ✅ | Album art blurred behind the player — cheap, and reads as expensive. |
| **Gesture controls** | S | ✅ | Swipe the mini player left/right to skip, down to dismiss. |
| **Tablet / landscape layouts** | M | ✅ | Two-pane on wide screens instead of a stretched phone layout. |
| **Search history & fuzzy matching** | S | ✅ | Tolerate typos, remember recent searches. |
| **App shortcuts & Quick Settings tile** | S | ✅ | Long-press the icon for "Shuffle all"/"Resume"; a play/pause tile in the notification shade. |

### 6. Reach — beyond the phone screen

| Feature | Effort | Risk | Notes |
|---|---|---|---|
| **Home-screen widgets** (Glance) | M | ⚠️ | Several sizes. Widgets run in a different process, so they need testing on a real launcher. |
| **Android Auto** | M | ⚠️ | The `MediaSessionService` groundwork is already in place, which is most of the battle — but it can't be meaningfully verified without a head unit or the Desktop Head Unit emulator. |
| **Wear OS companion** | L | ⚠️ | Controls on the watch. Its own module and build target. |
| **Chromecast / DLNA** | L | ⚠️ | Cast to speakers/TV. Adds a networking dependency and a lot of device-specific behaviour. |

### 7. Insight & history

| Feature | Effort | Risk | Notes |
|---|---|---|---|
| **Listening stats screen** | M | ✅ | Top tracks/artists, total listening time, trends. The underlying play-count and last-played data is already being collected — this is presentation. |
| **Play history timeline** | S | ✅ | A scrollable "what I played, when". |
| **Last.fm scrobbling** | M | ⚠️ | Would reintroduce network access and account credentials to an app that currently has neither. Worth deciding deliberately. |

### 8. Data safety & store readiness

| Feature | Effort | Risk | Notes |
|---|---|---|---|
| **Backup & restore** | S | ✅ | Export playlists/favourites/settings to a file and import them back — protects everything the app knows about you when switching phones. |
| **Release signing config** | S | ✅ | Secrets-based keystore for Play Store builds. See "Signing & updates" — **required before you publish**, and worth doing early. |
| **512×512 Play Store icon** | S | ✅ | Store listing asset. |
| **Localization** | M | ✅ | Strings are already in `strings.xml`; this is translation plus RTL checking. |

### What I'd do next

The original "if you only pick three" — library browsing, equalizer,
resume where you left off — are all shipped. Of what's left:

1. **Synced lyrics** (§4) — the most visible remaining gap for a music
   player, and low-risk since it reads local `.lrc` files with no network.
2. **Home-screen widget** (§6) — high day-to-day value, though it needs
   testing on a real launcher.
3. **Backup & restore** (§8) — cheap, and it protects playlists,
   favourites and settings, none of which currently survive losing a phone.

Ads and push notifications aren't on this list — they're deliberately
parked, see "Ads and push notifications" above. The WordPress plugins are
still in the repo whenever you want them wired back in.

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
│   ├── Song.kt                # Track model, incl. album/artist/folder/genre grouping keys
│   ├── MusicRepository.kt     # MediaStore query — duration, WhatsApp voice note and folder filters
│   ├── LibraryGroups.kt       # Derives Albums/Artists/Genres/Folders from the scanned list
│   ├── SmartPlaylist.kt       # Rule-based playlists + duplicate detection
│   ├── Playlist.kt            # Playlist, SavedQueue, StoredAudioEffects (serializable)
│   ├── SortOption.kt          # Title/Artist/Album/Duration/Recently played/Most played
│   ├── ThemeMode.kt
│   ├── AppearanceSettings.kt  # ColorSkin, FontCombination, FontSizeScale, FontWeightPreference, MinTrackDuration
│   ├── RemoteConfig.kt        # Announcement / app version / banner models
│   ├── RemoteConfigRepository.kt # Single GET of the site's app-config endpoint
│   └── UserDataRepository.kt  # DataStore-backed prefs, play stats, saved queue, cached remote config
├── playback/
│   ├── MusicService.kt        # MediaSessionService hosting ExoPlayer
│   ├── AudioEngine.kt         # Equalizer/bass/virtualizer/reverb/loudness bound to the audio session
│   └── PlaybackController.kt  # MediaController wrapper: queue, speed, sleep timer, A-B repeat
├── remote/
│   └── AnnouncementWorker.kt  # Periodic check for site announcements → notification
└── ui/
    ├── LibraryScreen.kt          # Browse tabs + compact search
    ├── BrowseViews.kt            # Album grid/list, artist/genre/folder lists
    ├── GroupDetailScreen.kt      # Shared detail screen for album/artist/genre/folder/smart playlist
    ├── PlaylistsScreen.kt
    ├── PlaylistDetailScreen.kt
    ├── DuplicatesScreen.kt
    ├── EqualizerScreen.kt
    ├── SettingsScreen.kt
    ├── NowPlayingScreen.kt
    ├── QueueSheet.kt             # "Up next" with drag-to-reorder
    ├── UpdatePromptDialog.kt
    ├── theme/                    # Color.kt (5 skins), Theme.kt (builds ColorScheme per skin), Type.kt (builds Typography)
    └── components/               # BrandTopBar, AlbumArt, MiniPlayer, AddToPlaylistDialog, RemoteBannerView

app/src/main/res/font/         # Bundled Roboto + Open Sans (Apache 2.0), Montserrat + Lato (OFL-1.1)
app/isokovibe-debug.keystore   # Pinned test signing key — see "Signing & updates"
wordpress-plugin/
├── isokovibe-app-control/         # ← install this one: announcements, update prompts, banners
├── isokovibe-push-notifications/  # superseded — auto-sent on every post
└── isokovibe-custom-ads/          # superseded — earlier ad feed
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

## What to build next

See **[Premium feature roadmap](#premium-feature-roadmap)** above for the
full catalogue, grouped by area with effort estimates and honest notes on
which ones need testing on a real device before you should trust them.
