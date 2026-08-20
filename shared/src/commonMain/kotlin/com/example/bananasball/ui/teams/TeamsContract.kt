package com.example.bananasball.ui.teams

import com.example.bananasball.domain.model.Team

data class TeamsState(
    val teams: List<Team> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class TeamsIntent {
    object OnRefresh : TeamsIntent()
}
