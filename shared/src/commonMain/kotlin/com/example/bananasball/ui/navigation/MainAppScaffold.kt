package com.example.bananasball.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bananasball.ui.schedule.ScheduleScreen
import com.example.bananasball.ui.schedule.ScheduleViewModel
import com.example.bananasball.ui.stats.StatsScreen
import com.example.bananasball.ui.stats.StatsViewModel
import com.example.bananasball.ui.teams.TeamsScreen
import com.example.bananasball.ui.teams.TeamsViewModel

enum class NavigationDestination(
    val title: String,
    val iconEmoji: String
) {
    SCHEDULE("Schedule", "📅"),
    STATS("Stats", "📊"),
    TEAMS("Teams", "⚾")
}

@Composable
fun MainAppScaffold(
    scheduleViewModel: ScheduleViewModel,
    statsViewModel: StatsViewModel,
    teamsViewModel: TeamsViewModel,
    onOpenUrl: (String) -> Unit
) {
    var selectedDestination by remember { mutableStateOf(NavigationDestination.SCHEDULE) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.secondary,
                tonalElevation = 8.dp
            ) {
                NavigationDestination.values().forEach { destination ->
                    val isSelected = selectedDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedDestination = destination },
                        icon = {
                            Text(
                                text = destination.iconEmoji,
                                fontSize = if (isSelected) 22.sp else 18.sp
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.White.copy(alpha = 0.7f),
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (selectedDestination) {
                NavigationDestination.SCHEDULE -> {
                    ScheduleScreen(
                        viewModel = scheduleViewModel,
                        onWatchLive = onOpenUrl
                    )
                }
                NavigationDestination.STATS -> {
                    StatsScreen(
                        viewModel = statsViewModel
                    )
                }
                NavigationDestination.TEAMS -> {
                    TeamsScreen(
                        viewModel = teamsViewModel,
                        onOpenUrl = onOpenUrl
                    )
                }
            }
        }
    }
}
