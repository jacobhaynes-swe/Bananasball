package com.example.bananasball.data.remote

data class EnrichedStreamMetadata(
    val videoId: String,
    val directUrl: String,
    val thumbnailUrl: String? = null,
    val waitingCount: Int? = null,
    val viewerCount: Int? = null,
    val isLiveBroadcast: Boolean = false,
    val scheduledStartTime: String? = null,
    val title: String? = null
)
