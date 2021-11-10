package com.clover.studio.exampleapp.data.models.networking

data class AuthResponse(
    val newUser: Boolean,
    val user: AppUser,
    val device: Device
)

data class AppUser(
    val id: Int,
    val displayName: String,
    val avatarUrl: String,
    val telephoneNumber: String,
    val emailAddress: String,
    val created: Int
)

data class Device(
    val id: Int,
    val userId: Int,
    val osName: String,
    val osVersion: Int,
    val appVersion: Int,
    val token: String,
    val pushToken: String,
    val tokenExpiredAt: Int,
)