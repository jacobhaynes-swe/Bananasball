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
    val awayScore: Int? = null,
    val homeScore: Int? = null,
    val status: String,
    val location: String,
    val date: String, // YYYY-MM-DD
    val thumbnailUrl: String? = null,
    val waitingCount: Int? = null,
    val viewerCount: Int? = null,
    val isLiveBroadcast: Boolean = false,
    val actualStartTime: String? = null,
    val streamTitle: String? = null,
    val awayRuns: Int? = null,
    val homeRuns: Int? = null,
    val awayHits: Int? = null,
    val homeHits: Int? = null,
    val currentInning: Int? = null,
    val inningHalf: String? = null,
    val outs: Int? = null,
    val inningDisplay: String? = null,
    val statsGameId: String? = null
)
