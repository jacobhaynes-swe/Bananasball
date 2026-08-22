package com.example.bananasball.data.remote

import com.example.bananasball.domain.model.GameDetail

interface ScheduleScraper {
    suspend fun fetchSchedule(): List<ScrapedGame>
    suspend fun fetchGameBoxScore(gameId: String): Result<GameDetail>
}
