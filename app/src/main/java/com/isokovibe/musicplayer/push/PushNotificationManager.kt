package com.isokovibe.musicplayer.push

import com.google.firebase.messaging.FirebaseMessaging

/** Must match ISOKOVIBE_PUSH_TOPIC in the WordPress plugin. */
const val NEW_MUSIC_TOPIC = "isokovibe_new_music"

/**
 * Subscribes/unsubscribes this device to the "new music" FCM topic —
 * topic-based, so there's no per-device token database to run anywhere.
 *
 * Safe to call before `google-services.json` is added: Firebase throws if
 * no app is configured yet, which this just swallows (no-op) rather than
 * crashing.
 */
object PushNotificationManager {
    fun setSubscribed(subscribed: Boolean) {
        val messaging = runCatching { FirebaseMessaging.getInstance() }.getOrNull() ?: return
        if (subscribed) {
            messaging.subscribeToTopic(NEW_MUSIC_TOPIC)
        } else {
            messaging.unsubscribeFromTopic(NEW_MUSIC_TOPIC)
        }
    }
}
