package com.example.bracelock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val dataManager = DataManager(context)
        // Zet de status op vergrendeld
        dataManager.isLocked = true
        
        // Start het vergrendelscherm
        val serviceIntent = Intent(context, LockService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        
        Toast.makeText(context, "07:00 - BraceLock is geactiveerd!", Toast.LENGTH_LONG).show()
    }
}
