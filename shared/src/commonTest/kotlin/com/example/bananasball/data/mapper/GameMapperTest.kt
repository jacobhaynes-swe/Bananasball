package com.example.bananasball.data.mapper

import com.example.bananasball.data.remote.ScrapedGame
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

    @Test
    fun testCanonicalGameIdIndependentOfTeamOrder() {
        val game1 = ScrapedGame(
            date = "2026-08-22",
            time = "19:00:00",
            teamCodes = listOf("TG", "IC"),
            location = "Chickasaw Bricktown Ballpark",
            youtubeUrl = "https://www.youtube.com/watch?v=MGAzVrF3ANw"
        )
        val game2 = ScrapedGame(
            date = "Saturday, August 22",
            time = "7:00pm CDT",
            teamCodes = listOf("IC", "TG"),
            location = "Chickasaw Bricktown Ballpark",
            youtubeUrl = "https://www.youtube.com/@TheTexasTailgaters/streams"
        )

        val entity1 = game1.toEntity()
        val entity2 = game2.toEntity()

        kotlin.test.assertEquals("2026-08-22_IC_TG", entity1.id)
        kotlin.test.assertEquals(entity1.id, entity2.id, "Canonical ID must match regardless of home/away order")
    }
}
