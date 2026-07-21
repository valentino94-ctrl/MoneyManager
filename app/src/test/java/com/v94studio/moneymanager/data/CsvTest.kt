package com.v94studio.moneymanager.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {
    @Test
    fun csvRow_quotesCommasQuotesAndLineBreaks() {
        val row = csvRow(1, "Food, snacks", "He said \"ok\"", "line\nbreak")

        assertEquals("1,\"Food, snacks\",\"He said \"\"ok\"\"\",\"line\nbreak\"", row)
    }

    @Test
    fun csvRow_keepsSimpleValuesUnquoted() {
        val row = csvRow(7, 12.5, "EXPENSE", "Food")

        assertEquals("7,12.5,EXPENSE,Food", row)
    }
}
