package com.example.bananasball.data.mapper

import com.example.bananasball.data.local.GameEntity
import com.example.bananasball.data.remote.ScrapedGame
import com.example.bananasball.data.repository.StaticTeamProvider
import com.example.bananasball.domain.model.BoxScore
import com.example.bananasball.domain.model.Game
import com.example.bananasball.domain.model.StreamingMetadata
import com.example.bananasball.domain.model.Team
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime
import kotlinx.datetime.*

fun GameEntity.toDomain(): Game {
    val fallbackHome = StaticTeamProvider.getTeam(homeTeamId)
    val fallbackAway = StaticTeamProvider.getTeam(awayTeamId)

    val home = fallbackHome?.copy(
        name = if (homeTeamName.isNotBlank()) homeTeamName else fallbackHome.name,
        shortName = if (homeTeamShort.isNotBlank()) homeTeamShort else fallbackHome.shortName
    ) ?: Team(
        id = homeTeamId,
        name = homeTeamName,
        shortName = homeTeamShort
    )

    val away = fallbackAway?.copy(
        name = if (awayTeamName.isNotBlank()) awayTeamName else fallbackAway.name,
        shortName = if (awayTeamShort.isNotBlank()) awayTeamShort else fallbackAway.shortName
    ) ?: Team(
        id = awayTeamId,
        name = awayTeamName,
        shortName = awayTeamShort
    )

    return Game(
        id = id,
        homeTeam = home,
        awayTeam = away,
        startTime = LocalDateTime.parse(startTime),
        youtubeUrl = youtubeUrl ?: fallbackHome?.youtubeChannelUrl ?: fallbackAway?.youtubeChannelUrl,
        boxScore = BoxScore(awayScore = awayScore, homeScore = homeScore, status = status),
        location = location,
        streamingMetadata = StreamingMetadata(
            thumbnailUrl = thumbnailUrl,
            waitingCount = waitingCount,
            actualStartTime = actualStartTime?.let { LocalDateTime.parse(it) },
            streamTitle = streamTitle
        )
    )
}

@OptIn(ExperimentalTime::class)
fun ScrapedGame.toEntity(): GameEntity {
    val homeCode = teamCodes.getOrNull(0) ?: "HOME"
    val awayCode = teamCodes.getOrNull(1) ?: "AWAY"
    
    val homeTeam = StaticTeamProvider.getTeam(homeCode)
    val awayTeam = StaticTeamProvider.getTeam(awayCode)

    val parsedDate = parseDate(date)
    val isoDate = parsedDate.toString()
    
    val parsedStartTime = parseGameDateTime(date, time, actualStartTime)

    return GameEntity(
        id = "${isoDate}_${homeCode}_${awayCode}",
        homeTeamId = homeCode,
        homeTeamName = homeTeam?.name ?: "Team $homeCode",
        homeTeamShort = homeTeam?.shortName ?: homeCode,
        awayTeamId = awayCode,
        awayTeamName = awayTeam?.name ?: "Team $awayCode",
        awayTeamShort = awayTeam?.shortName ?: awayCode,
        startTime = parsedStartTime,
        youtubeUrl = youtubeUrl ?: homeTeam?.youtubeChannelUrl ?: awayTeam?.youtubeChannelUrl,
        awayScore = awayScore ?: 0,
        homeScore = homeScore ?: 0,
        status = status ?: "Scheduled",
        location = location,
        date = isoDate,
        thumbnailUrl = thumbnailUrl,
        waitingCount = waitingCount,
        actualStartTime = parsedStartTime,
        streamTitle = streamTitle
    )
}

/**
 * Parses date and time into the user's local timezone LocalDateTime string.
 */
@OptIn(ExperimentalTime::class)
fun parseGameDateTime(dateStr: String, timeStr: String, actualStartTimeSeconds: String?): String {
    // 1. Exact Unix timestamp from YouTube scheduled live stream
    actualStartTimeSeconds?.toLongOrNull()?.let { seconds ->
        return Instant.fromEpochSeconds(seconds)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .toString()
    }

    val parsedDate = parseDate(dateStr)

    // 2. Parse timeStr (e.g., "7:00pm MST", "7:00 PM EST", "1:30 PM CST", "7:00pm")
    val timeRegex = Regex("(\\d{1,2}):(\\d{2})\\s*([aApP][mM])?\\s*([A-Za-z]+)?")
    val match = timeRegex.find(timeStr)

    if (match != null) {
        var hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        val amPm = match.groupValues[3].uppercase()
        val tzStr = match.groupValues[4].uppercase()

        if (amPm == "PM" && hour < 12) {
            hour += 12
        } else if (amPm == "AM" && hour == 12) {
            hour = 0
        } else if (amPm.isEmpty() && hour < 12 && hour in 1..8) {
            // Default to evening game
            hour += 12
        }

        val gameZone = when (tzStr) {
            "EST", "EDT", "ET" -> TimeZone.of("America/New_York")
            "CST", "CDT", "CT" -> TimeZone.of("America/Chicago")
            "MST", "MDT", "MT" -> TimeZone.of("America/Denver")
            "PST", "PDT", "PT" -> TimeZone.of("America/Los_Angeles")
            else -> TimeZone.of("America/New_York")
        }

        try {
            val localInGameZone = LocalDateTime(parsedDate.year, parsedDate.month, parsedDate.day, hour, minute)
            val instant = localInGameZone.toInstant(gameZone)
            return instant.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
        } catch (e: Exception) {
            return LocalDateTime(parsedDate.year, parsedDate.month, parsedDate.day, hour, minute).toString()
        }
    }

    // Default 7:00 PM Eastern
    val defaultEastern = LocalDateTime(parsedDate.year, parsedDate.month, parsedDate.day, 19, 0)
    return try {
        val instant = defaultEastern.toInstant(TimeZone.of("America/New_York"))
        instant.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
    } catch (e: Exception) {
        defaultEastern.toString()
    }
}

/**
 * Parses a date string like "Thursday, August 20" into a LocalDate.
 * Assumes the current year.
 */
@OptIn(ExperimentalTime::class)
private fun parseDate(dateStr: String): LocalDate {
    try {
        val parts = dateStr.split(",").last().trim().split(" ")
        if (parts.size >= 2) {
            val monthStr = parts[0]
            val day = parts[1].toInt()
            val month = Month.valueOf(monthStr.uppercase())
            val year = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
            return LocalDate(year, month, day)
        }
    } catch (e: Exception) {
        // Fallback to today if parsing fails
    }
    return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
