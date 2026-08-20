package com.example.bananasball.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bananasball.domain.repository.GameRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalTime::class)
class ScheduleViewModel(
    private val repository: GameRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        ScheduleState(
            selectedDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        )
    )
    val state: StateFlow<ScheduleState> = _state.asStateFlow()

    init {
        observeGames()
        refresh()
    }

    fun handleIntent(intent: ScheduleIntent) {
        when (intent) {
            is ScheduleIntent.OnDateSelected -> {
                _state.update { it.copy(selectedDate = intent.date) }
                observeGames()
            }
            ScheduleIntent.OnRefresh -> refresh()
            is ScheduleIntent.OnWatchLiveClicked -> {
                // Side effect handled by UI or Navigator
            }
        }
    }

    private fun observeGames() {
        viewModelScope.launch {
            repository.getGamesForDate(_state.value.selectedDate)
                .onEach { games ->
                    _state.update { it.copy(games = games, isLoading = false) }
                }
                .launchIn(this)
        }
    }

    private fun refresh() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.refreshSchedule()
            // In a real app, Room update triggers the flow
        }
    }
}
