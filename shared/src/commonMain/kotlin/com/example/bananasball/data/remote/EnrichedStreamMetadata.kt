package com.example.bananasball.data.remote

data class EnrichedStreamMetadata(
    val videoId: String,
    val directUrl: String,
    val thumbnailUrl: String? = null,
    val waitingCount: Int? = null,
    val scheduledStartTime: String? = null,
    val title: String? = null
)
