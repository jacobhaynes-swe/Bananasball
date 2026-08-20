package com.example.bananasball.data.remote

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit

@OptIn(ExperimentalTime::class)
class MockScheduleScraper : ScheduleScraper {
    override suspend fun fetchSchedule(): List<ScrapedGame> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return listOf(
            ScrapedGame(
                date = today.toString(),
                location = "St. Louis, MO",
                time = "7:00pm CST",
                teamCodes = listOf("SB", "PA"),
                youtubeUrl = "https://www.youtube.com/user/TheSavannahBananas",
                awayScore = 5,
                homeScore = 3,
                status = "Live"
            ),
            ScrapedGame(
                date = today.toString(),
                location = "Oklahoma City, OK",
                time = "7:00pm CST",
                teamCodes = listOf("FF", "TG"),
                youtubeUrl = "https://www.youtube.com/@ThePartyAnimalsBaseball",
                awayScore = 0,
                homeScore = 0,
                status = "Scheduled"
            ),
            ScrapedGame(
                date = today.plus(1, DateTimeUnit.DAY).toString(),
                location = "Foxborough, MA",
                time = "7:00pm EST",
                teamCodes = listOf("SB", "TG"),
                youtubeUrl = null,
                awayScore = 0,
                homeScore = 0,
                status = "Scheduled"
            )
        )
    }
}
