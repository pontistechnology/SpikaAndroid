package com.clover.studio.spikamessenger.data.models.networking.responses

data class SettingsResponse(
   val status: String?,
   val data: Settings?
)

data class UserSettingsResponse(
    val status: String?,
    val data: SettingsData?
)

data class SettingsData(
    val settings: List<Settings>
)

data class Settings(
    val id: Int?,
    val userId: Int?,
    val key: String?,
    val value: Boolean?,
    val teamMode: Boolean?
)