# iSokoVibe Custom Ads (WordPress plugin)

Lets you manage local business / affiliate ad banners shown in the
iSokoVibe Android app, from a normal WordPress admin screen — no
per-device settings, no app rebuild. Change an ad here, everyone using
the app sees it on their next refresh (roughly every app open, cached
for 5 minutes).

## Install

1. Zip this folder (`isokovibe-custom-ads/`) or upload it as-is to
   `wp-content/plugins/isokovibe-custom-ads/` via FTP/SFTP/your host's
   file manager.
2. In wp-admin: **Plugins → Installed Plugins**, activate
   "iSokoVibe Custom Ads".

## Add an ad

1. In wp-admin, a new **App Ads** menu appears (megaphone icon) →
   **Add New Ad**.
2. Give it a title (for your own reference — not shown in the app).
3. Set the **Featured Image** (right sidebar) to the ad banner. A wide
   image works best — roughly 1200×200px.
4. Fill in the **Click-through URL** — where tapping the ad opens a
   browser to. Your own page, an affiliate link, whatever you're
   promoting.
5. Optionally check **Show a "Sponsored" label** — turn this on for
   affiliate links; it's standard practice to disclose sponsored
   content, and required by most affiliate programs' terms.
6. Optionally adjust **Priority** if you're running several ads at once
   (lower number = shows more often). Defaults to 10.
7. Click **Publish**. It's now live in the app.

To stop showing an ad, switch it back to Draft or move it to Trash — no
need to delete it if you might reuse it later.

The **All Ads** list shows a "Ready?" column flagging any ad that's
missing its image or URL, so nothing goes out half-configured.

## How it works

- The app polls `GET /wp-json/isokovibe/v1/ads` (a plain, public, no
  Firebase/auth needed. See below.
- Only `publish`-status ads are returned, ordered by priority.
- The app caches the last successful response, so ads still show
  (from cache) if a device is briefly offline.
- If this endpoint returns nothing (no ads configured, or the plugin
  isn't installed), the app falls back to its AdMob banner instead —
  see Settings → Ads in the app for that side of things.

## Notes

- The REST endpoint is intentionally public/unauthenticated (`GET`
  only, read-only, no user data) — that's what lets the app fetch it
  without any login. It only ever returns what you've published here.
- Images are served straight from your WordPress media library, so
  keep them reasonably sized (a few hundred KB) for fast loading.
