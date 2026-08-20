package com.example.bananasball.data.remote

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

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
                    teamCodes = listOf("SB", "PA", "FF", "TG").filter { text.contains(it) }
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

                // 4. YouTube URL
                val watchCol = content.selectFirst(".col_watch")
                val youtubeUrl = extractYoutubeUrl(watchCol, teamCodes)

                // 5. Status
                val statusCol = content.selectFirst(".col_status")
                val status = statusCol?.text()?.trim()

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

    /**
     * Maps a team name (from alt text) to a standard two-letter team code.
     */
    private fun nameToCode(name: String): String {
        val normalized = name.lowercase()
        return when {
            normalized.contains("banana") -> "SB"
            normalized.contains("party animal") -> "PA"
            normalized.contains("firefighter") -> "FF"
            normalized.contains("tailgater") -> "TG"
            normalized.contains("clown") -> "IC"
            else -> {
                // Fallback: Use first characters of words
                val words = name.split(" ", "-", "_").filter { it.isNotEmpty() }
                if (words.size >= 2) {
                    (words[0].take(1) + words[1].take(1)).uppercase()
                } else {
                    name.take(2).uppercase()
                }
            }
        }
    }

    /**
     * Extracts the YouTube URL from the "Where to Watch" column or falls back
     * to the official channel for the participating teams.
     */
    private fun extractYoutubeUrl(watchCol: Element?, teamCodes: List<String>): String {
        // Check for an explicit link first
        val link = watchCol?.selectFirst("a")?.attr("href")
        if (link != null && (link.contains("youtube.com") || link.contains("youtu.be"))) {
            return link
        }

        // Fallback to text matching if no link is present
        val text = watchCol?.text() ?: ""
        val matchedCode = teamCodes.find { text.contains(it, ignoreCase = true) }
            ?: teamCodes.firstOrNull()

        return teamChannelMap[matchedCode] ?: DEFAULT_CHANNEL
    }

    companion object {
        private val teamChannelMap = mapOf(
            "SB" to "https://www.youtube.com/@TheSavannahBananas/streams",
            "PA" to "https://www.youtube.com/@thepartyanimals.bananaball/streams",
            "FF" to "https://www.youtube.com/@TheOfficialFirefighters/streams",
            "TG" to "https://www.youtube.com/@TheTexasTailgaters/streams",
            "IC" to "https://www.youtube.com/@TheIndianapolisClowns/streams",
            "V" to "https://www.youtube.com/@officialbananaball/streams"
        )
        private const val DEFAULT_CHANNEL = "https://www.youtube.com/@officialbananaball/streams"
    }
}
