package com.clover.studio.exampleapp.data.models

import com.clover.studio.exampleapp.utils.Const
import com.google.gson.JsonObject

data class UploadFile(
    // chunk: Base64 encoded text of chunk
    val chunk: String,

    // offset: The position of the chunk in the entire file
    val offset: Long,

    // total: Total number of chunks
    val total: Int,

    // size: File size in bytes
    val size: Long,

    val mimeType: String,
    val fileName: String,

    // clientId: The random String to identify upload
    val clientId: String,

    // fileHash: SHA-256 hash of the file
    var fileHash: String?,

    // type: The name of the other model to which the file refers to(avatar, message, group)
    var type: String?,

    var metaData: FileMetadata?
) {
    fun fileToJson(): JsonObject {
        val jsonObject = JsonObject()
        val metadataObject = JsonObject()

        jsonObject.addProperty(Const.JsonFields.TOTAL, total)
        jsonObject.addProperty(Const.JsonFields.SIZE, size)
        jsonObject.addProperty(Const.JsonFields.MIME_TYPE, mimeType)
        jsonObject.addProperty(Const.JsonFields.FILE_NAME, fileName)
        jsonObject.addProperty(Const.JsonFields.CLIENT_ID, clientId)
        jsonObject.addProperty(Const.JsonFields.FILE_HASH, fileHash)
        jsonObject.addProperty(Const.JsonFields.TYPE, type)

        if (metaData != null) {
            metadataObject.addProperty(Const.JsonFields.WIDTH, metaData?.width)
            metadataObject.addProperty(Const.JsonFields.HEIGHT, metaData?.height)
            metadataObject.addProperty(Const.JsonFields.DURATION, metaData?.duration)
            jsonObject.add(Const.JsonFields.METADATA, metadataObject)
        }

        return jsonObject
    }

    fun chunkToJson(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.addProperty(Const.JsonFields.CHUNK, chunk)
        jsonObject.addProperty(Const.JsonFields.OFFSET, offset)
        jsonObject.addProperty(Const.JsonFields.CLIENT_ID, clientId)

        return jsonObject
    }
}

data class FileMetadata(
    var width: Int?,
    var height: Int?,
    var duration: Int?
)
