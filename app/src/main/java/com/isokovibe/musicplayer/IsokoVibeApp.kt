package com.isokovibe.musicplayer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.android.gms.ads.MobileAds

const val UPDATES_NOTIFICATION_CHANNEL_ID = "isokovibe_updates"

class IsokoVibeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            UPDATES_NOTIFICATION_CHANNEL_ID,
            "New music & site updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Lets you know when new music is posted on iSokoVibe.com.ng"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
