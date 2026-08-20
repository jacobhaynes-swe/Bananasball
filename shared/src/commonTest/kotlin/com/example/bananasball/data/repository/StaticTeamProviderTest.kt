package com.example.bananasball.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StaticTeamProviderTest {

    @Test
    fun testAllSixTeamsPresent() {
        val teams = StaticTeamProvider.getAllTeams()
        assertEquals(6, teams.size, "Should have 6 official Banana Ball teams")

        val ids = teams.map { it.id }.toSet()
        assertTrue(ids.contains("SB"))
        assertTrue(ids.contains("PA"))
        assertTrue(ids.contains("FF"))
        assertTrue(ids.contains("TG"))
        assertTrue(ids.contains("IC"))
        assertTrue(ids.contains("LBC"))
    }

    @Test
    fun testGetCodeFromName() {
        assertEquals("SB", StaticTeamProvider.getCodeFromName("Savannah Bananas"))
        assertEquals("PA", StaticTeamProvider.getCodeFromName("Party Animals"))
        assertEquals("FF", StaticTeamProvider.getCodeFromName("Firefighters"))
        assertEquals("TG", StaticTeamProvider.getCodeFromName("Texas Tailgaters"))
        assertEquals("IC", StaticTeamProvider.getCodeFromName("Indianapolis Clowns"))
        assertEquals("LBC", StaticTeamProvider.getCodeFromName("Loco Beach Coconuts"))
    }

    @Test
    fun testTeamMetadataDetails() {
        val bananas = StaticTeamProvider.getTeam("SB")
        assertNotNull(bananas)
        assertEquals("Savannah Bananas", bananas.name)
        assertNotNull(bananas.primaryColorHex)
        assertNotNull(bananas.rosterUrl)
        assertNotNull(bananas.youtubeChannelUrl)
    }
}
