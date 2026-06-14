package com.taskmanager.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (Prefs.isLoggedIn(context) && Prefs.getServer(context).isNotEmpty()) {
                ApiClient.configure(Prefs.getServer(context))
                val svc = Intent(context, PollService::class.java)
                ContextCompat.startForegroundService(context, svc)
            }
        }
    }
}
