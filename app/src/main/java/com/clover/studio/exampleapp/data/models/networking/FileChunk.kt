package com.clover.studio.exampleapp.data.models.networking

data class FileChunk(
    // chunk: Base64 encoded text of chunk
    val chunk: String,

    // offset: The position of the chunk in the entire file
    val offset: Long,

    // total: Total number of chunks
    val total: Long,

    // size: File size in bytes
    val size: Long,

    val mimeType: String,
    val fileName: String,

    // clientId: The random String to identify upload
    val clientId: String,

    // fileHash: md5 hash of the file
    var fileHash: String?,

    // type: The name of the other model to which the file refers to(avatar, message, group)
    var type: String?,

    // relationId: The id of the model the file refers to
    var relationId: Int?
)