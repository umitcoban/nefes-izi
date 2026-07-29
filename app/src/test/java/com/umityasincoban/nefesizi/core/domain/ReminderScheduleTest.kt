package com.umityasincoban.nefesizi.core.domain

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderScheduleTest {
    @Test
    fun `daily reminder later today stays today`() {
        val delay = nextDailyReminderDelay(
            LocalDateTime.of(2026, 7, 29, 18, 0),
            LocalTime.of(21, 0),
        )

        assertEquals(3, delay.toHours())
    }

    @Test
    fun `daily reminder at or after configured time moves to tomorrow`() {
        val atTime = nextDailyReminderDelay(
            LocalDateTime.of(2026, 7, 29, 21, 0),
            LocalTime.of(21, 0),
        )
        val afterTime = nextDailyReminderDelay(
            LocalDateTime.of(2026, 7, 29, 22, 0),
            LocalTime.of(21, 0),
        )

        assertEquals(24, atTime.toHours())
        assertEquals(23, afterTime.toHours())
    }

    @Test
    fun `weekly reminder uses same week when target is ahead`() {
        val delay = nextWeeklyReminderDelay(
            LocalDateTime.of(2026, 7, 27, 19, 0),
            DayOfWeek.SUNDAY,
            LocalTime.of(19, 0),
        )

        assertEquals(6 * 24L, delay.toHours())
    }

    @Test
    fun `weekly reminder at exact target moves seven days`() {
        val delay = nextWeeklyReminderDelay(
            LocalDateTime.of(2026, 8, 2, 19, 0),
            DayOfWeek.SUNDAY,
            LocalTime.of(19, 0),
        )

        assertEquals(7 * 24L, delay.toHours())
    }
}
