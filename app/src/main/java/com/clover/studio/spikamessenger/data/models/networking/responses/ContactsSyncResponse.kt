package com.clover.studio.spikamessenger.data.models.networking.responses

import com.clover.studio.spikamessenger.data.models.entity.User

data class ContactsSyncResponse(
    val status: String,
    val data: SyncData?
)

data class SyncData(
    val list: List<User>,
    val limit: Int,
    val count: Int
)