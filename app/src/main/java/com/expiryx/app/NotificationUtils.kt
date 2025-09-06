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

    fun showExpiryNotification(context: Context, title: String, message: String, productId: Int = 0, type: String? = null) {
        createChannel(context)

        // Open app when tapped
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            productId.takeIf { it > 0 } ?: 0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name) // ensure this exists
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)

        // Snooze action for product-scoped notices
        if (Prefs.isNotificationsEnabled(context) && productId > 0) {
            val snoozeDays = 1
            val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("action", "snooze")
                putExtra("product_id", productId)
                putExtra("snooze_days", snoozeDays)
                putExtra("title", title)
                putExtra("message", message)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                productId + 1000,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_clock_filled, "Snooze $snoozeDays day(s)", snoozePendingIntent)
        }

        val notification = builder.build()

        // Stable ID: productId + (type/message) to allow stacking per product
        val stableKey = if (productId > 0) "${productId}_${type ?: message}" else "global_${title}_${message}"
        val notifyId = stableKey.hashCode()

        with(NotificationManagerCompat.from(context)) { notify(notifyId, notification) }
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = CHANNEL_DESC }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
