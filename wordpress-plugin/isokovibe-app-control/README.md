# iSokoVibe App Control

Controls the iSokoVibe Music Player app from WordPress. Three things, one
plugin:

1. **Announcements** — send a message to everyone with the app installed
2. **App Update** — prompt users to install a newer build
3. **Banners** — the 330×70 promo strip inside the app

## Install

1. Zip the `isokovibe-app-control` folder (the folder itself, not its
   contents) and upload it via **Plugins → Add New → Upload Plugin**, or
   copy the folder to `wp-content/plugins/` over FTP.
2. Activate it. An **App Control** menu appears in wp-admin.

Nothing else to configure — the app finds everything through one public,
read-only endpoint:

```
GET https://isokovibe.com.ng/wp-json/isokovibe/v1/app-config
```

Open that URL in a browser after activating; you should see JSON. If you
get a 404, go to **Settings → Permalinks** and click Save (that rebuilds
WordPress's routing table, which is all it usually needs).

---

## 1. Announcements

**App Control → New Announcement.**

- **Title** → the notification's heading
- **Body** → the notification's text
- **Link** → where tapping it goes (optional)

Publish to send. Save as draft and nothing happens.

Example:

> **Title:** New from Evang Ofano
> **Body:** Evang Ofano has just released a new song — listen now.
> **Link:** `https://isokovibe.com.ng/evang-ofano-new-song`

**Only announcements you publish here reach the app.** Ordinary blog posts
never do — that was the point of building this rather than reusing the
post feed.

### How delivery works, and how fast

The app checks this endpoint **roughly every 6 hours**, plus every time it
is opened. So an announcement reaches most people within a few hours, not
instantly, and Android may stretch that on a phone that's dozing or in
battery saver.

That's a deliberate trade. Instant delivery means Firebase Cloud
Messaging, which needs a Firebase project, a `google-services.json` file
built into the app, and a service-account key installed on the server —
all of which must be correct before a single message sends. This approach
needs none of that: publish, and phones pick it up. If you later decide
the delay matters, FCM can be added alongside this without replacing it.

**Only the newest announcement is served.** Publishing a second one
supersedes the first rather than queueing a backlog, so a phone that was
offline for a week gets one notification rather than seven at once.

---

## 2. App Update

**App Control → App Update.**

When you publish a new build of the app, fill in:

| Field | What it does |
|---|---|
| **Latest version code** | A whole number that only goes up. Must match the version code of the build you published. |
| **Version name** | Shown to the user, e.g. `0.1.20` |
| **Download URL** | Where "Update" sends people |
| **What's new** | Optional note shown in the prompt |
| **Required update** | Removes the "Later" button |

The app compares this against its own version code. Higher here means a
prompt; equal or lower means nothing. Users who tap "Later" aren't asked
again for that version.

> **On "Required update":** this leaves users no way past the prompt.
> Anyone who can't download right then — no data, no space, travelling —
> is locked out of their own offline music library until they can. Worth
> reserving for genuinely broken builds.

**Where to find the version code:** each CI build's GitHub release names
it (`Version code 17`), and it matches the release tag number
(`debug-build-17` → `17`).

---

## 3. Banners

**App Control → App Banners → New Banner.**

- **Featured image** → the artwork (this is the banner)
- **Click-through URL** → where tapping goes
- **Where in the app** → header, footer, or both
- **Sponsored** → shows a small "Sponsored" label

Publish to go live. Switch to Draft to pull it immediately — no app update
needed either way.

### Artwork size

Design at **330×70**, or any 33:7 ratio. **660×140 is better** — it looks
sharper on high-density screens, which is most phones now.

The app scales the image to the screen width and keeps the 33:7 ratio, so
artwork far off that shape gets letterboxed rather than cropped or
stretched.

### Multiple banners

Several banners can target the same slot; the app rotates through them
every 8 seconds. That way multiple clients share the space instead of
whoever was published first holding it permanently.

### A note on using both slots

Running header *and* footer banners at once costs roughly 140dp of
vertical space on every screen — on a typical phone that's around a fifth
of the display, on top of the app's own title bar, tab bar, and mini
player. It works, and it's your call, but the library list gets noticeably
cramped. Starting with the footer only and adding the header when a
campaign justifies it is the gentler option.

Users can switch banners off entirely in the app's Settings. That's
deliberate — an ad strip with no off switch tends to cost you installs.
