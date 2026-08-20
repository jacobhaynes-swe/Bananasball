package com.example.bananasball.domain.repository

import com.example.bananasball.domain.model.Game
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface GameRepository {
    fun getGamesForDate(date: LocalDate): Flow<List<Game>>
    suspend fun refreshSchedule(): Result<Unit>
}
