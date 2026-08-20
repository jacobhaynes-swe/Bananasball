package com.example.bananasball.data.remote

import com.example.bananasball.data.repository.StaticTeamProvider
import com.example.bananasball.domain.model.Team
import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

class KtorTeamScraper(
    private val httpClient: HttpClient
) {
    suspend fun fetchTeams(): List<Team> {
        val baseTeams = StaticTeamProvider.getAllTeams().associateBy { it.id }.toMutableMap()
        
        try {
            val html = httpClient.get("https://bananaball.com/teams/").bodyAsText()
            val doc = Ksoup.parse(html)
            
            // Extract links from hover boxes and buttons
            val links = doc.select("a[href]")
            for (link in links) {
                val href = link.attr("href")
                val text = link.text()
                
                // Match team code
                val code = StaticTeamProvider.getCodeFromName(text) 
                    ?: StaticTeamProvider.getCodeFromName(href)
                
                if (code != null && baseTeams.containsKey(code)) {
                    val current = baseTeams[code]!!
                    if (href.endsWith(".pdf", ignoreCase = true) || href.contains("roster", ignoreCase = true)) {
                        baseTeams[code] = current.copy(rosterUrl = href)
                    }
                }
            }
        } catch (e: Exception) {
            println("KtorTeamScraper: Error scraping teams: ${e.message}")
        }

        return baseTeams.values.toList()
    }
}
