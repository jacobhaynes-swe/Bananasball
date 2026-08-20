package com.example.bananasball.ui.stats

import com.example.bananasball.domain.model.LeagueStandings
import com.example.bananasball.domain.model.SeasonStats

enum class StatsTab(val title: String) {
    STANDINGS("Standings"),
    BATTING("Batting Leaders"),
    PITCHING("Pitching Leaders")
}

data class StatsState(
    val selectedTab: StatsTab = StatsTab.STANDINGS,
    val standings: LeagueStandings? = null,
    val seasonStats: SeasonStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class StatsIntent {
    data class OnTabSelected(val tab: StatsTab) : StatsIntent()
    object OnRefresh : StatsIntent()
}
