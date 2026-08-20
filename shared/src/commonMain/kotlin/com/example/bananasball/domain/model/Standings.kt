package com.example.bananasball.domain.model

data class TeamStandings(
    val rank: Int = 1,
    val team: Team,
    val wins: Int,
    val losses: Int,
    val winPercentage: Double,
    val gamesBehind: Double,
    val streak: String? = null,
    val runDifferential: Int = 0
)

data class LeagueStandings(
    val season: String = "2026",
    val lastUpdated: String? = null,
    val rankings: List<TeamStandings>
)
