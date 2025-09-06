package com.expiryx.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationUtils {
    private const val CHANNEL_ID = "expiry_notifications"
    private const val CHANNEL_NAME = "Expiry Reminders"
    private const val CHANNEL_DESC = "Notifications about product expirations"

    fun showExpiryNotification(context: Context, title: String, message: String, productId: Int = 0) {
        createChannel(context)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Add snooze action if enabled
        if (Prefs.isSnoozeEnabled(context) && productId > 0) {
            val snoozeDays = Prefs.getSnoozeDays(context)
            val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("action", "snooze")
                putExtra("product_id", productId)
                putExtra("snooze_days", snoozeDays)
                putExtra("title", title)
                putExtra("message", message)
            }
            
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                productId + 1000, // Unique request code
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            notificationBuilder.addAction(
                R.drawable.ic_clock_filled,
                "Snooze $snoozeDays day(s)",
                snoozePendingIntent
            )
        }

        val notification = notificationBuilder.build()

        with(NotificationManagerCompat.from(context)) {
            notify(title.hashCode(), notification)
        }
    }

    // 🔓 made public so other classes can call it
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
