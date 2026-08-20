package com.example.bananasball.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.example.bananasball.data.local.appContext
import com.example.bananasball.data.local.getDatabaseBuilder
import com.example.bananasball.data.remote.KtorScheduleScraper
import com.example.bananasball.data.remote.KtorStatsScraper
import com.example.bananasball.data.remote.KtorTeamScraper
import com.example.bananasball.data.remote.createHttpClient
import com.example.bananasball.data.repository.OfflineStatsRepository
import com.example.bananasball.data.repository.OfflineTeamRepository
import com.example.bananasball.data.repository.RoomGameRepository
import com.example.bananasball.domain.usecase.GetSeasonStatsUseCase
import com.example.bananasball.domain.usecase.GetStandingsUseCase
import com.example.bananasball.domain.usecase.GetTeamsUseCase
import com.example.bananasball.ui.navigation.MainAppScaffold
import com.example.bananasball.ui.schedule.ScheduleViewModel
import com.example.bananasball.ui.stats.StatsViewModel
import com.example.bananasball.ui.teams.TeamsViewModel
import com.example.bananasball.ui.theme.BananasTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize App Context for Room
        appContext = applicationContext
        
        val database = getDatabaseBuilder().build()
        val httpClient = createHttpClient()

        // Scrapers
        val scheduleScraper = KtorScheduleScraper(httpClient)
        val statsScraper = KtorStatsScraper(httpClient)
        val teamScraper = KtorTeamScraper(httpClient)

        // Repositories
        val gameRepository = RoomGameRepository(database.getGameDao(), scheduleScraper)
        val statsRepository = OfflineStatsRepository(database.getStatsDao(), database.getTeamDao(), statsScraper)
        val teamRepository = OfflineTeamRepository(database.getTeamDao(), teamScraper)

        // Use Cases
        val getTeamsUseCase = GetTeamsUseCase(teamRepository)
        val getStandingsUseCase = GetStandingsUseCase(statsRepository)
        val getSeasonStatsUseCase = GetSeasonStatsUseCase(statsRepository)
        
        // Initial sync
        MainScope().launch {
            try {
                gameRepository.sync()
                teamRepository.refreshTeams()
                statsRepository.refreshStats()
            } catch (e: Exception) {
                println("MainActivity: Initial sync error: ${e.message}")
            }
        }

        setContent {
            BananasTheme {
                val scheduleViewModel = remember { ScheduleViewModel(gameRepository) }
                val statsViewModel = remember { StatsViewModel(getStandingsUseCase, getSeasonStatsUseCase) }
                val teamsViewModel = remember { TeamsViewModel(getTeamsUseCase) }

                MainAppScaffold(
                    scheduleViewModel = scheduleViewModel,
                    statsViewModel = statsViewModel,
                    teamsViewModel = teamsViewModel,
                    onOpenUrl = { url ->
                        openExternalUrl(url)
                    }
                )
            }
        }
    }

    private fun openExternalUrl(url: String) {
        println("MainActivity: Opening URL: $url")
        val uri = Uri.parse(url)
        
        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            // 1. Try to open directly in the YouTube App
            val appIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.youtube")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(appIntent)
                return
            } catch (e: Exception) {
                println("MainActivity: YouTube app not found, falling back to browser.")
            }
        }

        // 2. Fallback to system default handler (browser or PDF viewer)
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(webIntent)
        } catch (e: Exception) {
            println("MainActivity: Error launching intent for $url: ${e.message}")
        }
    }
}
