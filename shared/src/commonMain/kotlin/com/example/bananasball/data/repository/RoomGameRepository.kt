package com.example.bananasball.data.repository

import com.example.bananasball.data.local.GameDao
import com.example.bananasball.data.mapper.toDomain
import com.example.bananasball.data.mapper.toEntity
import com.example.bananasball.data.remote.ScheduleScraper
import com.example.bananasball.domain.model.Game
import com.example.bananasball.domain.model.GameDetail
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

    override suspend fun getGameDetail(gameId: String): Result<GameDetail> {
        return scraper.fetchGameBoxScore(gameId)
    }
    
    suspend fun sync() {
        println("Repository: Starting immediate base schedule sync...")
        val baseGames = scraper.fetchBaseSchedule()
        if (baseGames.isNotEmpty()) {
            gameDao.replaceAllGames(baseGames.map { it.toEntity() })
            println("Repository: Base schedule sync complete (${baseGames.size} games saved to Room)")
        }

        println("Repository: Enriching live streams & hype counters in background...")
        val enrichedGames = scraper.enrichLiveStreams(baseGames.ifEmpty { scraper.fetchBaseSchedule() })
        if (enrichedGames.isNotEmpty()) {
            gameDao.replaceAllGames(enrichedGames.map { it.toEntity() })
            println("Repository: Stream enrichment complete (${enrichedGames.size} games updated in Room)")
        }
    }
}
