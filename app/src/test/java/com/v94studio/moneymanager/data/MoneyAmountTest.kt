package com.v94studio.moneymanager.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyAmountTest {
    @Test
    fun amountToMinor_roundsToNearestMinorUnit() {
        assertEquals(1235L, amountToMinor(12.345))
        assertEquals(1234L, amountToMinor(12.344))
    }

    @Test
    fun minorToAmount_convertsBackToMajorUnits() {
        assertEquals(12.34, minorToAmount(1234L), 0.0)
    }
}
