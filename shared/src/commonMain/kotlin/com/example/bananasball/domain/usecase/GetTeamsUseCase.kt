package com.example.bananasball.domain.usecase

import com.example.bananasball.domain.model.Team
import com.example.bananasball.domain.repository.TeamRepository
import kotlinx.coroutines.flow.Flow

class GetTeamsUseCase(private val repository: TeamRepository) {
    operator fun invoke(): Flow<List<Team>> {
        return repository.getTeams()
    }

    suspend fun refresh() {
        repository.refreshTeams()
    }
}
