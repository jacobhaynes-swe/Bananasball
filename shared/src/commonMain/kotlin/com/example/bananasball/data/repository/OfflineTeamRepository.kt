package com.example.bananasball.data.repository

import com.example.bananasball.data.local.TeamDao
import com.example.bananasball.data.local.toDomain
import com.example.bananasball.data.local.toEntity
import com.example.bananasball.data.remote.KtorTeamScraper
import com.example.bananasball.domain.model.Team
import com.example.bananasball.domain.repository.TeamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineTeamRepository(
    private val teamDao: TeamDao,
    private val teamScraper: KtorTeamScraper
) : TeamRepository {
    override fun getTeams(): Flow<List<Team>> {
        return teamDao.getAllTeams().map { entities -> 
            if (entities.isEmpty()) {
                StaticTeamProvider.getAllTeams()
            } else {
                entities.map { it.toDomain() }
            }
        }
    }

    override suspend fun refreshTeams() {
        val scrapedTeams = teamScraper.fetchTeams()
        teamDao.insertTeams(scrapedTeams.map { it.toEntity() })
    }
}
