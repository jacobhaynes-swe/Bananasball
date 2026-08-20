package com.example.bananasball.ui.schedule

import com.example.bananasball.domain.model.Game
import kotlinx.datetime.LocalDate

data class ScheduleState(
    val selectedDate: LocalDate,
    val games: List<Game> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class ScheduleIntent {
    data class OnDateSelected(val date: LocalDate) : ScheduleIntent()
    object OnRefresh : ScheduleIntent()
    data class OnWatchLiveClicked(val url: String) : ScheduleIntent()
}
