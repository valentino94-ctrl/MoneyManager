package com.v94studio.moneymanager.data

import java.util.Calendar
import java.util.Date

fun nextFutureRunAt(
    currentRunAt: Long,
    intervalDays: Int,
    now: Long
): Long {
    if (intervalDays <= 0) return now

    var runAt = currentRunAt
    val intervalMillis = intervalDays * DAY_MILLIS
    while (runAt <= now) {
        runAt += intervalMillis
    }
    return runAt
}

fun isRecurringOnDate(recurring: RecurringEntity, date: Date): Boolean {
    if (recurring.intervalDays <= 0) return false

    val targetCal = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val runCal = Calendar.getInstance().apply {
        timeInMillis = recurring.nextRunAt
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (targetCal.before(runCal)) return false

    val diffMillis = targetCal.timeInMillis - runCal.timeInMillis
    val diffDays = diffMillis / DAY_MILLIS

    return diffDays % recurring.intervalDays == 0L
}

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
