# iSokoVibe Push Notifications (WordPress plugin)

Sends a Firebase Cloud Messaging push to the iSokoVibe Android app every
time a post is published on iSokoVibe.com.ng. Single file, no Composer
dependencies — works on ordinary shared/managed WordPress hosting (needs
the PHP `openssl` extension, which is virtually always available).

## Install

1. Zip this folder (`isokovibe-push-notifications/`) or upload it as-is
   to `wp-content/plugins/isokovibe-push-notifications/` via FTP/SFTP/your
   host's file manager.
2. In wp-admin: **Plugins → Installed Plugins**, activate
   "iSokoVibe Push Notifications".

## Configure

1. In the [Firebase console](https://console.firebase.google.com/),
   open your project → gear icon → **Project settings** →
   **Service accounts** tab → **Generate new private key**. This
   downloads a JSON file — keep it private (it grants your app the
   ability to send pushes as your Firebase project).
2. In wp-admin: **Settings → iSokoVibe Push**.
3. Paste the entire contents of that JSON file into the
   "Firebase service account JSON" box.
4. Check **Enabled**, pick which post types should trigger a
   notification (defaults to regular Posts), and save.
5. Click **Send a test notification** to confirm it's wired up —
   check a phone with the app installed and "Notify me about new
   music" turned on in Settings.

From then on, every time you publish a post (of a checked post type),
everyone with notifications turned on in the app gets a push.

## How it works

- The Android app subscribes to the FCM topic `isokovibe_new_music`
  when the user enables "Notify me about new music" in Settings — no
  per-device token tracking needed on the WordPress side.
- This plugin hooks `transition_post_status`, and once a post's status
  becomes `publish` (and wasn't already), it signs a short-lived OAuth2
  token with your service account's private key and calls the
  [FCM HTTP v1 API](https://firebase.google.com/docs/cloud-messaging/migrate-v1)
  to push a notification to that topic.

## Notes

- The service account JSON is stored as a WordPress option (`wp_options`
  table) — anyone with admin access to the site can read it from the
  settings screen. Treat wp-admin access accordingly.
- If notifications stop working after a Firebase key rotation, generate
  a new service account key and paste it in again.
