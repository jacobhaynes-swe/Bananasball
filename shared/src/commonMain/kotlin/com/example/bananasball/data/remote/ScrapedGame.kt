package com.example.bananasball.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ScrapedGame(
    val date: String,
    val location: String,
    val time: String,
    val teamCodes: List<String>,
    val youtubeUrl: String? = null,
    val awayScore: Int? = null,
    val homeScore: Int? = null,
    val status: String? = null,
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
