package com.example.bananasball.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val homeTeamId: String,
    val homeTeamName: String,
    val homeTeamShort: String,
    val awayTeamId: String,
    val awayTeamName: String,
    val awayTeamShort: String,
    val startTime: String, // ISO string
    val youtubeUrl: String?,
    val awayScore: Int,
    val homeScore: Int,
    val status: String,
    val location: String,
    val date: String // YYYY-MM-DD
)
