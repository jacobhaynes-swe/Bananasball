package com.example.bananasball.domain.model

import kotlinx.datetime.LocalDateTime

data class StreamingMetadata(
    val thumbnailUrl: String? = null,
    val waitingCount: Int? = null,
    val actualStartTime: LocalDateTime? = null,
    val streamTitle: String? = null
)
