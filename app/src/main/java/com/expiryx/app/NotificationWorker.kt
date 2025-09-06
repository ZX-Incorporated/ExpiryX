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
    }

    override suspend fun doWork(): Result {
        val productId = inputData.getInt(KEY_PRODUCT_ID, 0)
        val productName = inputData.getString(KEY_PRODUCT_NAME) ?: "Item"
        val type = inputData.getString(KEY_MESSAGE_TYPE) ?: "custom"
        val customMessage = inputData.getString(KEY_MESSAGE)

        val sp = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val debounceKey = "last_${productId}_$type"
        val now = System.currentTimeMillis()
        val last = sp.getLong(debounceKey, 0L)

        val DEBOUNCE_MS = 18 * 60 * 60 * 1000L // 18 hours
        if (now - last < DEBOUNCE_MS) return Result.success()

        val message = customMessage ?: when (type) {
            "expired" -> "has already expired."
            "today" -> "expires today."
            "1day" -> "expires tomorrow."
            "reminder" -> "reminder - check expiry date soon."
            "snooze" -> "snooze reminder."
            else -> "reminder."
        }

        // Show notification with app icon in system bar
        NotificationUtils.showExpiryNotification(
            applicationContext,
            productName,
            "$productName $message",
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
