package com.example.bananasball.data.mapper

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameMapperTest {

    @Test
    fun testParseGameDateTimeWithTz() {
        val parsedMST = parseGameDateTime("Thursday, August 20", "7:00pm MST", null)
        assertNotNull(parsedMST)
        assertTrue(parsedMST.contains("2026-08-20") || parsedMST.contains("2026-08-21"))

        val parsedEST = parseGameDateTime("Friday, August 21", "7:00 PM EST", null)
        assertNotNull(parsedEST)
        assertTrue(parsedEST.contains("2026-08-21"))
    }

    @Test
    fun testParseGameDateTimeWithActualEpoch() {
        // Unix timestamp for 2026-08-20 23:00:00 UTC
        val epochSeconds = "1787266800"
        val parsed = parseGameDateTime("Thursday, August 20", "7:00pm", epochSeconds)
        assertNotNull(parsed)
        assertTrue(parsed.contains("2026"))
    }
}
