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
import com.example.bananasball.data.remote.createHttpClient
import com.example.bananasball.data.repository.RoomGameRepository
import com.example.bananasball.ui.schedule.ScheduleScreen
import com.example.bananasball.ui.schedule.ScheduleViewModel
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
        val scraper = KtorScheduleScraper(httpClient)
        val repository = RoomGameRepository(database.getGameDao(), scraper)
        
        // Initial sync
        MainScope().launch {
            repository.sync()
        }

        setContent {
            BananasTheme {
                val viewModel = remember { ScheduleViewModel(repository) }
                ScheduleScreen(
                    viewModel = viewModel,
                    onWatchLive = { url ->
                        println("MainActivity: Opening YouTube URL: $url")
                        val uri = Uri.parse(url)
                        
                        // 1. Try to open directly in the YouTube App using the package
                        val appIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage("com.google.android.youtube")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        
                        try {
                            startActivity(appIntent)
                        } catch (e: Exception) {
                            println("MainActivity: YouTube app not found, falling back to browser.")
                            // 2. Fallback to the system's default handler (usually a browser)
                            val webIntent = Intent(Intent.ACTION_VIEW, uri)
                            startActivity(webIntent)
                        }
                    }
                )
            }
        }
    }
}
