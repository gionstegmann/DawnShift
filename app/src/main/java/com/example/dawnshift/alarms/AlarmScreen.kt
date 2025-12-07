package com.example.dawnshift.alarms

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dawnshift.ui.theme.EarlierEveryDayTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class AlarmScreen : ComponentActivity() {

    private var mediaPlayer: android.media.MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start playing alarm sound
        val dataStore = com.example.dawnshift.data.WakeSettingsDataStore(this)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                // Get URI from settings
                val settings: com.example.dawnshift.data.WakeSettings = dataStore.settingsFlow.first()
                val soundUriStr = settings.alarmSoundUri
                val soundUri = if (soundUriStr.isNotEmpty()) soundUriStr.toUri() else android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
                
                mediaPlayer = android.media.MediaPlayer().apply {
                    setDataSource(this@AlarmScreen, soundUri)
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e: Exception) {

                // Fallback to notification sound or just fail silently if even that fails
                try {
                     mediaPlayer = android.media.MediaPlayer.create(this@AlarmScreen, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                     mediaPlayer?.isLooping = true
                     mediaPlayer?.start()
                } catch (e2: Exception) {

                }
            }
        }

        // Turn screen on and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
             @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        

        
        // Cancel the notification that launched this activity (to avoid duplicate visual)
        val index = intent.getIntExtra("ALARM_INDEX", -1)
        if (index != -1) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(index)
        }

        setContent {
            EarlierEveryDayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            var currentTime by remember { mutableStateOf(java.time.LocalTime.now()) }
                            LaunchedEffect(Unit) {
                                while (true) {
                                    currentTime = java.time.LocalTime.now()
                                    delay(1000)
                                }
                            }

                            Text(
                                text = "Good Morning",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = currentTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(64.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Dismiss Button
                                OutlinedButton(
                                    onClick = {
                                        // 1. Mark as completed
                                        val alarmIndex = intent.getIntExtra("ALARM_INDEX", -1)
                                        if (alarmIndex != -1) {
                                            val dataStore =
                                                com.example.dawnshift.data.WakeSettingsDataStore(
                                                    context = this@AlarmScreen
                                                )
                                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                            notificationManager.cancel(alarmIndex)
                                            kotlinx.coroutines.runBlocking {
                                                dataStore.addCompletedAlarmIndex(alarmIndex)
                                                dataStore.clearSnoozeState()
                                            }
                                        }
                                        finish()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                ) {
                                    Text("Dismiss")
                                }

                                // Snooze Button
                                Button(
                                    onClick = {
                                        val alarmIndex = intent.getIntExtra("ALARM_INDEX", -1)
                                        if (alarmIndex != -1) {
                                            val alarmManager =
                                                getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                                            val snoozeIntent = android.content.Intent(
                                                this@AlarmScreen,
                                                WakeUpReceiver::class.java
                                            ).apply {
                                                putExtra("ALARM_INDEX", alarmIndex)
                                            }
                                            val pendingIntent = android.app.PendingIntent.getBroadcast(
                                                this@AlarmScreen,
                                                alarmIndex,
                                                snoozeIntent,
                                                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                                            )

                                            // Snooze for 10 minutes
                                            val triggerTime = System.currentTimeMillis() + 10 * 60 * 1000

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                if (alarmManager.canScheduleExactAlarms()) {
                                                    alarmManager.setExactAndAllowWhileIdle(
                                                        android.app.AlarmManager.RTC_WAKEUP,
                                                        triggerTime,
                                                        pendingIntent
                                                    )
                                                }
                                            } else {
                                                alarmManager.setExactAndAllowWhileIdle(
                                                    android.app.AlarmManager.RTC_WAKEUP,
                                                    triggerTime,
                                                    pendingIntent
                                                )
                                            }

                                            // Post "Snoozed" persistent notification
                                            val notificationManager =
                                                getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                            val channelId = "snooze_channel"

                                            val channel = android.app.NotificationChannel(
                                                channelId,
                                                "Snooze Status",
                                                android.app.NotificationManager.IMPORTANCE_LOW
                                            )
                                            notificationManager.createNotificationChannel(channel)

                                            val snoozeTimeStr =
                                                java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT)
                                                    .format(
                                                        java.time.Instant.ofEpochMilli(triggerTime)
                                                            .atZone(java.time.ZoneId.systemDefault())
                                                    )

                                            // 3. Create Dismiss Action PendingIntent
                                            val dismissIntent = android.content.Intent(
                                                this@AlarmScreen,
                                                DismissReceiver::class.java
                                            ).apply {
                                                putExtra("ALARM_INDEX", alarmIndex)
                                            }
                                            val dismissPendingIntent =
                                                android.app.PendingIntent.getBroadcast(
                                                    this@AlarmScreen,
                                                    alarmIndex,
                                                    dismissIntent,
                                                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                                                )

                                            // 4. Create Content Intent (Launch App)
                                            val mainActivityIntent = android.content.Intent(this@AlarmScreen, com.example.dawnshift.MainActivity::class.java).apply {
                                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            }
                                            val contentPendingIntent = android.app.PendingIntent.getActivity(
                                                this@AlarmScreen,
                                                0,
                                                mainActivityIntent,
                                                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                                            )

                                            // 5. Persist Snooze State
                                            val dataStore = com.example.dawnshift.data.WakeSettingsDataStore(context = this@AlarmScreen)
                                            kotlinx.coroutines.runBlocking {
                                                dataStore.setSnoozeState(alarmIndex, triggerTime)
                                            }

                                            val notification = androidx.core.app.NotificationCompat.Builder(
                                                this@AlarmScreen,
                                                channelId
                                            )
                                                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                                                .setContentTitle("Alarm Snoozed")
                                                .setContentText("Next alarm at $snoozeTimeStr")
                                                .setOngoing(true) // Persistent
                                                .setContentIntent(contentPendingIntent)
                                                .addAction(
                                                    android.R.drawable.ic_menu_close_clear_cancel,
                                                    "Dismiss",
                                                    dismissPendingIntent
                                                )
                                                .build()

                                            notificationManager.notify(alarmIndex, notification)

                                            android.widget.Toast.makeText(
                                                this@AlarmScreen,
                                                "Snoozed until $snoozeTimeStr",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        finish()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                ) {
                                    Text("Snooze (10min)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        stopAlarmSound()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
    }
    
    private fun stopAlarmSound() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
