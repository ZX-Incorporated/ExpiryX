package com.expiryx.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * On boot, sweep DB:
 * - Fire immediate notifications for expired/today items (via WorkManager).
 * - Schedule all future reminders.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as ProductApplication
            val repo = app.repository

            CoroutineScope(Dispatchers.IO).launch {
                val products = repo.getAllProductsNow()
                products.forEach { product ->
                    NotificationScheduler.scheduleForProduct(context, product)
                }
            }
        }
    }
}
