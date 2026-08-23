package com.example.bananasball.ui.schedule

import com.example.bananasball.domain.model.*
import com.example.bananasball.domain.repository.GameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeGameRepository : GameRepository {
        val gamesFlow = MutableStateFlow<List<Game>>(emptyList())
        var mockDetailResult: Result<GameDetail> = Result.failure(Exception("Not loaded"))
        var refreshCount = 0

        override fun getGamesForDate(date: LocalDate): Flow<List<Game>> = gamesFlow.asStateFlow()

        override suspend fun refreshSchedule(): Result<Unit> {
            refreshCount++
            return Result.success(Unit)
        }

        override suspend fun getGameDetail(gameId: String): Result<GameDetail> = mockDetailResult
    }

    private lateinit var repository: FakeGameRepository
    private lateinit var viewModel: ScheduleViewModel

    private val sampleTeamAway = Team("sb", "Savannah Bananas", "Bananas", "#FFE000", "#002D62", null)
    private val sampleTeamHome = Team("pa", "Party Animals", "Animals", "#E91E63", "#0B1120", null)
    private val sampleGame = Game(
        id = "game_1",
        homeTeam = sampleTeamHome,
        awayTeam = sampleTeamAway,
        startTime = LocalDateTime.parse("2026-08-22T19:00:00"),
        youtubeUrl = "https://youtube.com/watch?v=mock",
        boxScore = BoxScore(awayScore = 1, homeScore = 0, status = "LIVE", currentInning = 2, inningHalf = "TOP"),
        location = "Grayson Stadium",
        statsGameId = "stats_101"
    )

    private fun createSampleGameDetail(
        awayPts: Int,
        homePts: Int,
        awayRuns: Int,
        homeRuns: Int,
        numInnings: Int = 2
    ): GameDetail {
        return GameDetail(
            gameId = "stats_101",
            status = "LIVE",
            venue = VenueDetail("Grayson Stadium", "Savannah", "GA", "EDT"),
            numberOfInnings = numInnings,
            equalizerPointAwarded = false,
            equalizerPointInning = null,
            awayTeam = TeamGameDetail(
                teamId = "sb",
                name = "Savannah Bananas",
                abbreviation = "BAN",
                logo = null,
                isHomeTeam = false,
                pointsRegular = awayPts,
                pointsShowdown = 0,
                pointsTotal = awayPts,
                runsTotal = awayRuns,
                hitsTotal = 5,
                innings = listOf(InningScore(1, awayRuns, 3, awayPts)),
                showdownRounds = emptyList(),
                batters = emptyList(),
                pitchers = emptyList()
            ),
            homeTeam = TeamGameDetail(
                teamId = "pa",
                name = "Party Animals",
                abbreviation = "ANI",
                logo = null,
                isHomeTeam = true,
                pointsRegular = homePts,
                pointsShowdown = 0,
                pointsTotal = homePts,
                runsTotal = homeRuns,
                hitsTotal = 4,
                innings = listOf(InningScore(1, homeRuns, 2, homePts)),
                showdownRounds = emptyList(),
                batters = emptyList(),
                pitchers = emptyList()
            )
        )
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeGameRepository()
        repository.gamesFlow.value = listOf(sampleGame)
        viewModel = ScheduleViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `opening game detail fetches box score and updates state`() = runTest(testDispatcher) {
        val initialDetail = createSampleGameDetail(awayPts = 1, homePts = 0, awayRuns = 2, homeRuns = 1)
        repository.mockDetailResult = Result.success(initialDetail)

        viewModel.handleIntent(ScheduleIntent.OnGameClicked(sampleGame))
        advanceTimeBy(100)

        val state = viewModel.state.value
        assertEquals("game_1", state.selectedGame?.id)
        assertEquals("stats_101", state.selectedGameDetail?.gameId)
        assertEquals(1, state.selectedGameDetail?.awayTeam?.pointsTotal)
        assertEquals(0, state.selectedGameDetail?.homeTeam?.pointsTotal)
        assertFalse(state.isLoadingDetail)
    }

    @Test
    fun `live refresh updates open game detail in place`() = runTest(testDispatcher) {
        val initialDetail = createSampleGameDetail(awayPts = 1, homePts = 0, awayRuns = 2, homeRuns = 1)
        repository.mockDetailResult = Result.success(initialDetail)

        viewModel.handleIntent(ScheduleIntent.OnGameClicked(sampleGame))
        advanceTimeBy(100)

        assertEquals(1, viewModel.state.value.selectedGameDetail?.awayTeam?.pointsTotal)

        // Mock live update where Inning 2 concludes with Party Animals scoring runs & points
        val updatedDetail = createSampleGameDetail(awayPts = 2, homePts = 1, awayRuns = 4, homeRuns = 3, numInnings = 3)
        repository.mockDetailResult = Result.success(updatedDetail)

        // Trigger refresh while sheet is open
        viewModel.handleIntent(ScheduleIntent.OnRefresh)
        advanceTimeBy(100)

        val updatedState = viewModel.state.value
        assertNotNull(updatedState.selectedGameDetail)
        assertEquals(2, updatedState.selectedGameDetail?.awayTeam?.pointsTotal)
        assertEquals(1, updatedState.selectedGameDetail?.homeTeam?.pointsTotal)
        assertEquals(4, updatedState.selectedGameDetail?.awayTeam?.runsTotal)
        assertEquals(3, updatedState.selectedGameDetail?.homeTeam?.runsTotal)
    }

    @Test
    fun `dismissing game detail clears selected state`() = runTest(testDispatcher) {
        viewModel.handleIntent(ScheduleIntent.OnGameClicked(sampleGame))
        advanceTimeBy(100)
        assertNotNull(viewModel.state.value.selectedGame)

        viewModel.handleIntent(ScheduleIntent.OnDismissGameDetail)
        advanceTimeBy(100)
        assertNull(viewModel.state.value.selectedGame)
        assertNull(viewModel.state.value.selectedGameDetail)
    }
}
