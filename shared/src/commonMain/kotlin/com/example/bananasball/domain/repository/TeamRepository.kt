package com.example.bananasball.domain.repository

import com.example.bananasball.domain.model.Team
import kotlinx.coroutines.flow.Flow

interface TeamRepository {
    fun getTeams(): Flow<List<Team>>
    suspend fun refreshTeams()
}
