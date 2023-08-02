package com.clover.studio.spikamessenger.data.models.networking.responses

data class BlockResponse(
    val status: String,
    val data: BlockData
)

data class BlockData(
    val blockedUsers: List<BlockUser>?,
    val block: Block?,
    val deleted: Boolean?
)

data class Block(
    val id: Int,
    val userId: Int,
    val blockedId: Int
)

data class BlockUser(
    val id: Int
)
