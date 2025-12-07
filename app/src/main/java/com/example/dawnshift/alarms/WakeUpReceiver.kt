package com.example.dawnshift.alarms

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dawnshift.data.WakeSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WakeUpReceiver : BroadcastReceiver() {
    @SuppressLint("FullScreenIntentPolicy")
    @Suppress("UseFullScreenIntent")
    override fun onReceive(context: Context, intent: Intent) {
        val index = intent.getIntExtra("ALARM_INDEX", -1)

        
        // If this is a snoozed alarm firing again, clear the snooze state
        // (the alarm is now actively ringing, not snoozed anymore)
        val dataStore = WakeSettingsDataStore(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = dataStore.settingsFlow.first()
                if (settings.snoozedAlarmIndex == index) {
                    // This is the snoozed alarm firing again, clear snooze state
                    dataStore.clearSnoozeState()
                }
            } catch (e: Exception) {

            } finally {
                pendingResult.finish()
            }
        }
        
        // 1. Create the Intent for the Activity
        val fullScreenIntent = Intent(context, AlarmScreen::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                   Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                   Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("ALARM_INDEX", index)
        }
        
        val fullScreenPendingIntent = android.app.PendingIntent.getActivity(
            context,
            index,
            fullScreenIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Create Notification Channel (Required for O+)
        val channelId = "alarm_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        val channel = android.app.NotificationChannel(
            channelId,
            "Alarm",
            android.app.NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(null, null) // If we play sound in Activity, or set default sound here
        }
        notificationManager.createNotificationChannel(channel)

        // 3. Build the Notification with fullScreenIntent
        val notificationBuilder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Wake Up!")
            .setContentText("It is time to wake up earlier.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)

        // 4. Show it (BEFORE starting activity)
        notificationManager.notify(index, notificationBuilder.build())
        

    }
}
