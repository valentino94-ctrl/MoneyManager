package com.v94studio.moneymanager.ui.screens

import android.content.Context
import com.v94studio.moneymanager.R
import java.util.Calendar
import java.util.Locale

internal fun startOfMonthMillis(now: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

internal fun startOfLastMonthMillis(now: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.MONTH, -1)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

internal fun endOfLastMonthMillis(now: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.MILLISECOND, -1)
    }
    return cal.timeInMillis
}

internal fun formatPercent(value: Double): String {
    return String.format(Locale.getDefault(), "%.1f%%", value * 100)
}

internal fun buildMonthBuckets(count: Int, now: Long): List<MonthBucket> {
    val buckets = mutableListOf<MonthBucket>()
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    for (i in 0 until count) {
        val key = java.text.SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
        val label = java.text.SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
        val start = Calendar.getInstance().apply {
            timeInMillis = cal.timeInMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        buckets.add(0, MonthBucket(key, label, start))
        cal.add(Calendar.MONTH, -1)
    }
    return buckets
}

internal fun buildDayBuckets(count: Int, now: Long): List<DayBucket> {
    val buckets = mutableListOf<DayBucket>()
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    for (i in 0 until count) {
        val key = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        val label = java.text.SimpleDateFormat("d", Locale.getDefault()).format(cal.time)
        buckets.add(0, DayBucket(key, label))
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    return buckets
}

internal fun budgetForMonth(perDayBudget: Double, monthStartMillis: Long): Double {
    val cal = Calendar.getInstance().apply { timeInMillis = monthStartMillis }
    val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return perDayBudget * days
}

internal fun relativeDayLabel(context: Context, timestamp: Long): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    return when {
        isSameDay(now, target) -> context.getString(R.string.label_today)
        isTomorrow(now, target) -> context.getString(R.string.label_tomorrow)
        else -> java.text.SimpleDateFormat("MMM d", Locale.getDefault()).format(target.time)
    }
}

private fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
           c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

private fun isTomorrow(now: Calendar, target: Calendar): Boolean {
    val tomorrow = Calendar.getInstance().apply {
        timeInMillis = now.timeInMillis
        add(Calendar.DAY_OF_YEAR, 1)
    }
    return isSameDay(tomorrow, target)
}
