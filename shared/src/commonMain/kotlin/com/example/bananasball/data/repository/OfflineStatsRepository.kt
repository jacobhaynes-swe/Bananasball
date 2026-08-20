package com.example.bananasball.data.repository

import com.example.bananasball.data.local.StandingEntity
import com.example.bananasball.data.local.StatsDao
import com.example.bananasball.data.local.TeamDao
import com.example.bananasball.data.local.toDomain
import com.example.bananasball.data.remote.KtorStatsScraper
import com.example.bananasball.domain.model.LeagueStandings
import com.example.bananasball.domain.model.SeasonStats
import com.example.bananasball.domain.model.TeamStandings
import com.example.bananasball.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class OfflineStatsRepository(
    private val statsDao: StatsDao,
    private val teamDao: TeamDao,
    private val statsScraper: KtorStatsScraper
) : StatsRepository {

    private val _seasonStats = MutableStateFlow<SeasonStats?>(null)

    override fun getStandings(): Flow<LeagueStandings?> {
        return combine(
            statsDao.getStandings(),
            teamDao.getAllTeams()
        ) { standings, teams ->
            val teamMap = teams.associate { it.id to it.toDomain() }
            val rankings = standings.mapNotNull { entity ->
                val team = teamMap[entity.teamId] ?: StaticTeamProvider.getTeam(entity.teamId) ?: return@mapNotNull null
                TeamStandings(
                    rank = entity.rank,
                    team = team,
                    wins = entity.wins,
                    losses = entity.losses,
                    winPercentage = entity.winPercentage,
                    gamesBehind = entity.gamesBehind,
                    streak = entity.streak,
                    runDifferential = entity.runDifferential
                )
            }

            if (rankings.isEmpty()) {
                // If DB is not populated yet, return default baseline
                statsScraper.fetchStandings()
            } else {
                LeagueStandings(rankings = rankings)
            }
        }
    }

    override fun getSeasonStats(): Flow<SeasonStats?> {
        return _seasonStats.asStateFlow()
    }

    override suspend fun refreshStats() {
        val scrapedStandings = statsScraper.fetchStandings()
        val entities = scrapedStandings.rankings.map { 
            StandingEntity(
                teamId = it.team.id,
                rank = it.rank,
                wins = it.wins,
                losses = it.losses,
                winPercentage = it.winPercentage,
                gamesBehind = it.gamesBehind,
                streak = it.streak,
                runDifferential = it.runDifferential
            )
        }
        statsDao.insertStandings(entities)

        // Refresh player season stats
        val seasonStats = statsScraper.fetchSeasonStats()
        _seasonStats.value = seasonStats
    }
}
