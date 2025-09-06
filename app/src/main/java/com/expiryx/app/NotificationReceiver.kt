package com.expiryx.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra("action")
        
        when (action) {
            "snooze" -> {
                val productId = intent.getIntExtra("product_id", 0)
                val snoozeDays = intent.getIntExtra("snooze_days", 1)
                val title = intent.getStringExtra("title") ?: "Reminder"
                val message = intent.getStringExtra("message") ?: "Product reminder"
                
                // Schedule snooze notification
                NotificationScheduler.scheduleSnooze(context, productId, snoozeDays, title, message)
            }
            else -> {
                val message = intent.getStringExtra("message") ?: "Product reminder"
                val title = intent.getStringExtra("title") ?: "Reminder"
                val productId = intent.getIntExtra("product_id", 0)

                NotificationUtils.createChannel(context)
                NotificationUtils.showExpiryNotification(context, title, message, productId)
            }

        }
    }
}

