package com.presto.mediamanager.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.presto.mediamanager.MainActivity
import com.presto.mediamanager.R

/**
 * Surfaces the "videos to review" count as a launcher badge. Android has no
 * direct icon-number API, so we publish an ongoing notification with
 * [NotificationCompat.Builder.setNumber]; supporting launchers render a
 * dot/count from it. The notification is removed when the count hits zero.
 */
object BadgeManager {
    private const val CHANNEL_ID = "review_badge"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Review badge",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows how many videos still need review"
                setShowBadge(true)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun update(context: Context, count: Int) {
        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)

        if (count <= 0) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        if (!hasNotificationPermission(context)) return

        val launch = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_badge)
            .setContentTitle("Videos to review")
            .setContentText("$count waiting")
            .setNumber(count)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(launch)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
