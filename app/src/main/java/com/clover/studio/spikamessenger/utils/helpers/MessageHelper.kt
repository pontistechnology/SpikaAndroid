package com.clover.studio.spikamessenger.utils.helpers

import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.MessageBody
import com.clover.studio.spikamessenger.data.models.entity.MessageFile
import com.clover.studio.spikamessenger.data.models.networking.responses.ThumbnailData
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Tools

object MessageHelper {

    fun createTempTextMessage(
        text: String?,
        roomId: Int,
        unsentMessages: MutableList<Message>,
        localUserId: Int,
        thumbnailData: ThumbnailData? = null
    ): Message {
        return createTempMessage(
            roomId = roomId,
            unsentMessages = unsentMessages,
            localUserId = localUserId,
            messageType = Const.JsonFields.TEXT_TYPE,
            text = text,
            thumbnailData = thumbnailData
        )
    }

    fun createTempFileMessage(
        roomId: Int,
        unsentMessages: MutableList<Message>,
        localUserId: Int,
        messageType: String,
        file: MessageFile
    ): Message {
        return createTempMessage(
            roomId = roomId,
            unsentMessages = unsentMessages,
            localUserId = localUserId,
            messageType = messageType,
            file = file
        )
    }

    private fun createTempMessage(
        roomId: Int,
        unsentMessages: MutableList<Message>,
        localUserId: Int,
        messageType: String,
        text: String? = null,
        file: MessageFile? = null,
        thumbnailData: ThumbnailData? = null
    ): Message {
        val messageBody = MessageBody(
            referenceMessage = null,
            text = text,
            fileId = 1,
            thumbId = 1,
            file = file,
            thumb = null,
            subjectId = null,
            objectIds = null,
            type = "",
            objects = null,
            subject = "",
            thumbnailData = thumbnailData
        )

        return Message(
            id = FilesHelper.getUniqueRandomId(unsentMessages),
            fromUserId = localUserId,
            totalUserCount = 0,
            deliveredCount = -1,
            seenCount = 0,
            roomId = roomId,
            type = messageType,
            body = messageBody,
            createdAt = System.currentTimeMillis(),
            modifiedAt = null,
            deleted = null,
            replyId = null,
            localId = Tools.generateRandomId(),
            messageStatus = Resource.Status.LOADING.toString(),
            isForwarded = false,
            referenceMessage = null,
            uri = null,
            thumbUri = null
        )
    }
}
