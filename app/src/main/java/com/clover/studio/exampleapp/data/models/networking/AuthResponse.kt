package com.clover.studio.exampleapp.data.models.networking

data class AuthResponse(
    val newUser: Boolean,
    val user: User,
    val device: Device
)

data class User(
    val id: Int,
    val displayName: String?,
    val avatarUrl: String?,
    val telephoneNumber: String?,
    val emailAddress: String?,
    val createdAt: String?,
    val modifiedAt: String?
)

data class Device(
    val id: Int,
    val userId: Int,
    val deviceId: String,
    val type: String?,
    val deviceName: String?,
    val osName: String,
    val osVersion: Int,
    val appVersion: Int,
    val token: String?,
    val pushToken: String?,
    val tokenExpiredAt: String?,
    val createdAt: String,
    val modifiedAt: String?
)