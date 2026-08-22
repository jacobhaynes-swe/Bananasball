package com.example.bananasball.data.repository

import com.example.bananasball.data.local.GameDao
import com.example.bananasball.data.mapper.toDomain
import com.example.bananasball.data.mapper.toEntity
import com.example.bananasball.data.remote.ScheduleScraper
import com.example.bananasball.domain.model.Game
import com.example.bananasball.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class RoomGameRepository(
    private val gameDao: GameDao,
    private val scraper: ScheduleScraper
) : GameRepository {

    override fun getGamesForDate(date: LocalDate): Flow<List<Game>> {
        return gameDao.getGamesByDate(date.toString()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshSchedule(): Result<Unit> = runCatching {
        sync()
    }
    
    suspend fun sync() {
        println("Repository: Starting sync...")
        val scrapedGames = scraper.fetchSchedule()
        println("Repository: Scraped ${scrapedGames.size} games")
        gameDao.clearAllGames()
        gameDao.insertGames(scrapedGames.map { it.toEntity() })
        println("Repository: Sync complete")
    }
}
