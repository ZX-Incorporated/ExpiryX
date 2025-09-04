// app/src/main/java/com/expiryx/app/Prefs.kt
package com.expiryx.app

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "expiryx_prefs"

    private const val KEY_NOTIF_ENABLED = "notifications_enabled"
    private const val KEY_DEFAULT_HOUR = "notif_default_hour"
    private const val KEY_DEFAULT_MINUTE = "notif_default_minute"
    private const val KEY_SNOOZE_ENABLED = "snooze_enabled"
    private const val KEY_SNOOZE_DAYS = "snooze_days"

    private fun sp(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    // ---- Notifications on/off ----
    fun isNotificationsEnabled(context: Context): Boolean =
        sp(context).getBoolean(KEY_NOTIF_ENABLED, true)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        sp(context).edit().putBoolean(KEY_NOTIF_ENABLED, enabled).apply()
    }

    // ---- Default reminder time ----
    fun getDefaultHour(context: Context): Int =
        sp(context).getInt(KEY_DEFAULT_HOUR, 9) // default 9AM

    fun getDefaultMinute(context: Context): Int =
        sp(context).getInt(KEY_DEFAULT_MINUTE, 0)

    fun setDefaultTime(context: Context, hour: Int, minute: Int) {
        sp(context).edit()
            .putInt(KEY_DEFAULT_HOUR, hour)
            .putInt(KEY_DEFAULT_MINUTE, minute)
            .apply()
    }

    // ---- Snooze ----
    fun isSnoozeEnabled(context: Context): Boolean =
        sp(context).getBoolean(KEY_SNOOZE_ENABLED, false)

    fun getSnoozeDays(context: Context): Int =
        sp(context).getInt(KEY_SNOOZE_DAYS, 1)

    fun setSnooze(context: Context, enabled: Boolean, days: Int) {
        sp(context).edit()
            .putBoolean(KEY_SNOOZE_ENABLED, enabled)
            .putInt(KEY_SNOOZE_DAYS, days)
            .apply()
    }
}
