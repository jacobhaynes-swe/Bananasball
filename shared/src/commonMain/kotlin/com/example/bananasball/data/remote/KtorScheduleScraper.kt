package com.example.bananasball.data.remote

import com.example.bananasball.data.mapper.parseDate
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime
import kotlinx.datetime.*
import kotlinx.serialization.json.*

/**
 * Ktor-based implementation of [ScheduleScraper] that fetches and parses the schedule
 * from both the official stats API (for live, today, and past games) and the website schedule HTML.
 */
class KtorScheduleScraper(
    private val httpClient: HttpClient
) : ScheduleScraper {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @OptIn(ExperimentalTime::class)
    override suspend fun fetchSchedule(): List<ScrapedGame> {
        println("Scraper: Fetching schedule and live games...")
        val gamesMap = LinkedHashMap<String, ScrapedGame>()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        // 1. Fetch from official Stats Games API (gives live in-progress, today's games, and completed games)
        val apiGames = fetchGamesFromApi()
        for (game in apiGames) {
            val key = canonicalGameKey(game.date, game.teamCodes)
            gamesMap[key] = game
        }

        // 2. Fetch from Website Schedule HTML (gives future upcoming tour schedule with venues and watch channels)
        val htmlGames = fetchGamesFromHtml()
        for (game in htmlGames) {
            val key = canonicalGameKey(game.date, game.teamCodes)
            val existing = gamesMap[key]
            if (existing != null) {
                gamesMap[key] = existing.copy(
                    location = if (existing.location.isNotBlank()) existing.location else game.location,
                    time = if (existing.time.isNotBlank()) existing.time else game.time,
                    youtubeUrl = existing.youtubeUrl ?: game.youtubeUrl
                )
            } else {
                gamesMap[key] = game
            }
        }

        // 3. For Today's games or LIVE games, enrich with real-time YouTube stream metadata & live viewers
        val enrichedGames = mutableListOf<ScrapedGame>()
        for (game in gamesMap.values) {
            val parsedLocalDate = parseDate(game.date)
            val isTodayOrLive = parsedLocalDate == today || game.status?.contains("Live", ignoreCase = true) == true

            if (isTodayOrLive || isWithinNextDays(game.date, 1)) {
                val candidate = discoverEnrichedMetadata(game.youtubeUrl ?: "", game.teamCodes, parsedLocalDate, today)
                println("Scraper: candidate for ${game.youtubeUrl} -> viewerCount=${candidate?.viewerCount}, isLive=${candidate?.isLiveBroadcast}, title=${candidate?.title}")
                if (candidate != null && matchesGameDate(candidate, parsedLocalDate, today)) {
                    enrichedGames.add(
                        game.copy(
                            youtubeUrl = candidate.directUrl,
                            thumbnailUrl = candidate.thumbnailUrl ?: game.thumbnailUrl,
                            waitingCount = candidate.waitingCount ?: game.waitingCount,
                            viewerCount = candidate.viewerCount ?: game.viewerCount,
                            isLiveBroadcast = candidate.isLiveBroadcast || (game.status?.equals("LIVE", ignoreCase = true) == true),
                            actualStartTime = candidate.scheduledStartTime ?: game.actualStartTime,
                            streamTitle = candidate.title ?: game.streamTitle
                        )
                    )
                    continue
                }
            }
            enrichedGames.add(game)
        }

        return enrichedGames
    }

    private suspend fun fetchGamesFromApi(): List<ScrapedGame> {
        val list = mutableListOf<ScrapedGame>()
        try {
            val response = httpClient.get("https://banana-stats-pages-seven.vercel.app/api/stats/games").bodyAsText()
            val root = json.parseToJsonElement(response).jsonArray
            for (elem in root) {
                val obj = elem.jsonObject
                val date = obj["date"]?.jsonPrimitive?.contentOrNull ?: continue
                val rawTime = obj["time"]?.jsonPrimitive?.contentOrNull ?: "19:00:00"
                val venueTz = obj["venue_timezone"]?.jsonPrimitive?.contentOrNull ?: ""
                val time = if (venueTz.isNotBlank()) "$rawTime $venueTz" else rawTime

                val rawStatus = obj["status"]?.jsonPrimitive?.contentOrNull ?: "scheduled"
                val displayStatus = when (rawStatus.lowercase()) {
                    "in_progress", "in-progress", "live" -> "LIVE"
                    "final", "complete" -> "Final"
                    else -> "Scheduled"
                }

                val homeObj = obj["home_team"]?.jsonObject
                val awayObj = obj["away_team"]?.jsonObject

                val homeAbbr = homeObj?.get("abbreviation")?.jsonPrimitive?.contentOrNull ?: ""
                val homeName = homeObj?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
                val homeCode = com.example.bananasball.data.repository.StaticTeamProvider.getCodeFromName(homeAbbr)
                    ?: com.example.bananasball.data.repository.StaticTeamProvider.getCodeFromName(homeName) ?: "HOME"

                val awayAbbr = awayObj?.get("abbreviation")?.jsonPrimitive?.contentOrNull ?: ""
                val awayName = awayObj?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
                val awayCode = com.example.bananasball.data.repository.StaticTeamProvider.getCodeFromName(awayAbbr)
                    ?: com.example.bananasball.data.repository.StaticTeamProvider.getCodeFromName(awayName) ?: "AWAY"

                val homePoints = homeObj?.get("points")?.jsonPrimitive?.intOrNull
                val awayPoints = awayObj?.get("points")?.jsonPrimitive?.intOrNull

                val homeRuns = homeObj?.get("runs")?.jsonPrimitive?.intOrNull
                val awayRuns = awayObj?.get("runs")?.jsonPrimitive?.intOrNull

                val homeHits = homeObj?.get("hits")?.jsonPrimitive?.intOrNull
                val awayHits = awayObj?.get("hits")?.jsonPrimitive?.intOrNull

                val homeIp = homeObj?.get("starting_pitcher")?.jsonObject?.get("innings_pitched")?.jsonPrimitive?.doubleOrNull?.toFloat()
                val awayIp = awayObj?.get("starting_pitcher")?.jsonObject?.get("innings_pitched")?.jsonPrimitive?.doubleOrNull?.toFloat()

                val (inning, half, outs, display) = deriveInningState(homeIp, awayIp, displayStatus)

                val teamCodes = listOf(homeCode, awayCode)
                val channelUrl = com.example.bananasball.data.repository.StaticTeamProvider.getChannelUrl(homeCode)

                list.add(
                    ScrapedGame(
                        date = date,
                        location = "",
                        time = time,
                        teamCodes = teamCodes,
                        youtubeUrl = channelUrl,
                        homeScore = homePoints,
                        awayScore = awayPoints,
                        status = displayStatus,
                        isLiveBroadcast = displayStatus.equals("LIVE", ignoreCase = true),
                        awayRuns = awayRuns,
                        homeRuns = homeRuns,
                        awayHits = awayHits,
                        homeHits = homeHits,
                        currentInning = inning,
                        inningHalf = half,
                        outs = outs,
                        inningDisplay = display
                    )
                )
            }
        } catch (e: Exception) {
            println("Scraper: Error fetching API games: ${e.message}")
        }
        return list
    }

    private data class InningState(val inning: Int?, val half: String?, val outs: Int?, val display: String?)

    private fun deriveInningState(homeIp: Float?, awayIp: Float?, status: String): InningState {
        if (!status.equals("LIVE", ignoreCase = true) || homeIp == null || awayIp == null) {
            return InningState(null, null, null, null)
        }

        val homeFull = homeIp.toInt()
        val homeFrac = ((homeIp - homeFull) * 10f).roundToInt()

        val awayFull = awayIp.toInt()
        val awayFrac = ((awayIp - awayFull) * 10f).roundToInt()

        // 1. Home team pitcher is actively pitching in Top half (Home pitches first).
        // 4.2 IP means 4 full innings + 2 outs in 5th inning = Top of 5th, 2 Outs.
        if (homeFrac in 1..2) {
            val inning = homeFull + 1
            val outs = homeFrac
            val suffix = if (outs == 1) "Out" else "Outs"
            return InningState(inning, "TOP", outs, "▲ ${toOrdinal(inning)} • $outs $suffix")
        }

        // 2. Away team pitcher is actively pitching in Bottom half.
        // 4.2 IP means 4 full innings + 2 outs in 5th inning = Bottom of 5th, 2 Outs.
        if (awayFrac in 1..2) {
            val inning = maxOf(homeFull, awayFull + 1)
            val outs = awayFrac
            val suffix = if (outs == 1) "Out" else "Outs"
            return InningState(inning, "BOT", outs, "▼ ${toOrdinal(inning)} • $outs $suffix")
        }

        // 3. Both are whole numbers (e.g. 5.0 vs 2.0 or 5.0 vs 5.0)
        return if (homeFull > awayFull) {
            val inning = homeFull
            InningState(inning, "BOT", 0, "▼ ${toOrdinal(inning)} • 0 Outs")
        } else if (homeFull == awayFull && homeFull > 0) {
            val nextInning = homeFull + 1
            InningState(nextInning, "TOP", 0, "▲ ${toOrdinal(nextInning)} • 0 Outs")
        } else {
            InningState(1, "TOP", 0, "▲ 1ST • 0 Outs")
        }
    }

    private fun toOrdinal(n: Int): String {
        if (n in 11..13) return "${n}TH"
        return when (n % 10) {
            1 -> "${n}ST"
            2 -> "${n}ND"
            3 -> "${n}RD"
            else -> "${n}TH"
        }
    }

    private suspend fun fetchGamesFromHtml(): List<ScrapedGame> {
        val html = try {
            httpClient.get("https://thesavannahbananas.com/schedule/").bodyAsText()
        } catch (e: Exception) {
            println("Scraper: Error fetching HTML: ${e.message}")
            return emptyList()
        }

        val doc = Ksoup.parse(html)
        val games = mutableListOf<ScrapedGame>()
        val eventRows = doc.select(".event_row")

        for (row in eventRows) {
            val date = row.selectFirst(".event_date p")?.text()?.trim() ?: ""
            val contents = row.select(".event_content")
            for (content in contents) {
                val teamsCol = content.selectFirst(".col_teams")
                val teamImages = teamsCol?.select("img") ?: emptyList()
                val teamNames = teamImages.mapNotNull { it.attr("alt") }
                var teamCodes = teamNames.map { nameToCode(it) }

                if (teamCodes.isEmpty()) {
                    val text = teamsCol?.text() ?: ""
                    teamCodes = listOf("SB", "PA", "FF", "TG", "IC", "LBC").filter { text.contains(it) }
                }

                val stadiumCol = content.selectFirst(".col_stadium")
                val location = stadiumCol?.text()?.substringBefore("@")?.trim() ?: ""

                val timeCol = content.selectFirst(".col_time")
                val time = timeCol?.text()?.trim()?.takeIf { it.isNotEmpty() && it != "--" }
                    ?: stadiumCol?.selectFirst(".game-time")?.text()?.removePrefix("@")?.trim()
                    ?: ""

                val statusCol = content.selectFirst(".col_status")
                val status = statusCol?.text()?.trim() ?: "Scheduled"

                val watchCol = content.selectFirst(".col_watch")
                val youtubeUrl = extractYoutubeUrl(watchCol, teamCodes)

                games.add(
                    ScrapedGame(
                        date = date,
                        location = location,
                        time = time,
                        teamCodes = teamCodes,
                        youtubeUrl = youtubeUrl,
                        status = status
                    )
                )
            }
        }
        return games
    }

    @OptIn(ExperimentalTime::class)
    private fun matchesGameDate(metadata: EnrichedStreamMetadata, gameDate: LocalDate, today: LocalDate): Boolean {
        metadata.scheduledStartTime?.let { raw ->
            val streamDate = raw.toLongOrNull()?.let { seconds ->
                runCatching {
                    Instant.fromEpochSeconds(seconds).toLocalDateTime(TimeZone.currentSystemDefault()).date
                }.getOrNull()
            } ?: runCatching {
                Instant.parse(raw).toLocalDateTime(TimeZone.currentSystemDefault()).date
            }.getOrNull()

            if (streamDate != null) {
                return streamDate == gameDate
            }
        }
        if (metadata.isLiveBroadcast) {
            return gameDate == today
        }
        return gameDate == today
    }

    private suspend fun discoverEnrichedMetadata(
        targetUrl: String,
        teamCodes: List<String> = emptyList(),
        targetDate: LocalDate? = null,
        today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    ): EnrichedStreamMetadata? {
        if (targetUrl.isBlank() && teamCodes.isEmpty()) return null
        return try {
            val directVideoId = if (targetUrl.contains("watch?v=") || targetUrl.contains("/live/")) {
                Regex("(?:watch\\?v=|/live/)([a-zA-Z0-9_-]+)").find(targetUrl)?.groupValues?.get(1)
            } else null

            val videoIds = mutableListOf<String>()
            if (directVideoId != null) {
                videoIds.add(directVideoId)
            }

            // Determine channel URL to scan for scheduled/live video IDs
            val channelUrl = when {
                targetUrl.contains("youtube.com/@") || targetUrl.contains("youtube.com/c/") || targetUrl.contains("youtube.com/channel/") -> targetUrl
                teamCodes.isNotEmpty() -> com.example.bananasball.data.repository.StaticTeamProvider.getChannelUrl(teamCodes.first())
                else -> ""
            }

            if (channelUrl.isNotBlank()) {
                try {
                    val channelHtml = httpClient.get(channelUrl) {
                        headers.append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    }.bodyAsText()

                    val videoIdRegex = Regex("(?:/watch\\?v=|/live/|\"videoId\"\\s*:\\s*\")([a-zA-Z0-9_-]+)")
                    videoIdRegex.findAll(channelHtml).forEach { match ->
                        val vid = match.groupValues[1]
                        if (vid !in videoIds && vid !in listOf("live_stream", "hqdefault", "default")) {
                            videoIds.add(vid)
                        }
                    }
                } catch (e: Exception) {
                    println("Scraper: Channel scan failed for $channelUrl: ${e.message}")
                }
            }

            if (videoIds.isEmpty()) return null

            // Inspect candidate streams (up to 8 candidates)
            val candidates = mutableListOf<EnrichedStreamMetadata>()
            for (videoId in videoIds.take(8)) {
                val directUrl = "https://www.youtube.com/watch?v=$videoId"
                val thumbUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                val watchHtml = try {
                    httpClient.get(directUrl) {
                        headers.append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    }.bodyAsText()
                } catch (e: Exception) {
                    continue
                }

                val titleRegex = Regex("<title>(.*?)(?: - YouTube)?</title>")
                val title = titleRegex.find(watchHtml)?.groupValues?.get(1)?.replace("&amp;", "&")

                // Pre-game waiting count (for upcoming scheduled streams)
                val runsWaitingRegex = Regex("\"runs\"\\s*:\\s*\\[\\s*\\{\\s*\"text\"\\s*:\\s*\"([0-9,]+)\"\\s*\\}\\s*,\\s*\\{\\s*\"text\"\\s*:\\s*\"[^\"]*waiting", RegexOption.IGNORE_CASE)
                val textWaitingRegex = Regex("\"text\"\\s*:\\s*\"([0-9,]+)\"\\s*\\}\\s*,\\s*\\{\\s*\"text\"\\s*:\\s*\"\\s*waiting", RegexOption.IGNORE_CASE)
                val literalWaitingRegex = Regex("([0-9,]+)\\s+waiting", RegexOption.IGNORE_CASE)

                val waitingCount = runsWaitingRegex.find(watchHtml)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
                    ?: textWaitingRegex.find(watchHtml)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
                    ?: literalWaitingRegex.find(watchHtml)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()

                val isUpcoming = Regex("\"isUpcoming\"\\s*:\\s*true").containsMatchIn(watchHtml) || 
                    Regex("\"isLiveNow\"\\s*:\\s*false").containsMatchIn(watchHtml) || 
                    (waitingCount != null && waitingCount > 0)
                val isLive = (Regex("\"isLiveNow\"\\s*:\\s*true").containsMatchIn(watchHtml) || watchHtml.contains("watching now") || watchHtml.contains("BADGE_STYLE_TYPE_LIVE_NOW")) && !isUpcoming

                // Real-time live viewers count (only for active live broadcasts)
                val origViewCountRegex = Regex("\"originalViewCount\"\\s*:\\s*\"(\\d+)\"")
                val runsViewCountRegex = Regex("\"viewCount\"\\s*:\\s*\\{\\s*\"videoViewCountRenderer\"\\s*:\\s*\\{.*?\"runs\"\\s*:\\s*\\[\\s*\\{\\s*\"text\"\\s*:\\s*\"([0-9,]+)\"")
                val watchingRegex = Regex("\"text\"\\s*:\\s*\"([0-9,]+)\"\\s*\\}\\s*,\\s*\\{\\s*\"text\"\\s*:\\s*\"\\s*watching")
                val fallbackWatchingRegex = Regex("([0-9,]+)\\s+watching now")

                val viewerCount = if (isLive) {
                    origViewCountRegex.find(watchHtml)?.groupValues?.get(1)?.toIntOrNull()
                        ?: runsViewCountRegex.find(watchHtml)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
                        ?: watchingRegex.find(watchHtml)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
                        ?: fallbackWatchingRegex.find(watchHtml)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
                } else null

                // Scheduled start time (ISO string or Unix timestamp)
                val startTimestampRegex = Regex("\"startTimestamp\"\\s*:\\s*\"([^\"]+)\"")
                val scheduledStartTimeRegex = Regex("\"scheduledStartTime\"\\s*:\\s*\"(\\d+)\"")
                val scheduledStartTime = startTimestampRegex.find(watchHtml)?.groupValues?.get(1)
                    ?: scheduledStartTimeRegex.find(watchHtml)?.groupValues?.get(1)

                candidates.add(
                    EnrichedStreamMetadata(
                        videoId = videoId,
                        directUrl = directUrl,
                        thumbnailUrl = thumbUrl,
                        waitingCount = waitingCount,
                        viewerCount = viewerCount,
                        isLiveBroadcast = isLive,
                        scheduledStartTime = scheduledStartTime,
                        title = title
                    )
                )
            }

            if (candidates.isEmpty()) return null

            // Prioritize English streams over Spanish:
            // 1. Date matches AND is English (not Spanish)
            // 2. Date matches AND is Spanish
            // 3. Live broadcast AND is English
            // 4. Live broadcast AND is Spanish
            // 5. English streams
            // 6. Any stream
            val sorted = candidates.sortedWith(
                compareBy<EnrichedStreamMetadata> { candidate ->
                    val matchesDate = if (targetDate != null) matchesGameDate(candidate, targetDate, today) else candidate.isLiveBroadcast
                    if (matchesDate) 0 else 1
                }.thenBy { candidate ->
                    if (isSpanishStream(candidate.title)) 1 else 0
                }
            )

            sorted.firstOrNull()
        } catch (e: Exception) {
            println("Scraper: Deep discovery error for $targetUrl: ${e.message}")
            null
        }
    }

    private fun isSpanishStream(title: String?): Boolean {
        if (title == null) return false
        val t = title.lowercase()
        return t.contains("español") || t.contains("espanol") || t.contains("spanish") || t.contains("transmisión")
    }

    @OptIn(ExperimentalTime::class)
    private fun isWithinNextDays(dateStr: String, days: Int): Boolean {
        return try {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val limit = today.plus(days, DateTimeUnit.DAY)
            val parsedDate = parseDate(dateStr)
            parsedDate in today..limit
        } catch (e: Exception) {
            false
        }
    }

    private fun nameToCode(name: String): String {
        return com.example.bananasball.data.repository.StaticTeamProvider.getCodeFromName(name) ?: run {
            val words = name.split(" ", "-", "_").filter { it.isNotEmpty() }
            if (words.size >= 2) {
                (words[0].take(1) + words[1].take(1)).uppercase()
            } else {
                name.take(2).uppercase()
            }
        }
    }

    private fun extractYoutubeUrl(watchCol: Element?, teamCodes: List<String>): String {
        val link = watchCol?.selectFirst("a")?.attr("href")
        if (link != null && (link.contains("youtube.com") || link.contains("youtu.be"))) {
            return link
        }

        val text = watchCol?.text() ?: ""
        val matchedCode = teamCodes.find { text.contains(it, ignoreCase = true) }
            ?: teamCodes.firstOrNull() ?: "SB"

        return com.example.bananasball.data.repository.StaticTeamProvider.getChannelUrl(matchedCode)
    }

    private fun canonicalGameKey(date: String, teamCodes: List<String>): String {
        val parsedDate = parseDate(date)
        val home = teamCodes.getOrNull(0) ?: ""
        val away = teamCodes.getOrNull(1) ?: ""
        val (teamA, teamB) = listOf(home, away).sorted()
        return "${parsedDate}_${teamA}_${teamB}"
    }
}
