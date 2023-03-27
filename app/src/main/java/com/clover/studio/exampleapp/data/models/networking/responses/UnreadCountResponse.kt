package com.clover.studio.exampleapp.data.models.networking.responses

data class UnreadCountResponse(
    val status: String?,
    val data: UnreadCountData
)

data class UnreadCountData(
    val unreadCounts: List<UnreadCountItem>?
)

data class UnreadCountItem(
    val roomId: Int,
    val unreadCount: Int
)
