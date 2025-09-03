package com.expiryx.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("message") ?: "Product reminder"
        val title = intent.getStringExtra("title") ?: "Reminder"

        NotificationUtils.createChannel(context) // ✅ now public
        NotificationUtils.showExpiryNotification(context, title, message)
    }
}

