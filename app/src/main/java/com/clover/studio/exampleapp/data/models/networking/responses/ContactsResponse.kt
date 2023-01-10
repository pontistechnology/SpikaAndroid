package com.clover.studio.exampleapp.data.models.networking.responses

import com.clover.studio.exampleapp.data.models.entity.User

data class ContactResponse(
    val status: String?,
    val data: Data?
)

data class Data(
    val list: List<User>?,
    val users: List<User>?,
    val count: Int?,
    val limit: Int?
)