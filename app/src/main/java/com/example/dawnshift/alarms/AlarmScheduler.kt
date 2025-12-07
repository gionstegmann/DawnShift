package com.example.dawnshift.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleExactAlarm(time: LocalDateTime, requestCode: Int) {
        scheduleExactInternal(time, requestCode)
    }

    fun scheduleAlarms(times: List<LocalDateTime>) {
        // Cancel existing ones first to be clean? 
        // Or just overwrite. Since we might have fewer alarms now than before, 
        // we should probably cancel all possible range first, or at least the ones we don't overwrite.
        // For simplicity, let's assume valid range is 0..100.
        cancelAll()
        
        times.forEachIndexed { index, time ->
            scheduleExactInternal(time, index)
        }
    }
    
    private fun scheduleExactInternal(time: LocalDateTime, requestCode: Int) {

        val intent = Intent(context, WakeUpReceiver::class.java)
        intent.putExtra("ALARM_INDEX", requestCode)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelAll() {
        // Cancel the single one
        val intent = Intent(context, WakeUpReceiver::class.java)
        
        // Cancel a range of potential alarms
        for (i in 0..100) {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                i,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
        
        // Also cancel all notifications (including Snooze persistence)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancelAll()
    }
    
    fun cancelAlarmsFromIndex(fromIndex: Int) {
        val intent = Intent(context, WakeUpReceiver::class.java)
        
        // Cancel alarms from the specified index onwards
        for (i in fromIndex..100) {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                i,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    companion object {
        private const val ALARM_REQUEST_CODE = 1000 // Moved to a safe distance from 0..N indices
    }
}

