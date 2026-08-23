package com.example.bananasball.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.bananasball.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailModalSheet(
    game: Game,
    detail: GameDetail?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            GameDetailHeader(game = game, detail = detail)

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    com.example.bananasball.ui.components.SpinningBaseballLoader(
                        text = "Loading Official Box Score...",
                        ballSize = 44.dp
                    )
                }
            } else if (detail != null) {
                val hasInnings = detail.homeTeam.innings.isNotEmpty() || detail.awayTeam.innings.isNotEmpty()
                val hasBatters = detail.homeTeam.batters.any { it.atBats > 0 || it.hits > 0 } || detail.awayTeam.batters.any { it.atBats > 0 || it.hits > 0 }

                if (!hasInnings && !hasBatters) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ℹ️", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Official stats not provided yet for this game",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Live box score and in-game statistics will update as official scorers enter data.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Line Score Matrix Table
                if (hasInnings) {
                    LineScoreMatrix(detail = detail)
                    Spacer(Modifier.height(20.dp))
                }

                // Tabbed Box Score Section (Lineup vs Bullpen vs Showdowns)
                BoxScoreTabs(detail = detail)
            } else {
                // Fallback for pre-game or when live API data is pending
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚾", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Lineup & Box Score Pending",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Official lineups and in-game box score will populate as game time approaches.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun GameDetailHeader(game: Game, detail: GameDetail?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Pill
            val statusText = detail?.status?.uppercase() ?: game.boxScore.status.uppercase()
            Surface(
                color = if (statusText.contains("LIVE")) Color(0xFFE53935) else MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = if (statusText.contains("LIVE")) "🔴 LIVE" else statusText,
                    color = if (statusText.contains("LIVE")) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Teams and Score Row
            val hasStats = (detail != null && (detail.homeTeam.innings.isNotEmpty() || detail.awayTeam.innings.isNotEmpty() || detail.homeTeam.batters.any { it.atBats > 0 || it.hits > 0 })) || game.boxScore.hasOfficialStats
            val awayPts = if (hasStats) (detail?.awayTeam?.pointsTotal ?: game.boxScore.awayScore) else null
            val homePts = if (hasStats) (detail?.homeTeam?.pointsTotal ?: game.boxScore.homeScore) else null

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Away Team
                TeamBannerItem(
                    name = game.awayTeam.shortName,
                    logo = game.awayTeam.logoUrl,
                    points = awayPts
                )

                // VS or Points Divider
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (hasStats && awayPts != null && homePts != null) {
                        Text(
                            text = "PTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$awayPts - $homePts",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFE000)
                        )
                        val awayR = detail?.awayTeam?.runsTotal ?: game.boxScore.awayRuns
                        val homeR = detail?.homeTeam?.runsTotal ?: game.boxScore.homeRuns
                        if (awayR != null && homeR != null) {
                            Text(
                                text = "($awayR - $homeR Runs)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "VS",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFE000)
                        )
                    }
                }

                // Home Team
                TeamBannerItem(
                    name = game.homeTeam.shortName,
                    logo = game.homeTeam.logoUrl,
                    points = homePts
                )
            }

            // Venue & Location
            val venueName = detail?.venue?.name ?: game.location
            if (venueName.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏟", fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (detail?.venue?.city != null) "$venueName • ${detail.venue.city}, ${detail.venue.state ?: ""}" else venueName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamBannerItem(name: String, logo: String?, points: Int? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (!logo.isNullOrBlank()) {
            AsyncImage(
                model = logo,
                contentDescription = name,
                modifier = Modifier.size(48.dp).clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFE000)),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(2), fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun LineScoreMatrix(detail: GameDetail) {
    val scrollState = rememberScrollState()
    val totalInnings = maxOf(detail.numberOfInnings, detail.awayTeam.innings.size, detail.homeTeam.innings.size, 9)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "INNING LINE SCORE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFE000),
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Column {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TEAM", modifier = Modifier.width(60.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        for (i in 1..totalInnings) {
                            Text(
                                text = "$i",
                                modifier = Modifier.width(28.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = if (i == 9) FontWeight.Bold else FontWeight.Normal,
                                color = if (i == 9) Color(0xFFFFE000) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Text("PTS", modifier = Modifier.width(36.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFFFE000))
                        Text("R", modifier = Modifier.width(30.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("H", modifier = Modifier.width(30.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Away Team Row
                    InningTeamRow(
                        teamAbbr = detail.awayTeam.abbreviation,
                        teamInnings = detail.awayTeam.innings,
                        totalInnings = totalInnings,
                        pointsTotal = detail.awayTeam.pointsTotal,
                        runsTotal = detail.awayTeam.runsTotal,
                        hitsTotal = detail.awayTeam.hitsTotal
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Home Team Row
                    InningTeamRow(
                        teamAbbr = detail.homeTeam.abbreviation,
                        teamInnings = detail.homeTeam.innings,
                        totalInnings = totalInnings,
                        pointsTotal = detail.homeTeam.pointsTotal,
                        runsTotal = detail.homeTeam.runsTotal,
                        hitsTotal = detail.homeTeam.hitsTotal
                    )
                }
            }
            
            Spacer(Modifier.height(6.dp))
            Text(
                text = "★ 9th Inning awards 2 points. Every regular inning won awards 1 point.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun InningTeamRow(
    teamAbbr: String,
    teamInnings: List<InningScore>,
    totalInnings: Int,
    pointsTotal: Int,
    runsTotal: Int,
    hitsTotal: Int
) {
    val inningsMap = teamInnings.associateBy { it.inning }

    Row(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(teamAbbr, modifier = Modifier.width(60.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        for (i in 1..totalInnings) {
            val inningScore = inningsMap[i]
            val runsText = inningScore?.runs?.toString() ?: "-"
            val pts = inningScore?.pointsAwarded ?: 0
            
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .then(
                        if (pts > 0) Modifier.background(Color(0xFFFFE000).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = runsText,
                    fontWeight = if (pts > 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (pts > 0) Color(0xFFFFE000) else MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp
                )
            }
        }
        Text("$pointsTotal", modifier = Modifier.width(36.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFFFFE000))
        Text("$runsTotal", modifier = Modifier.width(30.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        Text("$hitsTotal", modifier = Modifier.width(30.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun BoxScoreTabs(detail: GameDetail) {
    var selectedTab by remember { mutableStateOf(0) }
    val hasShowdowns = detail.homeTeam.showdownRounds.isNotEmpty() || detail.awayTeam.showdownRounds.isNotEmpty()
    val tabs = if (hasShowdowns) listOf("LINEUP", "PITCHING", "SHOWDOWNS") else listOf("LINEUP", "PITCHING")

    Column(modifier = Modifier.fillMaxWidth()) {
        // Segmented Tab Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedTab = index }
                        .background(if (isSelected) Color(0xFFFFE000) else Color.Transparent)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        when (selectedTab) {
            0 -> LineupTabContent(detail = detail)
            1 -> PitchingTabContent(detail = detail)
            2 -> ShowdownsTabContent(detail = detail)
        }
    }
}

@Composable
private fun LineupTabContent(detail: GameDetail) {
    var selectedTeamIndex by remember { mutableStateOf(0) }
    val teams = listOf(detail.awayTeam, detail.homeTeam)
    val currentTeam = teams[selectedTeamIndex]

    Column(modifier = Modifier.fillMaxWidth()) {
        // Team Toggle Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            teams.forEachIndexed { index, team ->
                val isSelected = selectedTeamIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { selectedTeamIndex = index }
                        .background(if (isSelected) Color(0xFFFFE000) else Color.Transparent)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${team.name} (${team.abbreviation})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Batter Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", modifier = Modifier.width(24.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("BATTER", modifier = Modifier.weight(1f), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text("AB", modifier = Modifier.width(28.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("R", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("H", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("RBI", modifier = Modifier.width(28.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("B4S", modifier = Modifier.width(32.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = Color(0xFFFFE000), fontWeight = FontWeight.Bold)
            Text("AVG", modifier = Modifier.width(44.dp), textAlign = TextAlign.End, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        if (currentTeam.batters.isEmpty()) {
            Text(
                text = "Lineup not yet available.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            currentTeam.batters.forEach { batter ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (batter.order in 1..10) "${batter.order}" else "-",
                        modifier = Modifier.width(24.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = batter.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (batter.jerseyNumber != null) {
                                Text(
                                    text = " #${batter.jerseyNumber}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (batter.positions.isNotEmpty()) {
                            Text(
                                text = batter.positions.joinToString(", "),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text("${batter.atBats}", modifier = Modifier.width(28.dp), textAlign = TextAlign.Center, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${batter.runs}", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${batter.hits}", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${batter.rbi}", modifier = Modifier.width(28.dp), textAlign = TextAlign.Center, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    
                    // Ball Four Sprint badge
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .then(
                                if (batter.ballFourSprints > 0) Modifier.background(Color(0xFFFFE000).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${batter.ballFourSprints}",
                            fontWeight = if (batter.ballFourSprints > 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (batter.ballFourSprints > 0) Color(0xFFFFE000) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = batter.battingAverage?.let { if (it >= 1.0) "1.000" else ".${(it * 1000).toInt()}" } ?: "-",
                        modifier = Modifier.width(44.dp),
                        textAlign = TextAlign.End,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
private fun PitchingTabContent(detail: GameDetail) {
    var selectedTeamIndex by remember { mutableStateOf(0) }
    val teams = listOf(detail.awayTeam, detail.homeTeam)
    val currentTeam = teams[selectedTeamIndex]

    Column(modifier = Modifier.fillMaxWidth()) {
        // Team Toggle Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            teams.forEachIndexed { index, team ->
                val isSelected = selectedTeamIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { selectedTeamIndex = index }
                        .background(if (isSelected) Color(0xFFFFE000) else Color.Transparent)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${team.name} Bullpen",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Pitcher Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PITCHER", modifier = Modifier.weight(1f), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text("IP", modifier = Modifier.width(32.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("H", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("R", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("ER", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("K", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = Color(0xFFFFE000), fontWeight = FontWeight.Bold)
            Text("ERA", modifier = Modifier.width(38.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("MPI", modifier = Modifier.width(42.dp), textAlign = TextAlign.End, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        if (currentTeam.pitchers.isEmpty()) {
            Text(
                text = "Bullpen stats not yet available.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            currentTeam.pitchers.forEach { pitcher ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = pitcher.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (pitcher.jerseyNumber != null) {
                                Text(
                                    text = " #${pitcher.jerseyNumber}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (pitcher.designations.isNotEmpty()) {
                            Text(
                                text = pitcher.designations.joinToString(", "),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(pitcher.inningsPitched, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${pitcher.hitsAllowed}", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${pitcher.runsAllowed}", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${pitcher.earnedRuns}", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${pitcher.strikeouts}", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFFFE000))
                    Text(pitcher.era?.let { "${it}" } ?: "-", modifier = Modifier.width(38.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(pitcher.minutesPerInning ?: "-", modifier = Modifier.width(42.dp), textAlign = TextAlign.End, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
private fun ShowdownsTabContent(detail: GameDetail) {
    val allShowdowns = (detail.awayTeam.showdownRounds + detail.homeTeam.showdownRounds).sortedBy { it.round }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (allShowdowns.isEmpty()) {
            Text(
                text = "No showdown tiebreaker rounds in this game.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            allShowdowns.forEach { sd ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Round ${sd.round}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFFFE000)
                            )
                            if (!sd.result.isNullOrBlank()) {
                                Text(
                                    text = sd.result,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (sd.isWalkoff) {
                            Surface(
                                color = Color(0xFFFFE000),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "WALK-OFF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
