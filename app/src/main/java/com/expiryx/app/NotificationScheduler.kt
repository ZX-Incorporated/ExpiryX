package com.expiryx.app

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max

object NotificationScheduler {

    // These are the values for reminder intervals stored in Prefs and used in NotificationSettingsActivity
    internal val POSSIBLE_INTERVAL_VALUES = arrayOf("0", "1", "3", "7", "14") // Changed to internal
    internal const val EXPIRED_NOTICE_TYPE = "expired_notice" // Changed to internal

    fun scheduleForProduct(context: Context, product: Product) {
        // 1. Check global notification master switch
        if (!Prefs.isNotificationsEnabled(context)) {
            cancelForProduct(context, product) // Clear any existing for this product
            return
        }

        // 2. Check global snooze setting
        if (Prefs.isSnoozeActive(context)) {
            // If snoozed, don't schedule new notifications.
            return
        }

        val expiryDate = product.expirationDate ?: return // Must have an expiry date

        cancelForProduct(context, product)

        val now = System.currentTimeMillis()
        val defaultHour = Prefs.getDefaultHour(context)
        val defaultMinute = Prefs.getDefaultMinute(context)

        val startOfExpiryDay = startOfDay(expiryDate)
        val startOfToday = startOfDay(now)

        if (startOfExpiryDay < startOfToday) {
            enqueueImmediate(context, product, EXPIRED_NOTICE_TYPE)
            return
        }

        val selectedIntervalsDays = Prefs.getReminderIntervals(context)
            .mapNotNull { it.toIntOrNull() }
            .sorted()

        for (daysBefore in selectedIntervalsDays) {
            val notificationTimeCal = Calendar.getInstance().apply {
                timeInMillis = startOfExpiryDay
                add(Calendar.DAY_OF_YEAR, -daysBefore)
                set(Calendar.HOUR_OF_DAY, defaultHour)
                set(Calendar.MINUTE, defaultMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val notificationTime = notificationTimeCal.timeInMillis
            val messageType = "interval_$daysBefore"

            if (notificationTime > now) {
                scheduleAt(context, product, messageType, notificationTime, now)
            } else if (daysBefore == 0 && startOfDay(notificationTime) == startOfToday) {
                enqueueImmediate(context, product, messageType)
            }
        }
    }

    fun cancelForProduct(context: Context, product: Product) {
        POSSIBLE_INTERVAL_VALUES.forEach { day ->
            cancelUnique(context, uniqueName(product.id, "interval_$day"))
        }
        cancelUnique(context, uniqueName(product.id, EXPIRED_NOTICE_TYPE))
        cancelUnique(context, uniqueName(product.id, "snooze"))
    }

    fun rescheduleAllNotifications(context: Context, products: List<Product>) {
        products.forEach { product ->
            cancelForProduct(context, product)
        }
        products.forEach { product ->
            scheduleForProduct(context, product)
        }
    }

    fun scheduleSnooze(context: Context, productId: Int, snoozeDays: Int, title: String, message: String) {
        val now = System.currentTimeMillis()
        val snoozeTime = now + daysMs(snoozeDays)

        val input = NotificationWorker.Input(
            productId = productId,
            productName = title,
            messageType = "snooze",
            message = message
        ).toData()

        val req = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(input)
            .setInitialDelay(snoozeTime - now, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                uniqueName(productId, "snooze"),
                ExistingWorkPolicy.REPLACE,
                req
            )
    }

    private fun scheduleAt(
        context: Context,
        product: Product,
        messageType: String,
        scheduledAt: Long,
        now: Long
    ) {
        val delay = max(0L, scheduledAt - now)
        val input = NotificationWorker.Input(
            productId = product.id,
            productName = product.name,
            messageType = messageType,
            message = null
        ).toData()

        val req = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(input)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                uniqueName(product.id, messageType),
                ExistingWorkPolicy.REPLACE,
                req
            )
    }

    private fun enqueueImmediate(context: Context, product: Product, messageType: String) {
        val input = NotificationWorker.Input(
            productId = product.id,
            productName = product.name,
            messageType = messageType,
            message = null
        ).toData()

        val req = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(input)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                uniqueName(product.id, messageType),
                ExistingWorkPolicy.REPLACE,
                req
            )
    }

    private fun cancelUnique(context: Context, name: String) {
        WorkManager.getInstance(context).cancelUniqueWork(name)
    }

    private fun uniqueName(productId: Int, type: String) = "notif_${productId}_$type"
    private fun daysMs(d: Int) = d * 24L * 60 * 60 * 1000

    private fun startOfDay(ts: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
