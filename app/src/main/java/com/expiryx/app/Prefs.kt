// app/src/main/java/com/expiryx/app/Prefs.kt
package com.expiryx.app

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "expiryx_prefs"
    private const val KEY_NOTIF_ENABLED = "notifications_enabled"

    private fun sp(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isNotificationsEnabled(context: Context): Boolean =
        sp(context).getBoolean(KEY_NOTIF_ENABLED, true)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        sp(context).edit().putBoolean(KEY_NOTIF_ENABLED, enabled).apply()
    }
}
