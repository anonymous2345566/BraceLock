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

    fun getNotes(): List<String> {
        val notesString = prefs.getString("saved_notes", "") ?: ""
        return if (notesString.isEmpty()) emptyList() else notesString.split("||")
    }

    fun addNote(note: String) {
        val timestamp = SimpleDateFormat("dd-MM HH:mm", Locale.getDefault()).format(Date())
        val newEntry = "[$timestamp] $note"
        val currentNotes = getNotes().toMutableList()
        currentNotes.add(0, newEntry) // Voeg bovenaan toe
        prefs.edit().putString("saved_notes", currentNotes.joinToString("||")).apply()
    }

    fun getHistory(): List<String> {
        val historyString = prefs.getString("override_logs", "") ?: ""
        return if (historyString.isEmpty()) emptyList() else historyString.split("\n").filter { it.isNotBlank() }.reversed()
    }

    fun logOverride(reason: String) {
        val currentLogs = prefs.getString("override_logs", "") ?: ""
        val timestamp = SimpleDateFormat("dd-MM HH:mm", Locale.getDefault()).format(Date())
        val newLog = "[$timestamp] Override: $reason\n"
        prefs.edit().putString("override_logs", currentLogs + newLog).apply()
        isLocked = false
    }

    fun startWearing() {
        wearStartTime = System.currentTimeMillis()
        val currentLogs = prefs.getString("override_logs", "") ?: ""
        val timestamp = SimpleDateFormat("dd-MM HH:mm", Locale.getDefault()).format(Date())
        prefs.edit().putString("override_logs", currentLogs + "[$timestamp] Brace omgedaan\n").apply()
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
