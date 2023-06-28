package com.clover.studio.spikamessenger.data.models.networking.responses

import com.clover.studio.spikamessenger.data.models.entity.User

data class AuthResponse(
    val status: String,
    val data: AuthData
)

data class AuthData(
    val isNewUser: Boolean,
    val user: User,
    val device: Device
)

data class Device(
    val id: Int,
    val userId: Int,
    val token: String?,
    val tokenExpiredAt: Long?,
    val osName: String,
    val osVersion: String,
    val appVersion: String,
    val pushToken: String?,
)
