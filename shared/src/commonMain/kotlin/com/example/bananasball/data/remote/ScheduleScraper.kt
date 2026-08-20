package com.example.bananasball.data.remote

interface ScheduleScraper {
    suspend fun fetchSchedule(): List<ScrapedGame>
}
