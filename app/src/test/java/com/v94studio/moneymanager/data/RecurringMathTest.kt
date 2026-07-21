package com.v94studio.moneymanager.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringMathTest {
    @Test
    fun nextFutureRunAt_advancesMissedRunsUntilAfterNow() {
        val day = 24L * 60L * 60L * 1000L

        val next = nextFutureRunAt(
            currentRunAt = 0L,
            intervalDays = 7,
            now = 20L * day
        )

        assertEquals(21L * day, next)
    }

    @Test
    fun nextFutureRunAt_returnsNowWhenIntervalIsInvalid() {
        assertEquals(123L, nextFutureRunAt(currentRunAt = 0L, intervalDays = 0, now = 123L))
    }
}
