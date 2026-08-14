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
- Bundled Roboto (`res/font/`) so text always renders in Roboto instead of
  whatever an OEM skin substitutes for the system font; every
  `Typography` role is set explicitly (not just the ones this app touches
  directly) so buttons/chips/labels get it too
- Every `ColorScheme` role is set explicitly, not just primary/surface —
  Material3 auto-derives the roles you leave unset from its purple
  baseline palette, which is what was leaking through as a lilac tint on
  selected nav items and chips
- Instant screen transitions (Compose Navigation's default fade animation
  is disabled) for a snappier feel
- Spinning "vinyl" album art while playing, togglable in Settings
- Mini player gets a Previous button alongside Play/Pause and Next, and
  its background color is customizable from Settings (a few brand-red/
  black/white presets)
- Sticky footer ad slot with **two ad sources**, in priority order:
  1. **Your own ads** — local business / affiliate banners you manage
     yourself via the iSokoVibe Custom Ads WordPress plugin (see "Ads
     setup" below). No app rebuild needed to add/change/remove one.
  2. **AdMob** — falls back to this when you have no ads of your own
     configured. Ships with Google's official test ad unit by default so
     it builds and shows test ads out of the box; swap in a real AdMob
     ad unit ID from Settings once there's an AdMob account behind it.
- Push notifications for new iSokoVibe.com.ng content — code is fully
  wired (Android FCM client + topic subscription + a WordPress plugin
  that sends the push when a post is published), waiting only on
  `app/google-services.json` from your Firebase project. See
  "Push notifications setup" below.

## Ads setup

Two independent ad sources share one footer slot — your own ads win
whenever there's at least one, AdMob is the fallback.

### Your own ads (local/affiliate)

1. Install the plugin in `wordpress-plugin/isokovibe-custom-ads/` on
   iSokoVibe.com.ng (same install steps as the push-notifications
   plugin — zip and upload, or FTP, then activate).
2. In wp-admin, a new **App Ads** menu appears. Add New Ad → set the
   featured image to the banner graphic, fill in the click-through URL,
   optionally flag it "Sponsored" (do this for affiliate links — most
   affiliate programs require the disclosure), publish.
3. That's it — every app install picks it up automatically (polled on
   launch, cached for offline use). Settings → Your own ads shows how
   many are currently loaded, with a manual Refresh button and an
   editable feed URL if you ever move the site.
4. To stop showing an ad, switch it to Draft in WordPress — no app-side
   action needed.

Full details, including what makes an ad "ready" to show, are in that
folder's README.

### AdMob (to make it show real, paying ads)

Right now the app ships with **Google's official test ad unit**, so
what you see in test builds are Google's sample ads, not real ones —
this is intentional (lets the banner work out of the box without an
AdMob account, and avoids accidentally serving live ads from a dev
build). To switch to real ads:

1. Create an account at [admob.google.com](https://admob.google.com/)
   if you don't have one, and add an app for iSokoVibe Music Player
   (package name `com.isokovibe.musicplayer`). AdMob doesn't require a
   Play Store listing to generate IDs, though full ad serving/payouts
   typically expect one eventually.
2. Create a **Banner** ad unit under that app — this gives you an ad
   unit ID like `ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY`.
3. In the app: **Settings → AdMob**, paste that ID into "AdMob ad unit
   ID" and save. No rebuild needed — this part is a runtime setting.
4. The **App ID** (a separate, app-level identifier — different from
   the ad unit ID) is still Google's test one, hardcoded in
   `AndroidManifest.xml`. This one *does* need a rebuild: send me your
   real AdMob App ID (`ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY`, note
   the `~` not `/`) and I'll swap it in.
5. "Manage it easily" day-to-day: Settings → AdMob has the on/off
   toggle and the ad unit ID field — no code changes needed for either.
   Everything else (impressions, revenue, payment threshold) is managed
   from the AdMob console itself, same as any AdMob-integrated app.

## Push notifications setup

Two pieces, both already written:

1. **Android app** (`app/src/main/java/.../push/`) — subscribes to the
   FCM topic `isokovibe_new_music` when a user turns on "Notify me about
   new music" in Settings, and shows a notification when a push arrives
   on that topic. Inert until `app/google-services.json` exists (the
   Gradle build checks for the file and skips applying the Firebase
   plugin if it's missing, so the build stays green either way).

   To activate: in the [Firebase console](https://console.firebase.google.com/),
   add an Android app to your project with package name
   `com.isokovibe.musicplayer`, download the resulting
   `google-services.json`, and drop it in `app/`. It's safe to commit —
   [per Google's own docs](https://firebase.google.com/docs/projects/learn-more#config-files-objects)
   it holds identifiers, not secrets. Anyone with an existing install of
   an earlier debug build (package name had a `.debug` suffix, since
   removed to match what gets registered in Firebase) will need to
   uninstall and reinstall once this lands.

2. **WordPress plugin** (`wordpress-plugin/isokovibe-push-notifications/`)
   — sends the actual push whenever a post is published, via the FCM
   HTTP v1 API (hand-signed JWT + OAuth2, no Composer dependencies, so it
   runs on ordinary WordPress hosting). Install it on iSokoVibe.com.ng,
   then in wp-admin go to Settings → iSokoVibe Push and paste in a
   Firebase **service account** JSON (different file from
   `google-services.json` — generate it from Firebase console → Project
   settings → Service accounts). Full instructions in that folder's
   README.

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

Push notifications are no longer in this list — see "Push notifications
setup" above, they just need `app/google-services.json` to go live.

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
│   ├── CustomAd.kt           # Your-own-ads model (serializable)
│   ├── CustomAdsRepository.kt # Fetches the ads feed from WordPress
│   ├── SortOption.kt
│   ├── ThemeMode.kt
│   └── UserDataRepository.kt # DataStore-backed favorites/playlists/theme/ad prefs
├── playback/
│   ├── MusicService.kt       # MediaSessionService hosting ExoPlayer
│   └── PlaybackController.kt # MediaController wrapper: queue, speed, sleep timer
├── push/
│   ├── PushNotificationManager.kt   # FCM topic subscribe/unsubscribe
│   └── IsokoVibeMessagingService.kt # Shows the notification when a push arrives
└── ui/
    ├── LibraryScreen.kt
    ├── PlaylistsScreen.kt
    ├── PlaylistDetailScreen.kt
    ├── SettingsScreen.kt
    ├── NowPlayingScreen.kt
    ├── theme/                 # Color.kt, Theme.kt, Type.kt
    └── components/            # BrandTopBar, AlbumArt, MiniPlayer, AddToPlaylistDialog,
                                # BannerAdView (AdMob), CustomAdCarousel (your own ads)

app/src/main/res/font/         # Bundled Roboto TTFs (Apache 2.0)
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
6. Listening stats / "recently played" smart playlist
7. A-B repeat
8. M3U playlist import/export
9. A real Play Store–size (512×512) icon export
