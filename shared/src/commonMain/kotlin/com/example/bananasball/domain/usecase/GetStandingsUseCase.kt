package com.example.bananasball.domain.usecase

import com.example.bananasball.domain.model.LeagueStandings
import com.example.bananasball.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow

class GetStandingsUseCase(private val repository: StatsRepository) {
    operator fun invoke(): Flow<LeagueStandings?> {
        return repository.getStandings()
    }

    suspend fun refresh() {
        repository.refreshStats()
    }
}
