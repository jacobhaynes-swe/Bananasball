package com.example.bananasball.data.repository

import com.example.bananasball.domain.model.Team
import com.example.bananasball.domain.repository.TeamProvider

object StaticTeamProvider : TeamProvider {
    private val teams = mapOf(
        "SB" to Team(
            id = "SB",
            name = "Savannah Bananas",
            shortName = "Bananas",
            logoUrl = "https://thesavannahbananas.com/wp-content/uploads/2017/01/logo.png",
            primaryColorHex = "#FFE000",
            secondaryColorHex = "#002D62",
            rosterUrl = "https://thesavannahbananas.com/roster",
            websiteUrl = "https://thesavannahbananas.com/",
            youtubeChannelUrl = "https://www.youtube.com/@TheSavannahBananas/streams"
        ),
        "PA" to Team(
            id = "PA",
            name = "Party Animals",
            shortName = "Animals",
            logoUrl = "https://thepartyanimals.com/wp-content/uploads/2023/09/logo.png",
            primaryColorHex = "#FF007F",
            secondaryColorHex = "#1A1A1A",
            rosterUrl = "https://thepartyanimals.com/team/team-roster/",
            websiteUrl = "https://thepartyanimals.com/",
            youtubeChannelUrl = "https://www.youtube.com/@thepartyanimals.bananaball/streams"
        ),
        "FF" to Team(
            id = "FF",
            name = "Firefighters",
            shortName = "Firefighters",
            logoUrl = "https://thesavannahbananas.com/wp-content/uploads/2024/10/firefighters.png",
            primaryColorHex = "#E63946",
            secondaryColorHex = "#1D3557",
            rosterUrl = "https://thefirefighters.com/team/team-roster/",
            websiteUrl = "https://thefirefighters.com/",
            youtubeChannelUrl = "https://www.youtube.com/@TheOfficialFirefighters/streams"
        ),
        "TG" to Team(
            id = "TG",
            name = "Texas Tailgaters",
            shortName = "Tailgaters",
            logoUrl = "https://bananaball.com/wp-content/uploads/2024/10/RT_BBCL_Tailgaters_with-text-color-1.png",
            primaryColorHex = "#C1440E",
            secondaryColorHex = "#2B2D42",
            rosterUrl = "https://bananaball.com/wp-content/uploads/2026/01/2026-TG-ROSTER.pdf",
            websiteUrl = "https://bananaball.com/the-texas-tailgaters",
            youtubeChannelUrl = "https://www.youtube.com/@TheTexasTailgaters/streams"
        ),
        "IC" to Team(
            id = "IC",
            name = "Indianapolis Clowns",
            shortName = "Clowns",
            logoUrl = "https://bananaball.com/wp-content/uploads/2025/10/clowns.png",
            primaryColorHex = "#6A0572",
            secondaryColorHex = "#F7B267",
            rosterUrl = "https://bananaball.com/wp-content/uploads/2026/01/2026-CLOWNS-ROSTER.pdf",
            websiteUrl = "https://bananaball.com/indianapolisclowns",
            youtubeChannelUrl = "https://www.youtube.com/@TheIndianapolisClowns/streams"
        ),
        "LBC" to Team(
            id = "LBC",
            name = "Loco Beach Coconuts",
            shortName = "Coconuts",
            logoUrl = "https://bananaball.com/wp-content/uploads/2025/10/coconuts.png",
            primaryColorHex = "#00A896",
            secondaryColorHex = "#F4A261",
            rosterUrl = "https://bananaball.com/wp-content/uploads/2026/01/2026-LBC-ROSTER.pdf",
            websiteUrl = "https://bananaball.com/locobeachcoconuts",
            youtubeChannelUrl = "https://www.youtube.com/@Loco.Beach.Coconuts/streams"
        )
    )

    private val defaultChannel = "https://www.youtube.com/@officialbananaball/streams"

    override fun getTeam(id: String): Team? = teams[id.uppercase()]
    
    override fun getAllTeams(): List<Team> = teams.values.toList()

    override fun getChannelUrl(teamId: String): String {
        return teams[teamId.uppercase()]?.youtubeChannelUrl ?: defaultChannel
    }

    override fun getCodeFromName(name: String): String? {
        val normalized = name.lowercase().trim()
        return when {
            normalized.contains("banana") || normalized == "sb" || normalized == "sav" -> "SB"
            normalized.contains("party animal") || normalized == "pa" -> "PA"
            normalized.contains("firefighter") || normalized == "ff" -> "FF"
            normalized.contains("tailgater") || normalized == "tg" || normalized == "tex" -> "TG"
            normalized.contains("clown") || normalized == "ic" || normalized == "ind" -> "IC"
            normalized.contains("coconut") || normalized == "lbc" || normalized == "bc" -> "LBC"
            else -> null
        }
    }
}
