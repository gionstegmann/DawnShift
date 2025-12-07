package com.example.dawnshift.logic

import java.time.LocalTime
import java.time.temporal.ChronoUnit

object WakeScheduleCalculator {

    /**
     * Calculates a list of wake-up times from [start] to [target] over [days] days.
     *
     * @param start The current wake-up time.
     * @param target The goal wake-up time.
     * @param days The number of days to transition. Must be at least 1.
     * @return A list of [LocalTime] of size [days], where the first element is the next day's target
     * and the last element is [target].
     *
     * Note: This logic assumes we want to move from 'start' to 'target'.
     * If 'target' is "earlier" than 'start' (e.g. 11:00 -> 08:00), it's a simple subtraction.
     * The interpolation handles the minute difference distributed over the days.
     */
    fun calculateSchedule(start: LocalTime, target: LocalTime, days: Int): List<LocalTime> {
        if (days < 1) return emptyList()

        // Calculate total minutes difference.
        // We assume the target is always "earlier" or "later" within a 24h cycle,
        // but for a wake-up habit, usually we mean the shortest duration difference.
        // However, typically "earlier" means moving backwards in time.
        // E.g. 11:00 -> 08:00 is -180 minutes.
        // E.g. 08:00 -> 07:00 is -60 minutes.
        
        // Let's interpret the difference as: how many minutes do we need to shift 'start' to reach 'target'.
        // If start=11:00, target=08:00.
        // Direct difference: target - start.
        
        val minutesDiff = start.until(target, ChronoUnit.MINUTES)
        
        // If we want to wake up *earlier*, the target is before start, so minutesDiff is negative.
        // E.g. 11:00 to 08:00 -> -180.
        // If we crossed midnight (e.g. start 01:00, target 23:00 previous day), 
        // using simple until might give -120 (2 hours earlier). This is correct.
        
        // What if user inputs start 08:00, target 11:00? (waking up later)
        // minutesDiff is +180.
        
        // The step per day.
        // Day 1 should be start + step.
        // Day 'days' should be target (approximately).
        
        // Actually, usually "Day 1" is tomorrow.
        // If I wake up at 11:00 today (Day 0).
        // I want to wake up at 08:00 in 3 days.
        // Day 1: 10:00
        // Day 2: 09:00
        // Day 3: 08:00
        // So step = diff / days.
        
        val stepMinutes = minutesDiff.toDouble() / days
        
        val schedule = mutableListOf<LocalTime>()
        for (i in 1..days) {
            val shift = (stepMinutes * i).toLong()
            schedule.add(start.plusMinutes(shift))
        }
        
        return schedule
    }
}
