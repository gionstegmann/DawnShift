package com.example.dawnshift.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wake_settings")

class WakeSettingsDataStore(private val context: Context) {

    private val startHourKey = intPreferencesKey("start_hour")
    private val startMinuteKey = intPreferencesKey("start_minute")
    private val targetHourKey = intPreferencesKey("target_hour")
    private val targetMinuteKey = intPreferencesKey("target_minute")
    private val daysKey = intPreferencesKey("days")
    private val alarmsSetKey = booleanPreferencesKey("alarms_set")
    private val scheduleStartDateKey = longPreferencesKey("schedule_start_date")
    private val completedIndicesKey = stringSetPreferencesKey("completed_indices")
    private val alarmSoundUriKey = stringPreferencesKey("alarm_sound_uri")
    private val snoozedAlarmIndexKey = intPreferencesKey("snoozed_alarm_index")
    private val snoozeUntilEpochKey = longPreferencesKey("snooze_until_epoch")

    val settingsFlow: Flow<WakeSettings> = context.dataStore.data
        .map { preferences ->
            WakeSettings(
                startHour = preferences[startHourKey] ?: 11,
                startMinute = preferences[startMinuteKey] ?: 0,
                targetHour = preferences[targetHourKey] ?: 8,
                targetMinute = preferences[targetMinuteKey] ?: 0,
                days = preferences[daysKey] ?: 7,
                alarmsSet = preferences[alarmsSetKey] ?: false,
                scheduleStartDateEpoch = preferences[scheduleStartDateKey] ?: 0L,
                completedIndices = preferences[completedIndicesKey]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet(),
                alarmSoundUri = preferences[alarmSoundUriKey] ?: android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI.toString(),
                snoozedAlarmIndex = preferences[snoozedAlarmIndexKey] ?: -1,
                snoozeUntilEpochMillis = preferences[snoozeUntilEpochKey] ?: 0L
            )
        }

    suspend fun saveStartTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[startHourKey] = hour
            preferences[startMinuteKey] = minute
        }
    }

    suspend fun saveTargetTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[targetHourKey] = hour
            preferences[targetMinuteKey] = minute
        }
    }

    suspend fun saveDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[daysKey] = days
        }
    }

    suspend fun setAlarmsSet(isSet: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[alarmsSetKey] = isSet
        }
    }

    suspend fun setScheduleStartDate(epochDay: Long) {
        context.dataStore.edit { preferences ->
            preferences[scheduleStartDateKey] = epochDay
        }
    }

    suspend fun addCompletedAlarmIndex(index: Int) {
        context.dataStore.edit { preferences ->
            val current = preferences[completedIndicesKey] ?: emptySet()
            preferences[completedIndicesKey] = current + index.toString()
        }
    }

    suspend fun clearProgress() {
        context.dataStore.edit { preferences ->
            preferences[completedIndicesKey] = emptySet()
        }
    }

    suspend fun saveAlarmSound(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[alarmSoundUriKey] = uri
        }
    }

    suspend fun setSnoozeState(index: Int, time: Long) {
        context.dataStore.edit { preferences ->
            preferences[snoozedAlarmIndexKey] = index
            preferences[snoozeUntilEpochKey] = time
        }
    }

    suspend fun clearSnoozeState() {
        context.dataStore.edit { preferences ->
            preferences[snoozedAlarmIndexKey] = -1
            preferences[snoozeUntilEpochKey] = 0L
        }
    }
}

data class WakeSettings(
    val startHour: Int,
    val startMinute: Int,
    val targetHour: Int,
    val targetMinute: Int,
    val days: Int,
    val alarmsSet: Boolean = false,
    val scheduleStartDateEpoch: Long = 0L,
    val completedIndices: Set<Int> = emptySet(),
    val alarmSoundUri: String = "",
    val snoozedAlarmIndex: Int = -1,
    val snoozeUntilEpochMillis: Long = 0L
)
