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
            val parsedDate = parseDate(game.date)
            val home = game.teamCodes.getOrNull(0) ?: ""
            val away = game.teamCodes.getOrNull(1) ?: ""
            val key = "${parsedDate}_${home}_${away}"
            gamesMap[key] = game
        }

        // 2. Fetch from Website Schedule HTML (gives future upcoming tour schedule with venues and watch channels)
        val htmlGames = fetchGamesFromHtml()
        for (game in htmlGames) {
            val parsedDate = parseDate(game.date)
            val home = game.teamCodes.getOrNull(0) ?: ""
            val away = game.teamCodes.getOrNull(1) ?: ""
            val key = "${parsedDate}_${home}_${away}"
            val altKey = "${parsedDate}_${away}_${home}"

            if (gamesMap.containsKey(key)) {
                val existing = gamesMap[key]!!
                gamesMap[key] = existing.copy(
                    location = if (existing.location.isNotBlank()) existing.location else game.location,
                    time = if (existing.time.isNotBlank()) existing.time else game.time,
                    youtubeUrl = existing.youtubeUrl ?: game.youtubeUrl
                )
            } else if (gamesMap.containsKey(altKey)) {
                val existing = gamesMap.remove(altKey)!!
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
                val candidate = discoverEnrichedMetadata(game.youtubeUrl ?: "")
                println("Scraper: candidate for ${game.youtubeUrl} -> viewerCount=${candidate?.viewerCount}, isLive=${candidate?.isLiveBroadcast}")
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
        metadata.scheduledStartTime?.toLongOrNull()?.let { seconds ->
            val streamDate = Instant.fromEpochSeconds(seconds).toLocalDateTime(TimeZone.currentSystemDefault()).date
            return streamDate == gameDate
        }
        if (metadata.isLiveBroadcast) {
            return gameDate == today
        }
        return gameDate == today
    }

    private suspend fun discoverEnrichedMetadata(targetUrl: String): EnrichedStreamMetadata? {
        if (targetUrl.isBlank()) return null
        return try {
            val directVideoId = if (targetUrl.contains("watch?v=") || targetUrl.contains("/live/")) {
                Regex("(?:watch\\?v=|/live/)([a-zA-Z0-9_-]+)").find(targetUrl)?.groupValues?.get(1)
            } else null

            val channelHtml = if (directVideoId != null) "" else {
                httpClient.get(targetUrl) {
                    headers.append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                }.bodyAsText()
            }
            val videoId = directVideoId ?: run {
                val videoIdRegex = Regex("(?:/watch\\?v=|/live/|\"videoId\"\\s*:\\s*\"|\"contentId\"\\s*:\\s*\")([a-zA-Z0-9_-]+)")
                videoIdRegex.find(channelHtml)?.groupValues?.get(1) ?: return null
            }
            
            val directUrl = "https://www.youtube.com/watch?v=$videoId"
            val thumbUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

            // Now fetch the watch page to get exact real-time live viewers and stream title
            val watchHtml = httpClient.get(directUrl) {
                headers.append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            }.bodyAsText()

            val titleRegex = Regex("<title>(.*?)(?: - YouTube)?</title>")
            val title = titleRegex.find(watchHtml)?.groupValues?.get(1)?.replace("&amp;", "&")

            val isLive = watchHtml.contains("\"isLive\":true") || 
                         watchHtml.contains("watching now") || 
                         watchHtml.contains("BADGE_STYLE_TYPE_LIVE_NOW") ||
                         watchHtml.contains("\"style\":\"LIVE\"")

            // Real-time live viewers count
            val origViewCountRegex = Regex("\"originalViewCount\"\\s*:\\s*\"(\\d+)\"")
            val runsViewCountRegex = Regex("\"viewCount\"\\s*:\\s*\\{\\s*\"videoViewCountRenderer\"\\s*:\\s*\\{.*?\"runs\"\\s*:\\s*\\[\\s*\\{\\s*\"text\"\\s*:\\s*\"([0-9,]+)\"")
            val watchingRegex = Regex("\"text\"\\s*:\\s*\"([0-9,]+)\"\\s*\\}\\s*,\\s*\\{\\s*\"text\"\\s*:\\s*\"\\s*watching")
            val fallbackWatchingRegex = Regex("([0-9,]+)\\s+watching")

            val viewerCount = origViewCountRegex.find(watchHtml)?.groupValues?.get(1)?.toIntOrNull()
                ?: runsViewCountRegex.find(watchHtml)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
                ?: watchingRegex.find(watchHtml)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
                ?: fallbackWatchingRegex.find(watchHtml)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()

            // Pre-game waiting count
            val waitingRegex = Regex("([0-9,]+)\\s+waiting")
            val waitingText = waitingRegex.find(watchHtml)?.groupValues?.get(1)
            val waitingCount = waitingText?.replace(",", "")?.toIntOrNull()

            // Scheduled time (Unix timestamp)
            val scheduledRegex = Regex("\"scheduledStartTime\"\\s*:\\s*\"(\\d+)\"")
            val scheduledStartTime = scheduledRegex.find(watchHtml)?.groupValues?.get(1)

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
        } catch (e: Exception) {
            println("Scraper: Deep discovery error for $targetUrl: ${e.message}")
            null
        }
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
}
