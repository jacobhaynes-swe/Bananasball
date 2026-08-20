package com.example.bananasball.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.bananasball.domain.model.Game
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onWatchLive: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                        viewModel.handleIntent(ScheduleIntent.OnDateSelected(picked))
                    }
                    showDatePicker = false
                }) {
                    Text("Select", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.secondary)) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "BANANASBALL",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("📅", fontSize = 16.sp)
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
                DateRibbon(
                    selectedDate = state.selectedDate,
                    onDateSelected = { viewModel.handleIntent(ScheduleIntent.OnDateSelected(it)) }
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.handleIntent(ScheduleIntent.OnRefresh) },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.games) { game ->
                    GameCard(
                        game = game,
                        onWatchLive = onWatchLive
                    )
                }
                
                if (state.games.isEmpty() && !state.isLoading) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🍌", fontSize = 40.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "No Games Scheduled",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Check adjacent dates on the ribbon or use the calendar picker.",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateRibbon(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val dates = remember(selectedDate) {
        val start = selectedDate.plus(-10, DateTimeUnit.DAY)
        (0..20).map { start.plus(it, DateTimeUnit.DAY) }
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 8)

    LaunchedEffect(selectedDate) {
        val index = dates.indexOf(selectedDate)
        if (index >= 0) {
            listState.animateScrollToItem((index - 2).coerceAtLeast(0))
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(dates) { date ->
            val isSelected = date == selectedDate
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f))
                    .clickable { onDateSelected(date) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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

@Composable
fun GameCard(
    game: Game,
    onWatchLive: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            // YouTube Thumbnail if available
            game.streamingMetadata?.thumbnailUrl?.let { thumbUrl ->
                Box(Modifier.fillMaxWidth().height(180.dp)) {
                    AsyncImage(
                        model = thumbUrl,
                        contentDescription = "Game Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    // Hype Count badge
                    game.streamingMetadata.waitingCount?.let { count ->
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🔥 $count waiting",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = game.location,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    val isLive = game.boxScore.status.equals("Live", ignoreCase = true)
                    Surface(
                        color = if (isLive) Color(0xFFE53935) else Color(0xFFECEFF1),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = game.boxScore.status.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (isLive) Color.White else Color(0xFF455A64),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(Modifier.height(14.dp))
                
                TeamRow(team = game.awayTeam, score = game.boxScore.awayScore)
                Spacer(Modifier.height(8.dp))
                TeamRow(team = game.homeTeam, score = game.boxScore.homeScore)

                // Start Time in Local Device Time
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🕒", fontSize = 13.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Start Time (Local):",
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = formatLocalGameTime(game.streamingMetadata?.actualStartTime ?: game.startTime),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                if (game.youtubeUrl != null) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onWatchLive(game.youtubeUrl) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (game.boxScore.status.equals("Live", ignoreCase = true)) Color(0xFFCC0000) else MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (game.boxScore.status.equals("Live", ignoreCase = true)) "▶ WATCH LIVE" else "▶ OPEN STREAM",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
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
fun TeamRow(team: com.example.bananasball.domain.model.Team, score: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = Color(0xFFF0F4F8)
            ) {
                if (team.logoUrl != null) {
                    AsyncImage(
                        model = team.logoUrl,
                        contentDescription = team.name,
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(team.shortName.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(team.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(team.shortName, color = Color.Gray, fontSize = 11.sp)
            }
        }
        Text(score.toString(), fontWeight = FontWeight.Black, fontSize = 20.sp)
    }
}
