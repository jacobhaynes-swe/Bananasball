package com.example.bananasball.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "standings")
data class StandingEntity(
    @PrimaryKey val teamId: String,
    val rank: Int = 1,
    val wins: Int,
    val losses: Int,
    val winPercentage: Double,
    val gamesBehind: Double,
    val streak: String? = null,
    val runDifferential: Int = 0
)
