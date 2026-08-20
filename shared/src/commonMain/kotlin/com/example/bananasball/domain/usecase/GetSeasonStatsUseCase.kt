package com.example.bananasball.domain.usecase

import com.example.bananasball.domain.model.SeasonStats
import com.example.bananasball.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow

class GetSeasonStatsUseCase(private val repository: StatsRepository) {
    operator fun invoke(): Flow<SeasonStats?> {
        return repository.getSeasonStats()
    }

    suspend fun refresh() {
        repository.refreshStats()
    }
}
