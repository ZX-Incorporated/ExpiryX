package com.expiryx.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_PRODUCT_ID = "product_id"
        const val KEY_PRODUCT_NAME = "product_name"
        const val KEY_MESSAGE_TYPE = "message_type"
        const val KEY_MESSAGE = "message"
        private const val PREFS_NAME = "notif_debounce"
        private const val DEBOUNCE_MS = 18 * 60 * 60 * 1000L // 18 hours
    }

    override suspend fun doWork(): Result {
        val productId = inputData.getInt(KEY_PRODUCT_ID, 0)
        val productName = inputData.getString(KEY_PRODUCT_NAME) ?: "Item"
        val type = inputData.getString(KEY_MESSAGE_TYPE) ?: "custom"
        val customMessage = inputData.getString(KEY_MESSAGE)

        // debounce to avoid spam
        val sp = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val debounceKey = "last_${productId}_$type"
        val now = System.currentTimeMillis()
        val last = sp.getLong(debounceKey, 0L)
        if (now - last < DEBOUNCE_MS) return Result.success()

        val resolvedMessage = when {
            // from NotificationScheduler: "interval_X"
            type.startsWith("interval_") -> {
                val d = type.removePrefix("interval_").toIntOrNull()
                when (d) {
                    0 -> "$productName expires today."
                    1 -> "$productName expires tomorrow."
                    null -> "$productName reminder."
                    else -> "$productName expires in $d days."
                }
            }
            // from NotificationScheduler: "expired_notice"
            type == NotificationScheduler.EXPIRED_NOTICE_TYPE -> "$productName has already expired."
            // snooze
            type == "snooze" -> customMessage ?: "$productName snooze reminder."
            // fallback (custom)
            else -> customMessage ?: "$productName reminder."
        }

        NotificationUtils.showExpiryNotification(
            applicationContext,
            productName,
            resolvedMessage,
            productId
        )

        sp.edit().putLong(debounceKey, now).apply()
        return Result.success()
    }

    class Input(
        val productId: Int,
        val productName: String,
        val messageType: String,
        val message: String? = null
    ) {
        fun toData(): Data {
            return Data.Builder()
                .putInt(KEY_PRODUCT_ID, productId)
                .putString(KEY_PRODUCT_NAME, productName)
                .putString(KEY_MESSAGE_TYPE, messageType)
                .putString(KEY_MESSAGE, message)
                .build()
        }
    }
}
