package com.xplay.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val repository = DeviceRepository(context)
            if (repository.isHostMode.value) {
                val serviceIntent = Intent(context, com.xplay.player.server.LocalServerService::class.java)
                context.startForegroundService(serviceIntent)
            }
            
            val activityIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(activityIntent)
        }
    }
}
