package com.example.bananasball.data.remote

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class KtorScheduleScraperTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun testStreamOnlyBindsToMatchingGameDate() = runTest {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val todayMonthName = today.month.name.lowercase().replaceFirstChar { it.uppercase() }
        val todayDay = today.day

        val scheduleHtml = """
        <div class="event_row" data-date="$today">
            <div class="event_date"><p>Thursday, $todayMonthName $todayDay</p></div>
            <div class="event_content">
                <div class="col col_teams">
                    <img alt="The Firefighters"><span>vs</span><img alt="Party Animals">
                </div>
                <div class="col col_stadium"><p>Billings, MT <span class="game-time">@7:00pm MST</span></p></div>
                <div class="col col_time"><p>7:00pm MST</p></div>
                <div class="col col_watch"><p>FF YouTube</p></div>
                <div class="col col_status"><p>Scheduled</p></div>
            </div>
        </div>
        <div class="event_row" data-date="2026-10-15">
            <div class="event_date"><p>Saturday, October 15</p></div>
            <div class="event_content">
                <div class="col col_teams">
                    <img alt="The Firefighters"><span>vs</span><img alt="Party Animals">
                </div>
                <div class="col col_stadium"><p>Savannah, GA <span class="game-time">@7:00pm EST</span></p></div>
                <div class="col col_time"><p>7:00pm EST</p></div>
                <div class="col col_watch"><p>FF YouTube</p></div>
                <div class="col col_status"><p>Scheduled</p></div>
            </div>
        </div>
        """.trimIndent()

        val mockYoutubeChannelHtml = """
        {
            "videoId": "liveStream123",
            "title": {"runs": [{"text": "Firefighters vs Party Animals Live"}]},
            "thumbnail": {"thumbnails": [{"url": "https://img.youtube.com/vi/liveStream123/maxresdefault.jpg"}]},
            "style": "LIVE",
            "isLive": true
        }
        2,450 watching now
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            when {
                url.contains("thesavannahbananas.com/schedule") -> respond(
                    content = ByteReadChannel(scheduleHtml),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "text/html")
                )
                url.contains("youtube.com") -> respond(
                    content = ByteReadChannel(mockYoutubeChannelHtml),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "text/html")
                )
                url.contains("stats/games") -> respond(
                    content = ByteReadChannel("[]"),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond(
                    content = ByteReadChannel(""),
                    status = HttpStatusCode.OK
                )
            }
        }

        val scraper = KtorScheduleScraper(HttpClient(mockEngine))
        val games = scraper.fetchSchedule()

        assertEquals(2, games.size)

        // Today's game should have the live stream attached and 2450 viewers
        val todayGame = games[0]
        assertNotNull(todayGame.thumbnailUrl)
        assertTrue(todayGame.isLiveBroadcast, "Today's active stream should be marked live")
        assertEquals(2450, todayGame.viewerCount)
        assertEquals("https://www.youtube.com/watch?v=liveStream123", todayGame.youtubeUrl)

        // October 15 game with identical matchup must NOT inherit today's stream metadata
        val futureGame = games[1]
        assertNull(futureGame.thumbnailUrl, "Future game must not inherit today's thumbnail")
        assertNull(futureGame.viewerCount, "Future game must not inherit today's viewer count")
        assertNull(futureGame.waitingCount, "Future game must not inherit today's waiting count")
    }
}
