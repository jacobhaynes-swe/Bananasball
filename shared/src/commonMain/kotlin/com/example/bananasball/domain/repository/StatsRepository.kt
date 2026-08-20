package com.example.bananasball.domain.repository

import com.example.bananasball.domain.model.LeagueStandings
import com.example.bananasball.domain.model.SeasonStats
import kotlinx.coroutines.flow.Flow

interface StatsRepository {
    fun getStandings(): Flow<LeagueStandings?>
    fun getSeasonStats(): Flow<SeasonStats?>
    suspend fun refreshStats()
}
