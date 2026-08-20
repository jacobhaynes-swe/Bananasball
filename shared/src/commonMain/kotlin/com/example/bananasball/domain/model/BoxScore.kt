package com.example.bananasball.domain.model

data class BoxScore(
    val awayScore: Int,
    val homeScore: Int,
    val status: String // e.g., "Final", "Live", "Scheduled"
)
