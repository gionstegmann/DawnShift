package com.example.dawnshift.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dawnshift.alarms.AlarmScheduler
import com.example.dawnshift.data.WakeSettingsDataStore
import com.example.dawnshift.logic.WakeScheduleCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class WakeSettingsUiState(
    val startTime: LocalTime = LocalTime.of(11, 0),
    val targetTime: LocalTime = LocalTime.of(8, 0),
    val days: Int = 7,
    val schedule: List<ScheduleItemData> = emptyList(),
    val error: String? = null,
    val alarmsSet: Boolean = false,
    val scheduleStartDate: LocalDate? = null,
    val activeSchedule: List<ScheduleItemData> = emptyList(),
    val completedCount: Int = 0,
    val nextIndex: Int = -1,
    val alarmSoundUri: String = "",
    val snoozedAlarmIndex: Int = -1,
    val snoozeUntilEpochMillis: Long = 0L
)

data class ScheduleItemData(
    val dayIndex: Int,
    val date: LocalDate,
    val time: LocalTime,
    val status: ItemStatus
)

enum class ItemStatus {
    COMPLETED,
    NEXT,
    FUTURE
}

class WakeSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(WakeSettingsUiState())
    val uiState: StateFlow<WakeSettingsUiState> = _uiState.asStateFlow()
    
    private val alarmScheduler = AlarmScheduler(application)
    private val dataStore = WakeSettingsDataStore(application)

    init {
        viewModelScope.launch {
            dataStore.settingsFlow.collect { settings ->
                val currentDate = LocalDate.now()
                val currentDateTime = LocalDateTime.now()
                
                val startDate = if (settings.scheduleStartDateEpoch > 0) LocalDate.ofEpochDay(settings.scheduleStartDateEpoch) else null
                
                _uiState.update { 
                    it.copy(
                        startTime = LocalTime.of(settings.startHour, settings.startMinute),
                        targetTime = LocalTime.of(settings.targetHour, settings.targetMinute),
                        days = settings.days,
                        alarmsSet = settings.alarmsSet,
                        scheduleStartDate = startDate,
                        alarmSoundUri = settings.alarmSoundUri,
                        snoozedAlarmIndex = settings.snoozedAlarmIndex,
                        snoozeUntilEpochMillis = settings.snoozeUntilEpochMillis
                    )
                }
                
                // If alarms are set, reconstruct the active schedule for display
                if (settings.alarmsSet && startDate != null) {
                    val scheduleTimes = WakeScheduleCalculator.calculateSchedule(
                        start = LocalTime.of(settings.startHour, settings.startMinute),
                        target = LocalTime.of(settings.targetHour, settings.targetMinute),
                        days = settings.days
                    )
                    
                    // Determine status for each item
                    val completed = settings.completedIndices
                    // The "Next" item is the first one (lowest index) that is NOT completed.
                    val nextIndex = (0 until settings.days).firstOrNull { it !in completed } ?: -1
                    
                    val activeScheduleWithStatus = scheduleTimes.mapIndexed { index, time ->
                        val status = when (index) {
                            in completed -> ItemStatus.COMPLETED
                            nextIndex -> ItemStatus.NEXT
                            else -> ItemStatus.FUTURE
                        }
                        ScheduleItemData(
                            dayIndex = index + 1,
                            date = startDate.plusDays(index.toLong()),
                            time = time,
                            status = status
                        )
                    }
                     _uiState.update { it.copy(
                         activeSchedule = activeScheduleWithStatus,
                         completedCount = completed.size,
                         nextIndex = nextIndex
                     ) }
                } else {
                     _uiState.update { it.copy(activeSchedule = emptyList(), completedCount = 0, nextIndex = -1) }
                }
                
                // Always calculate the potential schedule too
                calculatePotentialSchedule(currentDate, currentDateTime)
            }
        }
    }

    fun updateStartTime(time: LocalTime) {
        viewModelScope.launch {
            dataStore.saveStartTime(time.hour, time.minute)
        }
    }

    fun updateTargetTime(time: LocalTime) {
        viewModelScope.launch {
            dataStore.saveTargetTime(time.hour, time.minute)
        }
    }

    fun updateDays(daysStr: String) {
        val days = daysStr.toIntOrNull()
        if (days != null && days > 0) {
            viewModelScope.launch {
                dataStore.saveDays(days)
            }
            _uiState.update { it.copy(error = null) }
        } else {
             _uiState.update { it.copy(error = "Please enter a valid number of days") }
        }
    }
    
    fun updateAlarmSound(uri: String) {
        viewModelScope.launch {
            dataStore.saveAlarmSound(uri)
        }
    }

    private fun calculatePotentialSchedule(currentDate: LocalDate, currentDateTime: LocalDateTime) {
        val state = _uiState.value
        val scheduleTimes = WakeScheduleCalculator.calculateSchedule(
            start = state.startTime,
            target = state.targetTime,
            days = state.days
        )
        
        // Determine Start Date (Same logic as scheduleAllAlarms)
        val firstAlarmTime = scheduleTimes.firstOrNull() ?: LocalTime.now()
        val todayFirstAlarm = LocalDateTime.of(currentDate, firstAlarmTime)
        
        val startDate = if (todayFirstAlarm.isAfter(currentDateTime)) {
            currentDate
        } else {
            currentDate.plusDays(1)
        }
        
        val scheduleWithDates = scheduleTimes.mapIndexed { index, time ->
            ScheduleItemData(
                dayIndex = index + 1,
                date = startDate.plusDays(index.toLong()),
                time = time,
                status = ItemStatus.FUTURE
            )
        }
        
        _uiState.update { it.copy(schedule = scheduleWithDates) }
    }
    
    private fun scheduleAlarms(schedule: List<LocalTime>, startDate: LocalDate) {
        if (schedule.isNotEmpty()) {
            val alarmTimes = mutableListOf<LocalDateTime>()
            
            schedule.forEachIndexed { index, time ->
                val alarmTime = LocalDateTime.of(startDate.plusDays(index.toLong()), time)
                alarmTimes.add(alarmTime)
            }
            
            alarmScheduler.scheduleAlarms(alarmTimes)
        }
    }

    fun scheduleAllAlarms() {
        val state = _uiState.value
        val currentDate = LocalDate.now()
        val currentDateTime = LocalDateTime.now()

        val schedule = WakeScheduleCalculator.calculateSchedule(
            start = state.startTime,
            target = state.targetTime,
            days = state.days
        )
        
        // Determine Start Date based on whether the first alarm time has passed today
        val firstAlarmTime = schedule.firstOrNull() ?: LocalTime.now()
        val todayFirstAlarm = LocalDateTime.of(currentDate, firstAlarmTime)
        
        val startDate = if (todayFirstAlarm.isAfter(currentDateTime)) {
            currentDate
        } else {
            currentDate.plusDays(1)
        }
        
        viewModelScope.launch {
            dataStore.setAlarmsSet(true)
            dataStore.setScheduleStartDate(startDate.toEpochDay())
        }
        
        scheduleAlarms(schedule, startDate)
    }
    
    fun cancelAllAlarms() {
        alarmScheduler.cancelAll()
        viewModelScope.launch {
            dataStore.setAlarmsSet(false)
            dataStore.setScheduleStartDate(0L)
            dataStore.clearProgress()
            dataStore.clearSnoozeState()
        }
    }

    fun dismissSnooze() {
        viewModelScope.launch {
            val currentIndex = _uiState.value.snoozedAlarmIndex
            if (currentIndex != -1) {
                dataStore.addCompletedAlarmIndex(currentIndex)
            }
            dataStore.clearSnoozeState()
        }
    }
    
    fun updateActivePlan() {
        val state = _uiState.value
        if (!state.alarmsSet || state.scheduleStartDate == null) return
        
        viewModelScope.launch {
            val settings = dataStore.settingsFlow.first()
            val completedIndices = settings.completedIndices
            
            // Get original values from dataStore (these are the values before editing started)
            val originalStartTime = LocalTime.of(settings.startHour, settings.startMinute)
            val originalTargetTime = LocalTime.of(settings.targetHour, settings.targetMinute)
            val originalDays = settings.days
            
            // Find the next incomplete day index (based on original schedule)
            val nextIncompleteIndex = (0 until originalDays).firstOrNull { it !in completedIndices }
            if (nextIncompleteIndex == null) {
                // All days completed, just save the new settings
                dataStore.saveStartTime(state.startTime.hour, state.startTime.minute)
                dataStore.saveTargetTime(state.targetTime.hour, state.targetTime.minute)
                dataStore.saveDays(state.days)
                return@launch
            }
            
            // Calculate original schedule to determine current position
            val originalSchedule = WakeScheduleCalculator.calculateSchedule(
                start = originalStartTime,
                target = originalTargetTime,
                days = originalDays
            )
            
            // Calculate what the current wake time should be based on completed days
            // If nextIncompleteIndex is the first incomplete day, we've completed nextIncompleteIndex days
            val currentWakeTime = if (nextIncompleteIndex > 0 && nextIncompleteIndex <= originalSchedule.size) {
                originalSchedule[nextIncompleteIndex - 1]
            } else {
                originalStartTime // Haven't started yet
            }
            
            // Calculate remaining days (use new days value from state)
            val remainingDays = state.days - nextIncompleteIndex
            if (remainingDays <= 0) {
                // No remaining days, cancel future alarms and save settings
                alarmScheduler.cancelAlarmsFromIndex(nextIncompleteIndex)
                dataStore.saveStartTime(state.startTime.hour, state.startTime.minute)
                dataStore.saveTargetTime(state.targetTime.hour, state.targetTime.minute)
                dataStore.saveDays(state.days)
                return@launch
            }
            
            // Calculate new schedule for remaining days: current time -> new target time
            val newSchedule = WakeScheduleCalculator.calculateSchedule(
                start = currentWakeTime,
                target = state.targetTime,
                days = remainingDays
            )
            
            // Save updated settings to dataStore
            dataStore.saveStartTime(state.startTime.hour, state.startTime.minute)
            dataStore.saveTargetTime(state.targetTime.hour, state.targetTime.minute)
            dataStore.saveDays(state.days)
            
            // Cancel alarms from nextIncompleteIndex onwards
            alarmScheduler.cancelAlarmsFromIndex(nextIncompleteIndex)
            
            // Schedule new alarms for remaining days
            val startDate = state.scheduleStartDate
            val alarmTimes = mutableListOf<LocalDateTime>()
            newSchedule.forEachIndexed { scheduleIndex, time ->
                val dayIndex = nextIncompleteIndex + scheduleIndex
                val alarmDate = startDate.plusDays(dayIndex.toLong())
                val alarmTime = LocalDateTime.of(alarmDate, time)
                alarmTimes.add(alarmTime)
            }
            
            // Schedule alarms with correct indices (starting from nextIncompleteIndex)
            alarmTimes.forEachIndexed { index, time ->
                val alarmIndex = nextIncompleteIndex + index
                alarmScheduler.scheduleExactAlarm(time, alarmIndex)
            }
        }
    }
}


