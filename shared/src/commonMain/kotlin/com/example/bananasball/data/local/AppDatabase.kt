package com.example.bananasball.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GameEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getGameDao(): GameDao
}
