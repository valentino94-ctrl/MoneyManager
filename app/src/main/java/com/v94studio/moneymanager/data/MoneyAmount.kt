package com.v94studio.moneymanager.data

import kotlin.math.roundToLong

internal const val MINOR_UNITS_PER_MAJOR = 100L

fun amountToMinor(amount: Double): Long {
    return (amount * MINOR_UNITS_PER_MAJOR).roundToLong()
}

fun minorToAmount(amountMinor: Long): Double {
    return amountMinor.toDouble() / MINOR_UNITS_PER_MAJOR
}
