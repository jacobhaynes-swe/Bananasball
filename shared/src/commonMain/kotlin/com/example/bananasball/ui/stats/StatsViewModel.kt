package com.example.bananasball.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bananasball.domain.usecase.GetSeasonStatsUseCase
import com.example.bananasball.domain.usecase.GetStandingsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StatsViewModel(
    private val getStandingsUseCase: GetStandingsUseCase,
    private val getSeasonStatsUseCase: GetSeasonStatsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(StatsState(isLoading = true))
    val state: StateFlow<StatsState> = _state.asStateFlow()

    init {
        observeData()
        refresh()
    }

    fun handleIntent(intent: StatsIntent) {
        when (intent) {
            is StatsIntent.OnTabSelected -> {
                _state.update { it.copy(selectedTab = intent.tab) }
            }
            StatsIntent.OnRefresh -> refresh()
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            getStandingsUseCase()
                .onEach { standings ->
                    _state.update { it.copy(standings = standings, isLoading = false) }
                }
                .launchIn(this)

            getSeasonStatsUseCase()
                .onEach { stats ->
                    _state.update { it.copy(seasonStats = stats) }
                }
                .launchIn(this)
        }
    }

    private fun refresh() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                getStandingsUseCase.refresh()
                getSeasonStatsUseCase.refresh()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
