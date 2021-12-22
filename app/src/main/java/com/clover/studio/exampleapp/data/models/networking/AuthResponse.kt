package com.clover.studio.exampleapp.data.models.networking

import com.clover.studio.exampleapp.data.models.User

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
    val tokenExpiredAt: String?,
    val osName: String,
    val osVersion: Int,
    val appVersion: Int,
    val pushToken: String?,
)