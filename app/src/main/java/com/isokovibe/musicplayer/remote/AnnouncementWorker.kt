package com.isokovibe.musicplayer.remote

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.isokovibe.musicplayer.MainActivity
import com.isokovibe.musicplayer.R
import com.isokovibe.musicplayer.data.RemoteConfigRepository
import com.isokovibe.musicplayer.data.UserDataRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

const val ANNOUNCEMENTS_CHANNEL_ID = "isokovibe_announcements"
private const val ANNOUNCEMENT_NOTIFICATION_ID = 4201
private const val WORK_NAME = "isokovibe_announcement_check"

/**
 * Polls the site for a new announcement and posts a notification for it.
 *
 * Polling rather than Firebase Cloud Messaging is a deliberate trade. FCM
 * delivers instantly, but requires a Firebase project, a `google-services.json`
 * in the app, and a service-account key on the server — three pieces of
 * setup that have to be got right before a single message can be sent. This
 * needs none of them: publish an announcement in WordPress and phones pick
 * it up on their next check. The cost is latency — delivery lands within a
 * few hours rather than seconds, and Android may stretch that further on a
 * dozing phone. For "a new song is up", that's a fair trade; if instant
 * delivery is ever needed, FCM can be added alongside this without
 * replacing it.
 */
class AnnouncementWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userData = UserDataRepository(applicationContext)
        if (!userData.remoteUpdatesEnabled.first()) return Result.success()

        val config = RemoteConfigRepository()
            .fetch(userData.appConfigUrl.first())
            .getOrElse { return Result.retry() }

        // Cache regardless of whether there's anything to notify about, so
        // banners and the update check have fresh data next time the app opens.
        userData.cacheRemoteConfig(config)

        val announcement = config.announcement ?: return Result.success()
        if (announcement.id <= userData.lastSeenAnnouncement.first()) return Result.success()
        if (announcement.title.isBlank() && announcement.body.isBlank()) return Result.success()

        // Recorded before showing, so a failure to post (permission revoked,
        // channel blocked) can't turn into the same notification retrying
        // forever on every subsequent run.
        userData.setLastSeenAnnouncement(announcement.id)
        notify(announcement.title, announcement.body, announcement.url)
        return Result.success()
    }

    private fun notify(title: String, body: String, url: String) {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createChannel(context)

        // A link opens the browser; without one, tapping just opens the app.
        val intent = if (url.isNotBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        } else {
            Intent(context, MainActivity::class.java)
        }.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ANNOUNCEMENTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_isokovibe)
            .setContentTitle(title.ifBlank { "iSokoVibe" })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(context)
                .notify(ANNOUNCEMENT_NOTIFICATION_ID, notification)
        }
    }

    companion object {

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                ANNOUNCEMENTS_CHANNEL_ID,
                "News from iSokoVibe",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "New releases and announcements from iSokoVibe.com.ng"
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }

        /**
         * Schedules the recurring check. KEEP rather than REPLACE so the
         * existing schedule survives app restarts — REPLACE would reset the
         * interval every launch, and a phone opened often would never reach
         * the end of a period and so never actually check.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AnnouncementWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
