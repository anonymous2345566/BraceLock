package com.example.bracelock

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DataManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("brace_prefs", Context.MODE_PRIVATE)

    var isLocked: Boolean
        get() = prefs.getBoolean("is_locked", true)
        set(value) = prefs.edit().putBoolean("is_locked", value).apply()

    var wearStartTime: Long
        get() = prefs.getLong("wear_start_time", 0L)
        set(value) = prefs.edit().putLong("wear_start_time", value).apply()

    var dailyNotes: String
        get() = prefs.getString("daily_notes", "") ?: ""
        set(value) = prefs.edit().putString("daily_notes", value).apply()

    fun logOverride(reason: String) {
        val currentLogs = prefs.getString("override_logs", "") ?: ""
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val newLog = "[$timestamp] Reden: $reason\n"
        prefs.edit().putString("override_logs", currentLogs + newLog).apply()
        isLocked = false
    }

    fun startWearing() {
        wearStartTime = System.currentTimeMillis()
        isLocked = false
    }

    fun getElapsedTimeFormatted(): String {
        if (wearStartTime == 0L) return "0u 0m"
        val diff = System.currentTimeMillis() - wearStartTime
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff / (1000 * 60)) % 60
        return "${hours}u ${minutes}m"
    }
}
