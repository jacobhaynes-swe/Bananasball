package com.example.bananasball.domain.model

data class GameDetail(
    val gameId: String,
    val status: String,
    val venue: VenueDetail?,
    val numberOfInnings: Int,
    val equalizerPointAwarded: Boolean,
    val equalizerPointInning: Int?,
    val homeTeam: TeamGameDetail,
    val awayTeam: TeamGameDetail,
    val notes: String? = null
)

data class VenueDetail(
    val name: String,
    val city: String?,
    val state: String?,
    val timezone: String?
)

data class TeamGameDetail(
    val teamId: String,
    val name: String,
    val abbreviation: String,
    val logo: String?,
    val isHomeTeam: Boolean,
    val pointsRegular: Int,
    val pointsShowdown: Int,
    val pointsTotal: Int,
    val runsTotal: Int,
    val hitsTotal: Int,
    val innings: List<InningScore>,
    val showdownRounds: List<ShowdownRoundSummary>,
    val batters: List<BatterBoxItem>,
    val pitchers: List<PitcherBoxItem>
)

data class InningScore(
    val inning: Int,
    val runs: Int,
    val hits: Int,
    val pointsAwarded: Int
)

data class BatterBoxItem(
    val playerId: String,
    val name: String,
    val jerseyNumber: Int?,
    val order: Int,
    val positions: List<String>,
    val hittingRoles: List<String>,
    val atBats: Int,
    val runs: Int,
    val hits: Int,
    val rbi: Int,
    val ballFourSprints: Int,
    val strikeouts: Int,
    val walkOffs: Int,
    val stolenBases: Int,
    val battingAverage: Double?,
    val ops: Double?
)

data class PitcherBoxItem(
    val playerId: String,
    val name: String,
    val jerseyNumber: Int?,
    val designations: List<String>,
    val inningsPitched: String, // e.g. "4.2", "1.0"
    val hitsAllowed: Int,
    val runsAllowed: Int,
    val earnedRuns: Int,
    val walksAllowed: Int?,
    val strikeouts: Int,
    val era: Double?,
    val minutesPerInning: String? // e.g. "3:51"
)

data class ShowdownRoundSummary(
    val round: Int,
    val result: String?,
    val outcomeType: String?,
    val runsScored: Int,
    val hitsRecorded: Int,
    val isWalkoff: Boolean
)
