package com.example.bananasball.data.mapper

import com.example.bananasball.data.local.GameEntity
import com.example.bananasball.data.remote.ScrapedGame
import com.example.bananasball.domain.model.BoxScore
import com.example.bananasball.domain.model.Game
import com.example.bananasball.domain.model.Team
import kotlinx.datetime.*

fun GameEntity.toDomain(): Game {
    return Game(
        id = id,
        homeTeam = Team(id = homeTeamId, name = homeTeamName, shortName = homeTeamShort),
        awayTeam = Team(id = awayTeamId, name = awayTeamName, shortName = awayTeamShort),
        startTime = LocalDateTime.parse(startTime),
        youtubeUrl = youtubeUrl,
        boxScore = BoxScore(awayScore = awayScore, homeScore = homeScore, status = status),
        location = location
    )
}

fun ScrapedGame.toEntity(): GameEntity {
    // Basic mapping logic for the first iteration
    val homeCode = teamCodes.getOrNull(0) ?: "HOME"
    val awayCode = teamCodes.getOrNull(1) ?: "AWAY"
    
    val parsedDate = parseDate(date)
    val isoDate = parsedDate.toString()
    
    return GameEntity(
        id = "${isoDate}_${homeCode}_${awayCode}",
        homeTeamId = homeCode,
        homeTeamName = getFullTeamName(homeCode),
        homeTeamShort = homeCode,
        awayTeamId = awayCode,
        awayTeamName = getFullTeamName(awayCode),
        awayTeamShort = awayCode,
        startTime = "${isoDate}T19:00:00", // Placeholder for now
        youtubeUrl = youtubeUrl,
        awayScore = awayScore ?: 0,
        homeScore = homeScore ?: 0,
        status = status ?: "Scheduled",
        location = location,
        date = isoDate
    )
}

/**
 * Parses a date string like "Thursday, August 20" into a LocalDate.
 * Assumes the current year.
 */
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

private fun getFullTeamName(code: String): String = when (code) {
    "SB" -> "Savannah Bananas"
    "PA" -> "Party Animals"
    "FF" -> "Firefighters"
    "TG" -> "Texas Tailgaters"
    "IC" -> "Indianapolis Clowns"
    else -> "TBD ($code)"
}
