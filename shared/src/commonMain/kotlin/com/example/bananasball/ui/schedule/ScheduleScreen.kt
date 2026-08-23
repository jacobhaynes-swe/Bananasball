package com.example.bananasball.ui.schedule

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.bananasball.domain.model.Game
import com.example.bananasball.ui.components.BananaPullToRefreshIndicator
import com.example.bananasball.ui.components.SpinningBaseballLoader
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onWatchLive: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "BANANASBALL",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFE000),
                        letterSpacing = 1.sp,
                        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                    )
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Text("📅", fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color(0xFFFFE000)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DateRibbon(
                selectedDate = state.selectedDate,
                onDateSelected = { viewModel.handleIntent(ScheduleIntent.OnDateSelected(it)) }
            )

            val pullToRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                state = pullToRefreshState,
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.handleIntent(ScheduleIntent.OnRefresh) },
                indicator = {
                    BananaPullToRefreshIndicator(
                        state = pullToRefreshState,
                        isRefreshing = state.isLoading
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.games.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isLoading) {
                            SpinningBaseballLoader(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                text = "Loading Schedule..."
                            )
                        } else {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🍌", fontSize = 48.sp)
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "No Games Scheduled",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Check adjacent dates on the ribbon or use the calendar picker.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.games, key = { it.id }) { game ->
                            GameCard(
                                game = game,
                                onWatchLive = onWatchLive,
                                onGameClick = { viewModel.handleIntent(ScheduleIntent.OnGameClicked(game)) }
                            )
                        }
                    }
                }
            }
        }
    }

    state.selectedGame?.let { selectedGame ->
        GameDetailModalSheet(
            game = selectedGame,
            detail = state.selectedGameDetail,
            isLoading = state.isLoadingDetail,
            onDismiss = { viewModel.handleIntent(ScheduleIntent.OnDismissGameDetail) }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedInstant = Instant.fromEpochMilliseconds(millis)
                            val selectedLocalDate = selectedInstant.toLocalDateTime(TimeZone.UTC).date
                            viewModel.handleIntent(ScheduleIntent.OnDateSelected(selectedLocalDate))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun DateRibbon(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val dates = remember(today) {
        (-14..28).map { offset ->
            today.plus(offset, DateTimeUnit.DAY)
        }
    }
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val itemWidthPx = with(density) { 56.dp.toPx() }
        val centerOffsetPx = ((screenWidthPx - itemWidthPx) / 2).toInt()

        val selectedIndex = remember(dates, selectedDate) {
            dates.indexOf(selectedDate).takeIf { it >= 0 } ?: dates.indexOf(today).coerceAtLeast(0)
        }

        LaunchedEffect(selectedDate) {
            listState.animateScrollToItem(
                index = selectedIndex,
                scrollOffset = -centerOffsetPx
            )
        }

        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(dates, key = { it.toString() }) { date ->
                val isSelected = date == selectedDate
                Surface(
                    onClick = { onDateSelected(date) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFFFFE000) else Color.White.copy(alpha = 0.15f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = date.dayOfWeek.name.take(3),
                            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = date.day.toString(),
                            color = if (isSelected) Color.Black else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameCard(
    game: Game,
    onWatchLive: (String) -> Unit,
    onGameClick: () -> Unit = {}
) {
    val isLiveGame = game.boxScore.status.equals("Live", ignoreCase = true) || 
                     (game.streamingMetadata?.isLiveBroadcast == true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onGameClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            // YouTube Thumbnail Banner if available
            game.streamingMetadata?.thumbnailUrl?.let { thumbUrl ->
                Box(Modifier.fillMaxWidth().height(150.dp)) {
                    AsyncImage(
                        model = thumbUrl,
                        contentDescription = "Game Broadcast Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    
                    // Live Viewers or Pre-game Waiting Badge
                    val isLiveBroadcast = game.streamingMetadata.isLiveBroadcast || isLiveGame
                    val viewerCount = game.streamingMetadata.viewerCount
                    val waitingCount = game.streamingMetadata.waitingCount

                    if (isLiveBroadcast) {
                        val countText = when {
                            viewerCount != null && viewerCount >= 1000 -> "${((viewerCount / 100.0).toInt() / 10.0)}K watching"
                            viewerCount != null -> "$viewerCount watching"
                            else -> "LIVE NOW"
                        }
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            color = Color(0xFFCC0000).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🔴 $countText",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else if (waitingCount != null) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🔥 $waitingCount waiting",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Column(Modifier.padding(16.dp)) {
                // Header: Location & Scorebug Status (Inning / Clock / Scheduled Badge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = game.location,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(Modifier.width(8.dp))

                    if (isLiveGame) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LivePulsingBadge()
                            game.boxScore.inningDisplay?.let { inningText ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = inningText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        val status = game.boxScore.status.uppercase()
                        val isFinal = status == "FINAL"
                        Surface(
                            color = if (isFinal) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = status,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (isFinal) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(14.dp))
                
                // Away Team Row
                TeamRow(
                    team = game.awayTeam,
                    score = game.boxScore.awayScore,
                    runs = game.boxScore.awayRuns,
                    hits = game.boxScore.awayHits,
                    isLive = isLiveGame,
                    showScore = game.boxScore.hasOfficialStats
                )
                Spacer(Modifier.height(10.dp))
                // Home Team Row
                TeamRow(
                    team = game.homeTeam,
                    score = game.boxScore.homeScore,
                    runs = game.boxScore.homeRuns,
                    hits = game.boxScore.homeHits,
                    isLive = isLiveGame,
                    showScore = game.boxScore.hasOfficialStats
                )

                if (isLiveGame && !game.boxScore.hasOfficialStats) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ℹ️", fontSize = 11.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Official stats not provided yet for this game",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Game & Stream Time Details
                val streamStartTime = game.streamingMetadata?.actualStartTime
                val gameStartTime = game.startTime
                val hasDistinctStreamTime = streamStartTime != null && 
                    (streamStartTime.hour != gameStartTime.hour || streamStartTime.minute != gameStartTime.minute)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚾", fontSize = 12.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Game Start (First Pitch):",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = formatLocalGameTime(gameStartTime),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (hasDistinctStreamTime && streamStartTime != null) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📡", fontSize = 12.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Stream Broadcast:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = if (isLiveGame) "🔴 LIVE NOW" else formatLocalGameTime(streamStartTime),
                                    fontSize = 12.sp,
                                    color = if (isLiveGame) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onGameClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFFE000)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE000).copy(alpha = 0.6f))
                    ) {
                        Text("📊 Box Score", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    if (game.youtubeUrl != null) {
                        Button(
                            onClick = { onWatchLive(game.youtubeUrl) },
                            modifier = Modifier.weight(1.3f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLiveGame) Color(0xFFCC0000) else MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isLiveGame) "▶ Watch Live" else "▶ Stream",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LivePulsingBadge() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        color = Color(0xFFDC2626),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "LIVE",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

private fun formatLocalGameTime(dateTime: LocalDateTime): String {
    val hour12 = when (val h = dateTime.hour % 12) {
        0 -> 12
        else -> h
    }
    val amPm = if (dateTime.hour < 12) "AM" else "PM"
    val minuteStr = dateTime.minute.toString().padStart(2, '0')
    return "$hour12:$minuteStr $amPm"
}



@Composable
fun TeamRow(
    team: com.example.bananasball.domain.model.Team,
    score: Int? = null,
    runs: Int? = null,
    hits: Int? = null,
    isLive: Boolean = false,
    showScore: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (team.logoUrl != null) {
                    AsyncImage(
                        model = team.logoUrl,
                        contentDescription = team.name,
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = team.shortName.take(1),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = team.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = team.shortName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (runs != null || hits != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "•  ${runs ?: 0} R, ${hits ?: 0} H",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
        
        if (score != null && showScore) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = score.toString(),
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isLive) {
                    Text(
                        text = "PTS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
