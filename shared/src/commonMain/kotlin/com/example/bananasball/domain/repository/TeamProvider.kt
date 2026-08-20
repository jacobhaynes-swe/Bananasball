package com.example.bananasball.domain.repository

import com.example.bananasball.domain.model.Team

/**
 * Pure domain contract for retrieving and resolving team metadata.
 */
interface TeamProvider {
    fun getTeam(id: String): Team?
    fun getAllTeams(): List<Team>
    fun getCodeFromName(name: String): String?
    fun getChannelUrl(teamId: String): String?
}
