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
        boxScore = BoxScore(
            awayScore = awayScore,
            homeScore = homeScore,
            status = status,
            awayRuns = awayRuns,
            homeRuns = homeRuns,
            awayHits = awayHits,
            homeHits = homeHits,
            currentInning = currentInning,
            inningHalf = inningHalf,
            outs = outs,
            inningDisplay = inningDisplay
        ),
        location = location,
        streamingMetadata = StreamingMetadata(
            thumbnailUrl = thumbnailUrl,
            waitingCount = waitingCount,
            viewerCount = viewerCount,
            isLiveBroadcast = isLiveBroadcast,
            actualStartTime = actualStartTime?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
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
    
    val parsedGameStartTime = parseGameDateTime(date, time, null)
    val parsedStreamStartTime = actualStartTime?.let { raw ->
        raw.toLongOrNull()?.let { seconds ->
            runCatching {
                Instant.fromEpochSeconds(seconds)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .toString()
            }.getOrNull()
        } ?: runCatching {
            Instant.parse(raw)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toString()
        }.getOrNull()
    }

    return GameEntity(
        id = "${isoDate}_${homeCode}_${awayCode}",
        homeTeamId = homeCode,
        homeTeamName = homeTeam?.name ?: "Team $homeCode",
        homeTeamShort = homeTeam?.shortName ?: homeCode,
        awayTeamId = awayCode,
        awayTeamName = awayTeam?.name ?: "Team $awayCode",
        awayTeamShort = awayTeam?.shortName ?: awayCode,
        startTime = parsedGameStartTime,
        youtubeUrl = youtubeUrl ?: homeTeam?.youtubeChannelUrl ?: awayTeam?.youtubeChannelUrl,
        awayScore = awayScore ?: 0,
        homeScore = homeScore ?: 0,
        status = status ?: "Scheduled",
        location = if (location.isNotBlank()) location else "${homeTeam?.name ?: "Banana Ball"} Ballpark",
        date = isoDate,
        thumbnailUrl = thumbnailUrl,
        waitingCount = waitingCount,
        viewerCount = viewerCount,
        isLiveBroadcast = isLiveBroadcast,
        actualStartTime = parsedStreamStartTime,
        streamTitle = streamTitle,
        awayRuns = awayRuns,
        homeRuns = homeRuns,
        awayHits = awayHits,
        homeHits = homeHits,
        currentInning = currentInning,
        inningHalf = inningHalf,
        outs = outs,
        inningDisplay = inningDisplay
    )
}

/**
 * Parses date and time into the user's local timezone LocalDateTime string.
 */
@OptIn(ExperimentalTime::class)
fun parseGameDateTime(dateStr: String, timeStr: String, actualStartTimeSeconds: String?): String {
    actualStartTimeSeconds?.toLongOrNull()?.let { seconds ->
        return Instant.fromEpochSeconds(seconds)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .toString()
    }

    val parsedDate = parseDate(dateStr)

    val timeRegex = Regex("(\\d{1,2}):(\\d{2})(:\\d{2})?\\s*([aApP][mM])?\\s*([A-Za-z/_]+)?")
    val match = timeRegex.find(timeStr)

    if (match != null) {
        var hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        val amPm = match.groupValues[4].uppercase()
        val tzStr = match.groupValues[5].trim()

        if (amPm == "PM" && hour < 12) {
            hour += 12
        } else if (amPm == "AM" && hour == 12) {
            hour = 0
        }

        val gameZone = when {
            tzStr.contains("Denver", ignoreCase = true) || tzStr.equals("MST", true) || tzStr.equals("MDT", true) || tzStr.equals("MT", true) -> TimeZone.of("America/Denver")
            tzStr.contains("Chicago", ignoreCase = true) || tzStr.equals("CST", true) || tzStr.equals("CDT", true) || tzStr.equals("CT", true) -> TimeZone.of("America/Chicago")
            tzStr.contains("New_York", ignoreCase = true) || tzStr.equals("EST", true) || tzStr.equals("EDT", true) || tzStr.equals("ET", true) -> TimeZone.of("America/New_York")
            tzStr.contains("Los_Angeles", ignoreCase = true) || tzStr.equals("PST", true) || tzStr.equals("PDT", true) || tzStr.equals("PT", true) -> TimeZone.of("America/Los_Angeles")
            tzStr.isNotBlank() && runCatching { TimeZone.of(tzStr) }.isSuccess -> TimeZone.of(tzStr)
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

    val defaultEastern = LocalDateTime(parsedDate.year, parsedDate.month, parsedDate.day, 19, 0)
    return try {
        val instant = defaultEastern.toInstant(TimeZone.of("America/New_York"))
        instant.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
    } catch (e: Exception) {
        defaultEastern.toString()
    }
}

/**
 * Parses a date string like "Thursday, August 20" or "2026-08-20" into a LocalDate.
 */
@OptIn(ExperimentalTime::class)
fun parseDate(dateStr: String): LocalDate {
    try {
        if (dateStr.contains("-")) {
            return LocalDate.parse(dateStr.trim())
        }
        val parts = dateStr.split(",").last().trim().split(" ")
        if (parts.size >= 2) {
            val monthStr = parts[0].uppercase()
            val day = parts[1].toInt()
            val month = Month.valueOf(monthStr)
            val year = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
            return LocalDate(year, month, day)
        }
    } catch (e: Exception) {
        // Fallback to today if parsing fails
    }
    return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
