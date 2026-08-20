package com.example.bananasball.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bananasball.domain.repository.GameRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
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

    private var pollingJob: Job? = null
    private var observeJob: Job? = null

    init {
        observeGames()
        refresh()
        startAdaptivePolling()
    }

    fun handleIntent(intent: ScheduleIntent) {
        when (intent) {
            is ScheduleIntent.OnDateSelected -> {
                _state.update { it.copy(selectedDate = intent.date) }
                observeGames()
                startAdaptivePolling()
            }
            ScheduleIntent.OnRefresh -> refresh()
            is ScheduleIntent.OnWatchLiveClicked -> {
                // Side effect handled by UI or Navigator
            }
        }
    }

    private fun observeGames() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
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
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun startAdaptivePolling() {
        pollingJob?.cancel()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        // Only run 45s background polling when viewing Today's games
        if (_state.value.selectedDate == today) {
            pollingJob = viewModelScope.launch {
                while (isActive) {
                    delay(45_000)
                    if (isActive) {
                        repository.refreshSchedule()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        observeJob?.cancel()
    }
}
