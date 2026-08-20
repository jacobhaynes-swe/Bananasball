package com.example.bananasball.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.bananasball.domain.model.Game
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onWatchLive: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.secondary)) {
                CenterAlignedTopAppBar(
                    title = { Text("BANANASBALL", color = Color.White, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
        if (state.isLoading && state.games.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.games) { game ->
                    GameCard(
                        game = game,
                        onWatchLive = onWatchLive
                    )
                }
                
                if (state.games.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No games scheduled for this date", color = Color.Gray)
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
    val dates = remember {
        val start = selectedDate.plus(-7, DateTimeUnit.DAY)
        (0..14).map { start.plus(it, DateTimeUnit.DAY) }
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(dates) { date ->
            val isSelected = date == selectedDate
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onDateSelected(date) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = date.dayOfWeek.name.take(3),
                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    color = if (isSelected) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 1. YouTube Thumbnail if available
            game.streamingMetadata?.thumbnailUrl?.let { thumbUrl ->
                Box(Modifier.fillMaxWidth().height(180.dp)) {
                    AsyncImage(
                        model = thumbUrl,
                        contentDescription = "Game Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    // Overlay Hype Count
                    game.streamingMetadata.waitingCount?.let { count ->
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "$count waiting",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                            game.streamingMetadata?.actualStartTime?.let { startTime ->
                                Text(
                                    text = "LIVE START: ${startTime.hour}:${startTime.minute.toString().padStart(2, '0')} UTC",
                                    fontSize = 11.sp,
                                    color = Color(0xFF002D62),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    Surface(
                        color = if (game.boxScore.status == "Live") Color.Red else Color.LightGray,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = game.boxScore.status.uppercase(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                TeamRow(team = game.awayTeam, score = game.boxScore.awayScore)
                Spacer(Modifier.height(8.dp))
                TeamRow(team = game.homeTeam, score = game.boxScore.homeScore)
                
                if (game.youtubeUrl != null) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onWatchLive(game.youtubeUrl) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (game.boxScore.status == "Live") "WATCH LIVE" else "OPEN STREAM",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
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
                Modifier.size(40.dp),
                shape = CircleShape,
                color = Color.LightGray.copy(alpha = 0.2f)
            ) {
                if (team.logoUrl != null) {
                    AsyncImage(
                        model = team.logoUrl,
                        contentDescription = team.name,
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(team.shortName.take(1), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(team.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Text(score.toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}
