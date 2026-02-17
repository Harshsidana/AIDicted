package com.ai.aidicted.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ai.aidicted.R
import com.ai.aidicted.ui.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "aidicted_news_updates"
    private const val CHANNEL_NAME = "AI News Updates"
    private const val CHANNEL_DESCRIPTION = "Get notified when fresh AI news articles arrive"
    private const val NOTIFICATION_ID = 1001

    /**
     * Creates the notification channel (required for Android 8.0+).
     * Safe to call multiple times — channel is only created once.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Sends a notification about new AI news articles.
     * Tapping the notification opens the app.
     */
    fun sendNewArticlesNotification(context: Context, newArticleCount: Int, latestTitle: String?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (newArticleCount == 1) {
            "🤖 1 New AI Story"
        } else {
            "🤖 $newArticleCount New AI Stories"
        }

        val body = latestTitle ?: "Fresh AI news is waiting for you!"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
