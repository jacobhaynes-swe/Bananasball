package com.example.bananasball.data.remote

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.*

/**
 * Ktor-based implementation of [ScheduleScraper] that fetches and parses the schedule
 * from the Savannah Bananas website.
 */
class KtorScheduleScraper(
    private val httpClient: HttpClient
) : ScheduleScraper {

    override suspend fun fetchSchedule(): List<ScrapedGame> {
        println("Scraper: Fetching schedule...")
        val html = try {
            httpClient.get("https://thesavannahbananas.com/schedule/").bodyAsText()
        } catch (e: Exception) {
            println("Scraper: Error fetching: ${e.message}")
            return emptyList()
        }

        val doc = Ksoup.parse(html)
        println("Scraper: Parsed HTML. Document size: ${html.length}")
        val games = mutableListOf<ScrapedGame>()

        // The schedule is contained within rows with the "event_row" class
        val eventRows = doc.select(".event_row")
        println("Scraper: Found ${eventRows.size} event rows")
        for (row in eventRows) {
            val date = row.selectFirst(".event_date p")?.text()?.trim() ?: ""
            
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
                    teamCodes = listOf("SB", "PA", "FF", "TG", "IC").filter { text.contains(it) }
                }
                
                println("Scraper: Processing game with teams: $teamCodes")

                // 2. Location
                val stadiumCol = content.selectFirst(".col_stadium")
                // Usually "Stadium City @Time", we want the stadium/city part
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
                val status = statusCol?.text()?.trim()

                // 5. YouTube URL
                val watchCol = content.selectFirst(".col_watch")
                var youtubeUrl = extractYoutubeUrl(watchCol, teamCodes)

                var enrichedMetadata: EnrichedStreamMetadata? = null

                // If game is in the near future or Live, attempt to find direct stream link from the channel
                if (status?.contains("Live", ignoreCase = true) == true || isWithinNextDays(date, 3)) {
                    println("Scraper: Attempting deep discovery for upcoming stream at $youtubeUrl (Date: $date)")
                    enrichedMetadata = discoverEnrichedMetadata(youtubeUrl)
                    if (enrichedMetadata != null) {
                        println("Scraper: Discovered direct stream: ${enrichedMetadata.directUrl}")
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

    private suspend fun discoverEnrichedMetadata(channelUrl: String): EnrichedStreamMetadata? {
        return try {
            val html = httpClient.get(channelUrl).bodyAsText()
            
            // 1. Find the videoId that is specifically associated with a "Live" or "Scheduled" stream.
            // On a /streams page, this is often inside a "gridVideoRenderer" or "videoRenderer"
            // specifically for upcoming/live content.
            val videoIdRegex = Regex("\"videoId\":\"([^\"]+)\"")
            val videoIdMatch = videoIdRegex.find(html)
            val videoId = videoIdMatch?.groupValues?.get(1) ?: return null

            // 2. Parse the ytInitialPlayerResponse block for better data if we're on a direct video page
            // But since we're on the /streams page, let's look for the specific renderer data
            
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
            
            // Format check: "Thursday, August 20"
            val parts = dateStr.split(",").last().trim().split(" ")
            if (parts.size >= 2) {
                val monthStr = parts[0].uppercase()
                val dayVal = parts[1].toInt()
                val month = Month.valueOf(monthStr)
                val year = today.year
                val parsedDate = LocalDate(year, month, dayVal)
                
                // Allow games from today until 'days' into the future
                parsedDate in today..limit
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Maps a team name (from alt text) to a standard two-letter team code.
     */
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

    /**
     * Extracts the YouTube URL from the "Where to Watch" column or falls back
     * to the official channel for the participating teams.
     */
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
