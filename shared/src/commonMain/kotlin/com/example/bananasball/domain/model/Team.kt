package com.example.bananasball.domain.model

data class Team(
    val id: String,
    val name: String,
    val shortName: String,
    val logoUrl: String? = null,
    val primaryColorHex: String? = null,
    val secondaryColorHex: String? = null,
    val rosterUrl: String? = null,
    val websiteUrl: String? = null,
    val youtubeChannelUrl: String? = null
)
