package com.example.bananasball.domain.model

sealed class StatLeader {
    data class Batting(
        val rank: Int = 1,
        val player: String,
        val team: Team,
        val avg: Double,
        val hr: Int,
        val rbi: Int,
        val ops: Double,
        val hits: Int = 0,
        val games: Int = 0,
        val b4s: Int = 0,
        val stolenBases: Int = 0
    ) : StatLeader()

    data class Pitching(
        val rank: Int = 1,
        val player: String,
        val team: Team,
        val era: Double,
        val wins: Int,
        val so: Int,
        val whip: Double,
        val inningsPitched: Double = 0.0,
        val saves: Int = 0
    ) : StatLeader()
}

data class SeasonStats(
    val battingLeaders: List<StatLeader.Batting>,
    val pitchingLeaders: List<StatLeader.Pitching>
)
