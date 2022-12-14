package com.clover.studio.exampleapp.data.models

import com.clover.studio.exampleapp.data.models.entity.Message
import com.clover.studio.exampleapp.utils.Const
import com.google.gson.JsonObject

data class JsonMessage(
    val msgText: String?,
    val mimeType: String,
    val fileId: Long?,
    val thumbId: Long?,
    val roomId: Int?,
    val localId: String?
) {
    fun messageToJson(
        replyFlag: Boolean,
        referenceMessage: Message?,
    ): JsonObject {
        val jsonObject = JsonObject()
        val innerObject = JsonObject()
        val jsonRefObject = JsonObject()
        val jsonInnerRefObject = JsonObject()
        val jsonInnerFileObject = JsonObject()
        val jsonInnerThumbObject = JsonObject()

        innerObject.addProperty(
            Const.JsonFields.TEXT_TYPE,
            msgText
        )
        innerObject.add(
            Const.JsonFields.REFERENCE_MESSAGE_REPLY,
            jsonRefObject,
        )
        if (mimeType.contains(Const.JsonFields.IMAGE_TYPE)) {
            innerObject.addProperty(Const.JsonFields.FILE_ID, fileId)
            innerObject.addProperty(Const.JsonFields.THUMB_ID, thumbId)
            jsonObject.addProperty(Const.JsonFields.TYPE, mimeType)
        } else if (mimeType.contains(Const.JsonFields.FILE_TYPE) || mimeType.contains(Const.JsonFields.AUDIO_TYPE)) {
            innerObject.addProperty(Const.JsonFields.FILE_ID, fileId)
            jsonObject.addProperty(Const.JsonFields.TYPE, mimeType)
        } else if (mimeType.contains(Const.JsonFields.VIDEO_TYPE)) {
            innerObject.addProperty(Const.JsonFields.FILE_ID, fileId)
            innerObject.addProperty(Const.JsonFields.THUMB_ID, thumbId)
            jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.VIDEO_TYPE)
        } else jsonObject.addProperty(Const.JsonFields.TYPE, mimeType)

        jsonObject.addProperty(Const.JsonFields.LOCAL_ID, localId)
        jsonObject.addProperty(Const.JsonFields.ROOM_ID, roomId)

        if (replyFlag) {
            // For now, the thumb json field is commented out because I'm not sure how much is needed for reply messages
            // Also, I believe that some other fields are not necessary and will need to be removed later
            /*jsonInnerThumbObject.addProperty(Const.JsonFields.FILE_NAME, referenceMessage.body?.file?.fileName)
            jsonInnerThumbObject.addProperty(Const.JsonFields.MIME_TYPE, referenceMessage.body?.file?.mimeType)
            jsonInnerThumbObject.addProperty(Const.JsonFields.PATH, referenceMessage.body?.file?.path)
            jsonInnerThumbObject.addProperty(Const.JsonFields.SIZE, referenceMessage.body?.file?.size)*/

            when (referenceMessage?.type) {
                Const.JsonFields.IMAGE_TYPE, Const.JsonFields.VIDEO_TYPE, Const.JsonFields.AUDIO_TYPE, Const.JsonFields.FILE_TYPE -> {
                    jsonInnerFileObject.addProperty(
                        Const.JsonFields.FILE_NAME,
                        referenceMessage.body?.file?.fileName
                    )
                    jsonInnerFileObject.addProperty(
                        Const.JsonFields.MIME_TYPE,
                        referenceMessage.body?.file?.mimeType
                    )
                    jsonInnerFileObject.addProperty(
                        Const.JsonFields.PATH_REPLY,
                        referenceMessage.body?.file?.path
                    )
                    jsonInnerFileObject.addProperty(
                        Const.JsonFields.SIZE,
                        referenceMessage.body?.file?.size
                    )
                    jsonInnerRefObject.addProperty(
                        Const.JsonFields.FILE_ID,
                        referenceMessage.body?.fileId
                    )
                    jsonInnerRefObject.addProperty(
                        Const.JsonFields.THUMB_ID,
                        referenceMessage.body?.thumbId
                    )
                    jsonInnerRefObject.add(Const.JsonFields.FILE_TYPE, jsonInnerFileObject)
                    jsonInnerRefObject.add(Const.JsonFields.THUMB_REPLY, jsonInnerThumbObject)
                }
                else -> {
                    jsonInnerRefObject.addProperty(
                        Const.JsonFields.TEXT_TYPE,
                        referenceMessage?.body?.text
                    )
                    jsonInnerRefObject.addProperty(Const.JsonFields.LOCAL_ID_REPLY, 0)
                }
            }

            jsonRefObject.addProperty(Const.Networking.ID, referenceMessage?.id)
            jsonRefObject.addProperty(
                Const.JsonFields.FROM_USER_ID_REPLY,
                referenceMessage?.fromUserId
            )
            jsonRefObject.addProperty(
                Const.JsonFields.TOTAL_USER_COUNT_REPLY,
                referenceMessage?.totalUserCount
            )
            jsonRefObject.addProperty(
                Const.JsonFields.DELIVERED_COUNT_REPLY,
                referenceMessage?.deliveredCount
            )
            jsonRefObject.addProperty(
                Const.JsonFields.SEEN_COUNT_REPLY,
                referenceMessage?.seenCount
            )
            jsonRefObject.addProperty(Const.JsonFields.ROOM_ID, referenceMessage?.roomId)
            jsonRefObject.addProperty(Const.JsonFields.TYPE, referenceMessage?.type)
            jsonRefObject.add(Const.JsonFields.BODY, jsonInnerRefObject)
            jsonRefObject.addProperty(
                Const.JsonFields.CREATED_AT_REPLY,
                referenceMessage?.createdAt
            )
            jsonRefObject.addProperty(
                Const.JsonFields.MODIFIED_AT_REPLY,
                referenceMessage?.modifiedAt
            )
            jsonRefObject.addProperty(Const.JsonFields.LOCAL_ID_REPLY, referenceMessage?.modifiedAt)

            jsonObject.addProperty(Const.UserActions.MESSAGE_REPLY, true)
        }
        jsonObject.add(Const.JsonFields.BODY, innerObject)

        return jsonObject
    }
}
