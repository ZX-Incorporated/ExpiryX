package com.expiryx.app

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

object Prefs {
    private const val NAME = "expiryx_prefs"

    private const val KEY_NOTIF_ENABLED = "notifications_enabled" // Keep general on/off
    private const val KEY_DEFAULT_HOUR = "notif_default_hour"
    private const val KEY_DEFAULT_MINUTE = "notif_default_minute"
    private const val KEY_REMINDER_INTERVALS = "reminder_intervals"
    private const val KEY_SNOOZE_END_TIMESTAMP = "snooze_end_timestamp" // New key for snooze

    private fun sp(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    // ---- General Notifications Enabled ----
    fun isNotificationsEnabled(context: Context): Boolean =
        sp(context).getBoolean(KEY_NOTIF_ENABLED, true)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        sp(context).edit().putBoolean(KEY_NOTIF_ENABLED, enabled).apply()
        if (!enabled) { // If turning all notifications off, also clear any active snooze
            clearSnooze(context)
        }
    }

    // ---- Default reminder time ----
    fun getDefaultHour(context: Context): Int =
        sp(context).getInt(KEY_DEFAULT_HOUR, 9) // Default 9 AM

    fun getDefaultMinute(context: Context): Int =
        sp(context).getInt(KEY_DEFAULT_MINUTE, 0)

    fun setDefaultTime(context: Context, hour: Int, minute: Int) {
        sp(context).edit()
            .putInt(KEY_DEFAULT_HOUR, hour)
            .putInt(KEY_DEFAULT_MINUTE, minute)
            .apply()
    }

    // ---- Reminder Intervals ----
    fun getReminderIntervals(context: Context): Set<String> {
        return sp(context).getStringSet(KEY_REMINDER_INTERVALS, setOf("0", "1")) ?: setOf("0", "1")
    }

    fun setReminderIntervals(context: Context, intervals: Set<String>) {
        sp(context).edit().putStringSet(KEY_REMINDER_INTERVALS, intervals).apply()
    }

    // ---- Snooze ----
    fun getSnoozeEndTimestamp(context: Context): Long =
        sp(context).getLong(KEY_SNOOZE_END_TIMESTAMP, 0L)

    fun isSnoozeActive(context: Context): Boolean {
        val snoozeEndTime = getSnoozeEndTimestamp(context)
        return snoozeEndTime > 0 && System.currentTimeMillis() < snoozeEndTime
    }

    /**
     * Sets snooze for a specified number of days from now.
     * @param days Number of days to snooze. If 0 or less, snooze is cleared.
     */
    fun setSnooze(context: Context, days: Int) {
        if (days > 0) {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, days)
                set(Calendar.HOUR_OF_DAY, 23) // Snooze until end of the last day
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            sp(context).edit().putLong(KEY_SNOOZE_END_TIMESTAMP, calendar.timeInMillis).apply()
        } else {
            clearSnooze(context) // Clear snooze if days is 0 or negative
        }
    }

    fun clearSnooze(context: Context) {
        sp(context).edit().putLong(KEY_SNOOZE_END_TIMESTAMP, 0L).apply()
    }
}
