package com.example.bananasball.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.bananasball.domain.model.StatLeader
import com.example.bananasball.domain.model.TeamStandings
import com.example.bananasball.ui.components.BananaPullToRefreshIndicator
import com.example.bananasball.ui.components.SpinningBaseballLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondary)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "LEAGUE HUB",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color(0xFFFFE000),
                            letterSpacing = 2.sp
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        titleContentColor = Color(0xFFFFE000)
                    )
                )

                // Segmented Tabs
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatsTab.values().forEach { tab ->
                            val isSelected = state.selectedTab == tab
                            Surface(
                                onClick = { viewModel.handleIntent(StatsIntent.OnTabSelected(tab)) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFFFE000) else Color.Transparent,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = tab.title,
                                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.handleIntent(StatsIntent.OnRefresh) },
            indicator = {
                BananaPullToRefreshIndicator(
                    state = pullToRefreshState,
                    isRefreshing = state.isLoading
                )
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            val isInitialLoading = state.isLoading && (state.standings == null && state.seasonStats == null)
            if (isInitialLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SpinningBaseballLoader(text = "Loading League Stats...")
                }
            } else {
                when (state.selectedTab) {
                    StatsTab.STANDINGS -> StandingsTab(standings = state.standings?.rankings ?: emptyList())
                    StatsTab.BATTING -> BattingLeadersTab(leaders = state.seasonStats?.battingLeaders ?: emptyList())
                    StatsTab.PITCHING -> PitchingLeadersTab(leaders = state.seasonStats?.pitchingLeaders ?: emptyList())
                }
            }
        }
    }
}

@Composable
fun StandingsTab(standings: List<TeamStandings>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Table Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(28.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("TEAM", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("W", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("L", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("PCT", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("GB", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("STRK", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        items(standings) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank badge
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = when (item.rank) {
                            1 -> MaterialTheme.colorScheme.primary
                            2 -> MaterialTheme.colorScheme.surfaceVariant
                            3 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = item.rank.toString(),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = if (item.rank == 1) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Team logo & name
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            if (item.team.logoUrl != null) {
                                AsyncImage(
                                    model = item.team.logoUrl,
                                    contentDescription = item.team.name,
                                    modifier = Modifier.fillMaxSize().padding(2.dp)
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = item.team.shortName.take(1),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = item.team.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = item.team.shortName,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Text(
                        text = item.wins.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.losses.toString(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = formatWinPct(item.winPercentage),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.width(44.dp),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (item.gamesBehind == 0.0) "-" else item.gamesBehind.toString(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )

                    // Streak badge
                    val streak = item.streak ?: "-"
                    val isWinStreak = streak.startsWith("W", ignoreCase = true)
                    Surface(
                        modifier = Modifier.width(44.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = if (isWinStreak) Color(0xFF166534).copy(alpha = 0.2f) else Color(0xFF991B1B).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = streak,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isWinStreak) Color(0xFF22C55E) else Color(0xFFEF4444),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BattingLeadersTab(leaders: List<StatLeader.Batting>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(leaders) { leader ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(28.dp),
                                shape = CircleShape,
                                color = if (leader.rank == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#${leader.rank}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = if (leader.rank == 1) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = leader.player,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (leader.team.logoUrl != null) {
                                        AsyncImage(
                                            model = leader.team.logoUrl,
                                            contentDescription = leader.team.name,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = leader.team.name,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Prominent AVG display
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatAvg(leader.avg),
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else Color(0xFF002D62)
                            )
                            Text(
                                text = "BATTING AVG",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(Modifier.height(10.dp))

                    // Secondary Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBox(label = "HR", value = leader.hr.toString())
                        StatBox(label = "RBI", value = leader.rbi.toString())
                        StatBox(label = "OPS", value = formatOps(leader.ops))
                        StatBox(label = "B4S ⚡", value = leader.b4s.toString())
                        StatBox(label = "SB", value = leader.stolenBases.toString())
                    }
                }
            }
        }
    }
}

@Composable
fun PitchingLeadersTab(leaders: List<StatLeader.Pitching>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(leaders) { leader ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(28.dp),
                                shape = CircleShape,
                                color = if (leader.rank == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#${leader.rank}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = if (leader.rank == 1) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = leader.player,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (leader.team.logoUrl != null) {
                                        AsyncImage(
                                            model = leader.team.logoUrl,
                                            contentDescription = leader.team.name,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = leader.team.name,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Prominent ERA display
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatDecimal(leader.era),
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else Color(0xFF002D62)
                            )
                            Text(
                                text = "EARNED RUN AVG",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(Modifier.height(10.dp))

                    // Pitching stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBox(label = "WINS", value = leader.wins.toString())
                        StatBox(label = "SO ⚡", value = leader.so.toString())
                        StatBox(label = "WHIP", value = formatDecimal(leader.whip))
                        StatBox(label = "IP", value = leader.inningsPitched.toString())
                        StatBox(label = "SV", value = leader.saves.toString())
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatWinPct(pct: Double): String {
    val rounded = ((pct * 1000).toInt() / 1000.0)
    return if (rounded >= 1.0) "1.000" else rounded.toString().padEnd(5, '0').removePrefix("0")
}

private fun formatAvg(avg: Double): String {
    val rounded = ((avg * 1000).toInt() / 1000.0)
    return rounded.toString().padEnd(5, '0').removePrefix("0")
}

private fun formatOps(ops: Double): String {
    val rounded = ((ops * 1000).toInt() / 1000.0)
    return rounded.toString().padEnd(5, '0')
}

private fun formatDecimal(value: Double): String {
    val rounded = ((value * 100).toInt() / 100.0)
    return rounded.toString().padEnd(4, '0')
}
