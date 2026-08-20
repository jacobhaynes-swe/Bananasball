package com.example.bananasball.data.remote

import io.ktor.client.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KtorStatsScraperTest {

    @Test
    fun testFallbackStandingsAvailable() = runTest {
        val scraper = KtorStatsScraper(HttpClient())
        val standings = scraper.fetchStandings()
        assertNotNull(standings)
        assertTrue(standings.rankings.isNotEmpty(), "Standings should have team rankings")
        assertTrue(standings.rankings.size == 6, "Expected 6 teams in standings")
    }

    @Test
    fun testFallbackSeasonStatsAvailable() = runTest {
        val scraper = KtorStatsScraper(HttpClient())
        val stats = scraper.fetchSeasonStats()
        assertNotNull(stats)
        assertTrue(stats.battingLeaders.isNotEmpty(), "Batting leaders should not be empty")
        assertTrue(stats.pitchingLeaders.isNotEmpty(), "Pitching leaders should not be empty")
    }
}
