package com.example.bananasball.data.remote

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KtorStatsScraperTest {

    private val sampleHittingJson = """
    {
        "data": [
            {
                "first_name": "One",
                "last_name": "Hitter",
                "at_bats": 1,
                "hits": 1,
                "games_played": 1,
                "batting_average": 1.000,
                "home_runs": 0,
                "runs_batted_in": 0,
                "on_base_plus_slugging": 2.0,
                "ball_four_sprints": 0,
                "stolen_bases": 0,
                "team": { "name": "Savannah Bananas", "abbreviation": "SB" }
            },
            {
                "first_name": "Tanner",
                "last_name": "Allen",
                "at_bats": 209,
                "hits": 84,
                "games_played": 50,
                "batting_average": 0.402,
                "home_runs": 7,
                "runs_batted_in": 36,
                "on_base_plus_slugging": 1.074,
                "ball_four_sprints": 12,
                "stolen_bases": 3,
                "team": { "name": "Loco Beach Coconuts", "abbreviation": "LBC" }
            },
            {
                "first_name": "Jackie",
                "last_name": "Bradley Jr.",
                "at_bats": 190,
                "hits": 73,
                "games_played": 49,
                "batting_average": 0.384,
                "home_runs": 8,
                "runs_batted_in": 39,
                "on_base_plus_slugging": 1.022,
                "ball_four_sprints": 23,
                "stolen_bases": 4,
                "team": { "name": "Indianapolis Clowns", "abbreviation": "IND" }
            }
        ]
    }
    """.trimIndent()

    private val samplePitchingJson = """
    {
        "data": [
            {
                "first_name": "One",
                "last_name": "InningGuy",
                "innings_pitched": 1.0,
                "earned_run_average": 0.00,
                "wins": 0,
                "pitcher_strikeouts": 1,
                "saves": 0,
                "hits_allowed": 0,
                "sprints_allowed": 0,
                "team": { "name": "Party Animals", "abbreviation": "PA" }
            },
            {
                "first_name": "Danny",
                "last_name": "Hosley",
                "innings_pitched": 40.2,
                "earned_run_average": 1.77,
                "wins": 7,
                "pitcher_strikeouts": 51,
                "saves": 11,
                "hits_allowed": 30,
                "sprints_allowed": 12,
                "team": { "name": "Savannah Bananas", "abbreviation": "SAV" }
            },
            {
                "first_name": "Chris",
                "last_name": "Clarke",
                "innings_pitched": 58.1,
                "earned_run_average": 2.78,
                "wins": 3,
                "pitcher_strikeouts": 60,
                "saves": 0,
                "hits_allowed": 55,
                "sprints_allowed": 22,
                "team": { "name": "Texas Tailgaters", "abbreviation": "TEX" }
            }
        ]
    }
    """.trimIndent()

    private val sampleStandingsJson = """
    {
        "standings": [
            {
                "rank": 1,
                "team_name": "Texas Tailgaters",
                "team_abbreviation": "TEX",
                "wins": 27,
                "losses": 22,
                "win_pct": 0.551,
                "games_back": 0.0,
                "streak": "W1",
                "point_differential": 21
            },
            {
                "rank": 2,
                "team_name": "Savannah Bananas",
                "team_abbreviation": "SAV",
                "wins": 26,
                "losses": 22,
                "win_pct": 0.542,
                "games_back": 0.5,
                "streak": "L3",
                "point_differential": 24
            },
            {
                "rank": 3,
                "team_name": "Loco Beach Coconuts",
                "team_abbreviation": "LBC",
                "wins": 26,
                "losses": 24,
                "win_pct": 0.520,
                "games_back": 1.5,
                "streak": "L1",
                "point_differential": 11
            },
            {
                "rank": 4,
                "team_name": "Firefighters",
                "team_abbreviation": "FF",
                "wins": 23,
                "losses": 26,
                "win_pct": 0.469,
                "games_back": 4.0,
                "streak": "W4",
                "point_differential": -27
            },
            {
                "rank": 5,
                "team_name": "Party Animals",
                "team_abbreviation": "PA",
                "wins": 23,
                "losses": 26,
                "win_pct": 0.469,
                "games_back": 4.0,
                "streak": "L4",
                "point_differential": -3
            },
            {
                "rank": 6,
                "team_name": "Indianapolis Clowns",
                "team_abbreviation": "IND",
                "wins": 22,
                "losses": 27,
                "win_pct": 0.449,
                "games_back": 5.0,
                "streak": "W3",
                "point_differential": -19
            }
        ]
    }
    """.trimIndent()

    private fun createFullMockEngine(): MockEngine {
        return MockEngine { request ->
            val url = request.url.toString()
            val responseBody = when {
                url.contains("subCategory=hitting") -> sampleHittingJson
                url.contains("subCategory=pitching") -> samplePitchingJson
                url.contains("standings") -> sampleStandingsJson
                else -> "{}"
            }
            respond(
                content = ByteReadChannel(responseBody),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
    }

    @Test
    fun testHittingLeadersFiltersUnqualifiedHittersUnder40AB() = runTest {
        val scraper = KtorStatsScraper(HttpClient(createFullMockEngine()))
        val stats = scraper.fetchSeasonStats()

        // Unqualified hitter with 1 AB should be excluded from leaders
        val playerNames = stats.battingLeaders.map { it.player }
        assertTrue(!playerNames.contains("One Hitter"), "Unqualified hitter with < 40 AB must be excluded")

        // Qualified hitters should be ranked by hits descending
        assertEquals(2, stats.battingLeaders.size)
        assertEquals("Tanner Allen", stats.battingLeaders[0].player)
        assertEquals(1, stats.battingLeaders[0].rank)
        assertEquals(84, stats.battingLeaders[0].hits)
        assertEquals(0.402, stats.battingLeaders[0].avg)
        assertEquals("LBC", stats.battingLeaders[0].team.id)

        assertEquals("Jackie Bradley Jr.", stats.battingLeaders[1].player)
        assertEquals(2, stats.battingLeaders[1].rank)
        assertEquals(73, stats.battingLeaders[1].hits)
        assertEquals("IC", stats.battingLeaders[1].team.id)
    }

    @Test
    fun testPitchingLeadersFiltersUnqualifiedPitchersUnder15IP() = runTest {
        val scraper = KtorStatsScraper(HttpClient(createFullMockEngine()))
        val stats = scraper.fetchSeasonStats()

        // Unqualified pitcher with 1.0 IP must be excluded
        val pitcherNames = stats.pitchingLeaders.map { it.player }
        assertTrue(!pitcherNames.contains("One InningGuy"), "Unqualified pitcher with < 15 IP must be excluded")

        // Top qualified pitcher by ERA
        assertEquals("Danny Hosley", stats.pitchingLeaders[0].player)
        assertEquals(1, stats.pitchingLeaders[0].rank)
        assertEquals(1.77, stats.pitchingLeaders[0].era)
        assertEquals(7, stats.pitchingLeaders[0].wins)
        assertEquals(51, stats.pitchingLeaders[0].so)
        assertEquals(11, stats.pitchingLeaders[0].saves)
        assertEquals("SB", stats.pitchingLeaders[0].team.id)

        assertEquals("Chris Clarke", stats.pitchingLeaders[1].player)
        assertEquals(2, stats.pitchingLeaders[1].rank)
        assertEquals(2.78, stats.pitchingLeaders[1].era)
        assertEquals("TG", stats.pitchingLeaders[1].team.id)
    }

    @Test
    fun testStandingsParsesDirectusJsonCorrectly() = runTest {
        val scraper = KtorStatsScraper(HttpClient(createFullMockEngine()))
        val standings = scraper.fetchStandings()

        assertEquals(6, standings.rankings.size)
        assertEquals("TG", standings.rankings[0].team.id)
        assertEquals(27, standings.rankings[0].wins)
        assertEquals("W1", standings.rankings[0].streak)

        assertEquals("SB", standings.rankings[1].team.id)
        assertEquals("LBC", standings.rankings[2].team.id)
        assertEquals("FF", standings.rankings[3].team.id)
        assertEquals("PA", standings.rankings[4].team.id)
        assertEquals("IC", standings.rankings[5].team.id)
    }

    @Test
    fun testFallbackStandingsAndStatsResilience() = runTest {
        val errorEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel("Internal Server Error"),
                status = HttpStatusCode.InternalServerError
            )
        }

        val scraper = KtorStatsScraper(HttpClient(errorEngine))
        val standings = scraper.fetchStandings()
        val stats = scraper.fetchSeasonStats()

        assertNotNull(standings)
        assertEquals(6, standings.rankings.size, "Fallback must supply all 6 franchises")
        assertEquals("TG", standings.rankings[0].team.id)

        assertNotNull(stats)
        assertTrue(stats.battingLeaders.isNotEmpty(), "Fallback batting leaders should exist")
        assertEquals("Tanner Allen", stats.battingLeaders[0].player)
        assertTrue(stats.pitchingLeaders.isNotEmpty(), "Fallback pitching leaders should exist")
        assertEquals("Danny Hosley", stats.pitchingLeaders[0].player)
    }
}
