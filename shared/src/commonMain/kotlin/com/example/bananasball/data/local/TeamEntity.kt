package com.example.bananasball.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.bananasball.data.repository.StaticTeamProvider
import com.example.bananasball.domain.model.Team

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey val id: String,
    val name: String,
    val shortName: String,
    val logoUrl: String? = null,
    val primaryColorHex: String? = null,
    val secondaryColorHex: String? = null,
    val rosterUrl: String? = null,
    val websiteUrl: String? = null,
    val youtubeChannelUrl: String? = null
)

fun TeamEntity.toDomain(): Team {
    val fallback = StaticTeamProvider.getTeam(id)
    return Team(
        id = id,
        name = name.ifBlank { fallback?.name ?: id },
        shortName = shortName.ifBlank { fallback?.shortName ?: id },
        logoUrl = logoUrl ?: fallback?.logoUrl,
        primaryColorHex = primaryColorHex ?: fallback?.primaryColorHex,
        secondaryColorHex = secondaryColorHex ?: fallback?.secondaryColorHex,
        rosterUrl = rosterUrl ?: fallback?.rosterUrl,
        websiteUrl = websiteUrl ?: fallback?.websiteUrl,
        youtubeChannelUrl = youtubeChannelUrl ?: fallback?.youtubeChannelUrl
    )
}

fun Team.toEntity(): TeamEntity = TeamEntity(
    id = id,
    name = name,
    shortName = shortName,
    logoUrl = logoUrl,
    primaryColorHex = primaryColorHex,
    secondaryColorHex = secondaryColorHex,
    rosterUrl = rosterUrl,
    websiteUrl = websiteUrl,
    youtubeChannelUrl = youtubeChannelUrl
)
