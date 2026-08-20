package com.example.bananasball.data.remote

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.*
import kotlinx.serialization.json.*

/**
 * Ktor-based implementation of [ScheduleScraper] that fetches and parses the schedule
 * from the Savannah Bananas website and enriches with live scores and YouTube stream metadata.
 */
class KtorScheduleScraper(
    private val httpClient: HttpClient
) : ScheduleScraper {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun fetchSchedule(): List<ScrapedGame> {
        println("Scraper: Fetching schedule...")
        val html = try {
            httpClient.get("https://thesavannahbananas.com/schedule/").bodyAsText()
        } catch (e: Exception) {
            println("Scraper: Error fetching HTML: ${e.message}")
            return emptyList()
        }

        // Also fetch live scores from API to enrich games
        val scoresByDateAndTeams = fetchLiveGameScores()

        val doc = Ksoup.parse(html)
        val games = mutableListOf<ScrapedGame>()

        // The schedule is contained within rows with the "event_row" class
        val eventRows = doc.select(".event_row")
        for (row in eventRows) {
            val date = row.selectFirst(".event_date p")?.text()?.trim() ?: ""
            val isoDate = try { parseDate(date).toString() } catch (e: Exception) { "" }
            
            // Each event_row can contain multiple game entries (event_content)
            val contents = row.select(".event_content")
            for (content in contents) {
                // 1. Teams & Codes
                val teamsCol = content.selectFirst(".col_teams")
                val teamImages = teamsCol?.select("img") ?: emptyList()
                val teamNames = teamImages.mapNotNull { it.attr("alt") }
                var teamCodes = teamNames.map { nameToCode(it) }

                // Fallback: Check text for codes if image detection failed
                if (teamCodes.isEmpty()) {
                    val text = teamsCol?.text() ?: ""
                    teamCodes = listOf("SB", "PA", "FF", "TG", "IC", "LBC").filter { text.contains(it) }
                }

                // 2. Location
                val stadiumCol = content.selectFirst(".col_stadium")
                val location = stadiumCol?.text()
                    ?.substringBefore("@")
                    ?.trim() ?: ""

                // 3. Time
                val timeCol = content.selectFirst(".col_time")
                val time = timeCol?.text()?.trim()?.takeIf { it.isNotEmpty() && it != "--" }
                    ?: stadiumCol?.selectFirst(".game-time")?.text()?.removePrefix("@")?.trim()
                    ?: ""

                // 4. Status
                val statusCol = content.selectFirst(".col_status")
                var status = statusCol?.text()?.trim()

                // Check live score data
                val homeCode = teamCodes.getOrNull(0) ?: ""
                val awayCode = teamCodes.getOrNull(1) ?: ""
                val scoreKey = "${isoDate}_${homeCode}_${awayCode}"
                val altScoreKey = "${isoDate}_${awayCode}_${homeCode}"
                val liveScoreInfo = scoresByDateAndTeams[scoreKey] ?: scoresByDateAndTeams[altScoreKey]

                var homeScore: Int? = liveScoreInfo?.homePoints
                var awayScore: Int? = liveScoreInfo?.awayPoints
                if (liveScoreInfo?.status != null) {
                    status = liveScoreInfo.status
                }

                // 5. YouTube URL
                val watchCol = content.selectFirst(".col_watch")
                var youtubeUrl = extractYoutubeUrl(watchCol, teamCodes)

                var enrichedMetadata: EnrichedStreamMetadata? = null

                // If game is in the near future or Live, attempt to find direct stream link from the channel
                if (status?.contains("Live", ignoreCase = true) == true || isWithinNextDays(date, 3)) {
                    enrichedMetadata = discoverEnrichedMetadata(youtubeUrl)
                    if (enrichedMetadata != null) {
                        youtubeUrl = enrichedMetadata.directUrl
                    }
                }

                games.add(
                    ScrapedGame(
                        date = date,
                        location = location,
                        time = time,
                        teamCodes = teamCodes,
                        youtubeUrl = youtubeUrl,
                        homeScore = homeScore,
                        awayScore = awayScore,
                        status = status,
                        thumbnailUrl = enrichedMetadata?.thumbnailUrl,
                        waitingCount = enrichedMetadata?.waitingCount,
                        actualStartTime = enrichedMetadata?.scheduledStartTime,
                        streamTitle = enrichedMetadata?.title
                    )
                )
            }
        }
        return games
    }

    private data class ScoreEntry(
        val homePoints: Int,
        val awayPoints: Int,
        val status: String
    )

    private suspend fun fetchLiveGameScores(): Map<String, ScoreEntry> {
        val map = mutableMapOf<String, ScoreEntry>()
        try {
            val response = httpClient.get("https://banana-stats-pages-seven.vercel.app/api/stats/games").bodyAsText()
            val root = json.parseToJsonElement(response).jsonArray
            for (elem in root) {
                val obj = elem.jsonObject
                val date = obj["date"]?.jsonPrimitive?.contentOrNull ?: continue
                val rawStatus = obj["status"]?.jsonPrimitive?.contentOrNull ?: "final"
                val displayStatus = when (rawStatus.lowercase()) {
                    "in_progress", "live" -> "LIVE"
                    "final", "complete" -> "Final"
                    else -> "Scheduled"
                }

                val homeObj = obj["home_team"]?.jsonObject
                val awayObj = obj["away_team"]?.jsonObject

                val homeAbbr = homeObj?.get("abbreviation")?.jsonPrimitive?.contentOrNull ?: ""
                val homeName = homeObj?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
                val homeCode = com.example.bananasball.data.repository.StaticTeamProvider.getCodeFromName(homeAbbr)
                    ?: com.example.bananasball.data.repository.StaticTeamProvider.getCodeFromName(homeName) ?: ""

                val awayAbbr = awayObj?.get("abbreviation")?.jsonPrimitive?.contentOrNull ?: ""
                val awayName = awayObj?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
                val awayCode = com.example.bananasball.data.repository.StaticTeamProvider.getCodeFromName(awayAbbr)
                    ?: com.example.bananasball.data.repository.StaticTeamProvider.getCodeFromName(awayName) ?: ""

                val homePoints = homeObj?.get("points")?.jsonPrimitive?.intOrNull ?: 0
                val awayPoints = awayObj?.get("points")?.jsonPrimitive?.intOrNull ?: 0

                if (homeCode.isNotEmpty() && awayCode.isNotEmpty()) {
                    val key = "${date}_${homeCode}_${awayCode}"
                    map[key] = ScoreEntry(homePoints, awayPoints, displayStatus)
                }
            }
        } catch (e: Exception) {
            println("Scraper: Error fetching live scores: ${e.message}")
        }
        return map
    }

    private suspend fun discoverEnrichedMetadata(channelUrl: String): EnrichedStreamMetadata? {
        return try {
            val html = httpClient.get(channelUrl).bodyAsText()
            
            val videoIdRegex = Regex("\"videoId\":\"([^\"]+)\"")
            val videoIdMatch = videoIdRegex.find(html)
            val videoId = videoIdMatch?.groupValues?.get(1) ?: return null
            
            val titleRegex = Regex("\"title\":\\{\"runs\":\\[\\{\"text\":\"([^\"]+)\"\\}\\]")
            val title = titleRegex.find(html)?.groupValues?.get(1)

            val thumbRegex = Regex("\"thumbnail\":\\{\"thumbnails\":\\[\\{\"url\":\"([^\"]+)\"")
            val thumbUrl = thumbRegex.find(html)?.groupValues?.get(1)?.replace("\\u0026", "&")

            // Hype (Waiting)
            val waitingRegex = Regex("([0-9,]+)\\s+waiting")
            val waitingText = waitingRegex.find(html)?.groupValues?.get(1)
            val waitingCount = waitingText?.replace(",", "")?.toIntOrNull()

            // Scheduled time (Unix)
            val scheduledRegex = Regex("\"scheduledStartTime\":\"(\\d+)\"")
            val scheduledStartTime = scheduledRegex.find(html)?.groupValues?.get(1)

            EnrichedStreamMetadata(
                videoId = videoId,
                directUrl = "https://www.youtube.com/live/$videoId",
                thumbnailUrl = thumbUrl,
                waitingCount = waitingCount,
                scheduledStartTime = scheduledStartTime,
                title = title
            )
        } catch (e: Exception) {
            println("Scraper: Deep discovery error for $channelUrl: ${e.message}")
            null
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun isWithinNextDays(dateStr: String, days: Int): Boolean {
        return try {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val limit = today.plus(days, DateTimeUnit.DAY)
            
            val parts = dateStr.split(",").last().trim().split(" ")
            if (parts.size >= 2) {
                val monthStr = parts[0].uppercase()
                val dayVal = parts[1].toInt()
                val month = Month.valueOf(monthStr)
                val year = today.year
                val parsedDate = LocalDate(year, month, dayVal)
                
                parsedDate in today..limit
            } else {
                false
            }
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

    @OptIn(ExperimentalTime::class)
    private fun parseDate(dateStr: String): LocalDate {
        val parts = dateStr.split(",").last().trim().split(" ")
        if (parts.size >= 2) {
            val monthStr = parts[0].uppercase()
            val day = parts[1].toInt()
            val month = Month.valueOf(monthStr)
            val year = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
            return LocalDate(year, month, day)
        }
        return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
}
