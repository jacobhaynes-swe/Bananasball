package com.example.bananasball.ui.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bananasball.domain.usecase.GetTeamsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TeamsViewModel(
    private val getTeamsUseCase: GetTeamsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TeamsState(isLoading = true))
    val state: StateFlow<TeamsState> = _state.asStateFlow()

    init {
        observeTeams()
        refresh()
    }

    fun handleIntent(intent: TeamsIntent) {
        when (intent) {
            TeamsIntent.OnRefresh -> refresh()
        }
    }

    private fun observeTeams() {
        viewModelScope.launch {
            getTeamsUseCase()
                .onEach { teams ->
                    _state.update { it.copy(teams = teams, isLoading = false) }
                }
                .launchIn(this)
        }
    }

    private fun refresh() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                getTeamsUseCase.refresh()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
