package com.example.dawnshift.alarms

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dawnshift.data.WakeSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val index = intent.getIntExtra("ALARM_INDEX", -1)
        if (index != -1) {
            // 1. Mark as completed
            val dataStore = WakeSettingsDataStore(context)
            // Using goAsync for coroutine support in Receiver
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    dataStore.addCompletedAlarmIndex(index)
                    dataStore.clearSnoozeState()
                } finally {
                    pendingResult.finish()
                }
            }
            
            // 2. Cancel the Notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(index)
            
            // 3. Cancel the Snoozed Alarm (PendingIntent)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val snoozeIntent = Intent(context, WakeUpReceiver::class.java).apply {
                putExtra("ALARM_INDEX", index)
            }
            // Must match the exact PendingIntent used to schedule the snooze
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                index,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
            

        }
    }
}
