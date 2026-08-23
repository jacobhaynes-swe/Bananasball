package com.example.bananasball.domain.model

data class BoxScore(
    val awayScore: Int? = null, // Banana Ball Points
    val homeScore: Int? = null,
    val status: String, // e.g., "Final", "LIVE", "Scheduled"
    val awayRuns: Int? = null,
    val homeRuns: Int? = null,
    val awayHits: Int? = null,
    val homeHits: Int? = null,
    val currentInning: Int? = null,
    val inningHalf: String? = null, // "TOP", "BOT", "MID", "END"
    val outs: Int? = null, // 0, 1, 2
    val inningDisplay: String? = null // e.g. "▲ 3", "▼ 2"
) {
    val hasOfficialStats: Boolean
        get() = (awayScore != null && homeScore != null) || awayRuns != null || homeRuns != null || awayHits != null || homeHits != null || currentInning != null || inningDisplay != null
}
