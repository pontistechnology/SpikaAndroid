package com.clover.studio.exampleapp.data.models.networking.responses

data class BlockResponse(
    val status: String,
    val data: BlockData
)

data class BlockData(
    val blockedUsers: List<Int>?,
    val block: Block?,
    val deleted: Boolean?
)

data class Block(
    val id: Int,
    val userId: Int,
    val blockedId: Int
)
