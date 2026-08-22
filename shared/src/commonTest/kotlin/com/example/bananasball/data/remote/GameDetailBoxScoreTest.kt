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

class GameDetailBoxScoreTest {

    private val sampleBoxScoreJson = """
    {
      "gameId": "5ce11223-0756-46c6-a8fc-3db3d34e05ec",
      "gameDate": "2026-08-22",
      "time": "19:00:00",
      "status": "final",
      "venue": {
        "id": "357a49c3-b030-4d64-9b1d-63bf6b084e79",
        "name": "Busch Stadium",
        "city": "St. Louis",
        "state": "Missouri",
        "timezone": "America/Chicago"
      },
      "numberOfInnings": 9,
      "equalizerPointAwarded": false,
      "equalizerPointInning": null,
      "teams": [
        {
          "teamId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
          "teamName": "Savannah Bananas",
          "teamAbbreviation": "SB",
          "isHomeTeam": true,
          "prh": {
            "points_regular": 4,
            "points_sd": 0,
            "points_total": 4,
            "runs": 9,
            "hits": 9
          },
          "lineScore": {
            "innings": [
              { "inning": 1, "runs": 4, "hits": 2, "points_awarded": 1 },
              { "inning": 2, "runs": 0, "hits": 1, "points_awarded": 0 },
              { "inning": 9, "runs": 3, "hits": 3, "points_awarded": 2 }
            ],
            "showdown": []
          },
          "batters": [
            {
              "playerId": "p1",
              "name": "DR Meadows",
              "jersey_number": 5,
              "order": 1,
              "positions": ["CF"],
              "hitting_roles": [],
              "AB": 3,
              "R": 2,
              "H": 1,
              "RBI": 0,
              "B4S": 2,
              "K": 0,
              "WO": 0,
              "SB": 1,
              "AVG": 0.352,
              "OPS": 0.907
            }
          ],
          "pitchers": [
            {
              "playerId": "p2",
              "name": "Austin Drury",
              "jersey_number": 11,
              "designations": ["SP"],
              "IP": "2.2",
              "H": 12,
              "R": 11,
              "ER": 3,
              "BB": null,
              "K": 4,
              "ERA": 4.96,
              "MPI": "4:30"
            }
          ]
        },
        {
          "teamId": "party-animals-id",
          "teamName": "Party Animals",
          "teamAbbreviation": "PA",
          "isHomeTeam": false,
          "prh": {
            "points_regular": 3,
            "points_sd": 0,
            "points_total": 3,
            "runs": 6,
            "hits": 7
          },
          "lineScore": {
            "innings": [
              { "inning": 1, "runs": 1, "hits": 1, "points_awarded": 0 },
              { "inning": 2, "runs": 2, "hits": 2, "points_awarded": 1 }
            ],
            "showdown": []
          },
          "batters": [],
          "pitchers": []
        }
      ]
    }
    """.trimIndent()

    @Test
    fun testFetchGameBoxScoreParsesAccurately() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel(sampleBoxScoreJson),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val scraper = KtorScheduleScraper(HttpClient(mockEngine))
        val result = scraper.fetchGameBoxScore("5ce11223-0756-46c6-a8fc-3db3d34e05ec")

        assertTrue(result.isSuccess)
        val detail = result.getOrNull()
        assertNotNull(detail)

        assertEquals("5ce11223-0756-46c6-a8fc-3db3d34e05ec", detail.gameId)
        assertEquals("Busch Stadium", detail.venue?.name)
        assertEquals("St. Louis", detail.venue?.city)
        assertEquals("Missouri", detail.venue?.state)

        // Home Team
        assertEquals("Savannah Bananas", detail.homeTeam.name)
        assertEquals(4, detail.homeTeam.pointsTotal)
        assertEquals(9, detail.homeTeam.runsTotal)
        assertEquals(3, detail.homeTeam.innings.size)
        assertEquals(2, detail.homeTeam.innings.last().pointsAwarded) // 9th inning 2 pts

        // Batter with Ball Four Sprint
        assertEquals(1, detail.homeTeam.batters.size)
        val batter = detail.homeTeam.batters.first()
        assertEquals("DR Meadows", batter.name)
        assertEquals(5, batter.jerseyNumber)
        assertEquals(2, batter.ballFourSprints)
        assertEquals(listOf("CF"), batter.positions)

        // Pitcher with MPI
        assertEquals(1, detail.homeTeam.pitchers.size)
        val pitcher = detail.homeTeam.pitchers.first()
        assertEquals("Austin Drury", pitcher.name)
        assertEquals("2.2", pitcher.inningsPitched)
        assertEquals(4, pitcher.strikeouts)
        assertEquals("4:30", pitcher.minutesPerInning)
    }
}
