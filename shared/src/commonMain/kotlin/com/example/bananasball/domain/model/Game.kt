package com.example.bananasball.domain.model

import kotlinx.datetime.LocalDateTime

data class Game(
    val id: String,
    val homeTeam: Team,
    val awayTeam: Team,
    val startTime: LocalDateTime,
    val youtubeUrl: String?,
    val boxScore: BoxScore,
    val location: String
)
