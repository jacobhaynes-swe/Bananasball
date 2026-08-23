package com.example.bananasball.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE date = :date ORDER BY startTime ASC")
    fun getGamesByDate(date: String): Flow<List<GameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<GameEntity>)

    @Query("DELETE FROM games")
    suspend fun clearAllGames()

    @androidx.room.Transaction
    suspend fun replaceAllGames(games: List<GameEntity>) {
        clearAllGames()
        insertGames(games)
    }
}
