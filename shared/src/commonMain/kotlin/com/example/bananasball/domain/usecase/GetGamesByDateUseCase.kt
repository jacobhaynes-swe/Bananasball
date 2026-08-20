package com.example.bananasball.domain.usecase

import com.example.bananasball.domain.model.Game
import com.example.bananasball.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class GetGamesByDateUseCase(private val repository: GameRepository) {
    operator fun invoke(date: LocalDate): Flow<List<Game>> {
        return repository.getGamesForDate(date)
    }
}
