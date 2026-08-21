package com.example.bananasball.domain.model

data class BoxScore(
    val awayScore: Int, // Banana Ball Points
    val homeScore: Int,
    val status: String, // e.g., "Final", "LIVE", "Scheduled"
    val awayRuns: Int? = null,
    val homeRuns: Int? = null,
    val awayHits: Int? = null,
    val homeHits: Int? = null,
    val currentInning: Int? = null,
    val inningHalf: String? = null, // "TOP", "BOT", "MID", "END"
    val outs: Int? = null, // 0, 1, 2
    val inningDisplay: String? = null // e.g. "▲ 3RD • 2 Outs", "▼ 2ND • 2 Outs"
)
