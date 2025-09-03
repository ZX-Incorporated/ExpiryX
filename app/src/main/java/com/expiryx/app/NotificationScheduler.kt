package com.expiryx.app

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlin.math.max

object NotificationScheduler {

    private const val TYPE_7_DAYS = "7days"
    private const val TYPE_1_DAY = "1day"
    private const val TYPE_TODAY = "today"
    private const val TYPE_EXPIRED = "expired"
    private const val NOTIFY_HOUR = 9 // 9AM local time

    fun scheduleForProduct(context: Context, product: Product) {
        val expiry = product.expirationDate ?: return
        val now = System.currentTimeMillis()

        // day boundaries
        val startExpiry = startOfDay(expiry)
        val startToday = startOfDay(now)

        // compute intended schedule times (9am)
        val at7 = atHour(startExpiry - daysMs(7), NOTIFY_HOUR)
        val at1 = atHour(startExpiry - daysMs(1), NOTIFY_HOUR)
        val atToday9 = atHour(startExpiry, NOTIFY_HOUR)

        // expired?
        if (startExpiry < startToday) {
            enqueueImmediate(context, product, TYPE_EXPIRED)
        } else if (startExpiry == startToday) {
            // due today
            if (atToday9 > now) {
                scheduleAt(context, product, TYPE_TODAY, atToday9, now)
            } else {
                enqueueImmediate(context, product, TYPE_TODAY)
            }
        } else {
            // future day
            if (at7 > now) scheduleAt(context, product, TYPE_7_DAYS, at7, now)
            if (at1 > now) scheduleAt(context, product, TYPE_1_DAY, at1, now)
            if (atToday9 > now) scheduleAt(context, product, TYPE_TODAY, atToday9, now)
        }
    }

    fun cancelForProduct(context: Context, product: Product) {
        cancelUnique(context, uniqueName(product.id, TYPE_7_DAYS))
        cancelUnique(context, uniqueName(product.id, TYPE_1_DAY))
        cancelUnique(context, uniqueName(product.id, TYPE_TODAY))
        cancelUnique(context, uniqueName(product.id, TYPE_EXPIRED))
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
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = ts
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun atHour(dayStart: Long, hour24: Int): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = dayStart
            set(java.util.Calendar.HOUR_OF_DAY, hour24)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
