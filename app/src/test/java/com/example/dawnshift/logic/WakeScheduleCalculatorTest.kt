package com.example.dawnshift.logic

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class WakeScheduleCalculatorTest {

    @Test
    fun `calculateSchedule returns correct list size`() {
        val start = LocalTime.of(11, 0)
        val target = LocalTime.of(8, 0)
        val days = 3

        val result = WakeScheduleCalculator.calculateSchedule(start, target, days)

        assertEquals(3, result.size)
    }

    @Test
    fun `calculateSchedule last element is target`() {
        val start = LocalTime.of(11, 0)
        val target = LocalTime.of(8, 0)
        val days = 3

        val result = WakeScheduleCalculator.calculateSchedule(start, target, days)

        assertEquals(target, result.last())
    }

    @Test
    fun `calculateSchedule interpolates correctly`() {
        // 11:00 -> 08:00 is -3 hours (-180 mins).
        // 3 days. Step = -60 mins/day.
        // Day 1: 10:00
        // Day 2: 09:00
        // Day 3: 08:00
        val start = LocalTime.of(11, 0)
        val target = LocalTime.of(8, 0)
        val days = 3

        val result = WakeScheduleCalculator.calculateSchedule(start, target, days)
        
        assertEquals(LocalTime.of(10, 0), result[0])
        assertEquals(LocalTime.of(9, 0), result[1])
        assertEquals(LocalTime.of(8, 0), result[2])
    }
    
    @Test
    fun `calculateSchedule handles small shift`() {
        // 08:00 -> 07:30 is -30 mins.
        // 2 days. Step = -15 mins.
        // Day 1: 07:45
        // Day 2: 07:30
        
        val start = LocalTime.of(8, 0)
        val target = LocalTime.of(7, 30)
        val days = 2
        
        val result = WakeScheduleCalculator.calculateSchedule(start, target, days)
        
        assertEquals(LocalTime.of(7, 45), result[0])
        assertEquals(LocalTime.of(7, 30), result[1])
    }
}
