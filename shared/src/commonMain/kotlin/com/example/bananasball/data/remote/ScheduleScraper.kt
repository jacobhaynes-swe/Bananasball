package com.example.bananasball.data.remote

import com.example.bananasball.domain.model.GameDetail

interface ScheduleScraper {
    suspend fun fetchBaseSchedule(): List<ScrapedGame>
    suspend fun enrichLiveStreams(games: List<ScrapedGame>): List<ScrapedGame>
    suspend fun fetchSchedule(): List<ScrapedGame> = enrichLiveStreams(fetchBaseSchedule())
    suspend fun fetchGameBoxScore(gameId: String): Result<GameDetail>
}
