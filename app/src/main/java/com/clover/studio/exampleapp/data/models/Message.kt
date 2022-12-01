package com.clover.studio.exampleapp.data.models

import androidx.room.*
import com.clover.studio.exampleapp.data.AppDatabase
import com.google.gson.annotations.SerializedName

@Entity(tableName = AppDatabase.TablesInfo.TABLE_MESSAGE)
data class Message @JvmOverloads constructor(

    @PrimaryKey
    @ColumnInfo(name = AppDatabase.TablesInfo.ID)
    val id: Int,

    @SerializedName("fromUserId")
    @ColumnInfo(name = "from_user_id")
    val fromUserId: Int?,

    @SerializedName("fromDeviceId")
    @ColumnInfo(name = "from_device_id")
    val fromDeviceId: Int?,

    @SerializedName("totalDeviceCount")
    @ColumnInfo(name = "total_device_count")
    val totalDeviceCount: Int?,

    @SerializedName("totalUserCount")
    @ColumnInfo(name = "total_user_count")
    val totalUserCount: Int?,

    @SerializedName("deliveredCount")
    @ColumnInfo(name = "delivered_count")
    val deliveredCount: Int?,

    @SerializedName("seenCount")
    @ColumnInfo(name = "seen_count")
    val seenCount: Int?,

    @SerializedName("roomId")
    @ColumnInfo(name = "room_id")
    val roomId: Int?,

    @SerializedName("type")
    @ColumnInfo(name = "type")
    val type: String?,

    @SerializedName("body")
    @ColumnInfo(name = "body")
    @TypeConverters(TypeConverter::class)
    val body: MessageBody?,

    @SerializedName("createdAt")
    @ColumnInfo(name = "created_at")
    val createdAt: Long?,

    @SerializedName("modifiedAt")
    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long?,

    @SerializedName("deleted")
    @ColumnInfo(name = "deleted")
    val deleted: Boolean?,

    @SerializedName("reply")
    @ColumnInfo(name = "reply")
    val reply: Boolean?,

    // Two field below are used for firebase messaging and are not needed in the local db
    @Ignore
    @SerializedName("fromUserName")
    val userName: String = "",

    @Ignore
    val groupName: String? = "",

    @Ignore
    val muted: Boolean? = false,

    @Ignore
    var reaction: String = "",

    @Ignore
    var messagePosition: Int = 0,

    @Ignore
    var senderMessage: Boolean = false,

    // @Ignore
    // var roomUser: String = ""
)
/*{
    fun jsonMessageObject(
        bindingSetup: FragmentChatMessagesBinding,
        mimeType: UploadMimeTypes,
        fileId: Long,
        thumbId: Long,
        roomId: Int?,
        replyFlag: Boolean,
        referenceMessage: Message,
    ): JsonObject {
        val jsonObject = JsonObject()
        val innerObject = JsonObject()
        val jsonRefObject = JsonObject()
        val jsonInnerRefObject = JsonObject()
        val jsonInnerFileObject = JsonObject()
        val jsonInnerThumbObject = JsonObject()

        innerObject.addProperty(
            Const.JsonFields.TEXT,
            bindingSetup.etMessage.text.toString()
        )
        innerObject.add(
            Const.JsonFields.REFERENCE_MESSAGE,
            jsonRefObject,
        )
        if (UploadMimeTypes.IMAGE == mimeType) {
            innerObject.addProperty(Const.JsonFields.FILE_ID, fileId)
            innerObject.addProperty(Const.JsonFields.THUMB_ID, thumbId)
            jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.CHAT_IMAGE)
        } else if (UploadMimeTypes.FILE == mimeType) {
            innerObject.addProperty(Const.JsonFields.FILE_ID, fileId)
            jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.FILE_TYPE)
        } else if (UploadMimeTypes.VIDEO == mimeType) {
            innerObject.addProperty(Const.JsonFields.FILE_ID, fileId)
            innerObject.addProperty(Const.JsonFields.THUMB_ID, thumbId)
            jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.VIDEO)
        } else jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.TEXT)

        jsonObject.addProperty(Const.JsonFields.ROOM_ID, roomId)

        if (replyFlag) {
            // Thumb
            /*jsonInnerThumbObject.addProperty(Const.JsonFields.FILE_NAME, referenceMessage.body?.file?.fileName)
            jsonInnerThumbObject.addProperty(Const.JsonFields.MIME_TYPE, referenceMessage.body?.file?.mimeType)
            jsonInnerThumbObject.addProperty(Const.JsonFields.PATH, referenceMessage.body?.file?.path)
            jsonInnerThumbObject.addProperty(Const.JsonFields.SIZE, referenceMessage.body?.file?.size)*/

            when (referenceMessage.type) {
                Const.JsonFields.CHAT_IMAGE, Const.JsonFields.VIDEO, Const.JsonFields.AUDIO, Const.JsonFields.FILE_TYPE -> {
                    jsonInnerFileObject.addProperty(
                        Const.JsonFields.FILE_NAME,
                        referenceMessage.body?.file?.fileName
                    )
                    jsonInnerFileObject.addProperty(
                        Const.JsonFields.MIME_TYPE,
                        referenceMessage.body?.file?.mimeType
                    )
                    jsonInnerFileObject.addProperty(
                        Const.JsonFields.PATH,
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
                    jsonInnerRefObject.add(Const.JsonFields.THUMB, jsonInnerThumbObject)
                }
                else -> {
                    jsonInnerRefObject.addProperty(
                        Const.JsonFields.TEXT,
                        referenceMessage.body?.text
                    )
                    jsonInnerRefObject.addProperty(Const.JsonFields.LOCAL_ID, 0)
                }
            }

            jsonRefObject.addProperty(Const.JsonFields.ID, referenceMessage.id)
            jsonRefObject.addProperty(Const.JsonFields.FROM_USER_ID, referenceMessage.fromUserId)
            jsonRefObject.addProperty(
                Const.JsonFields.TOTAL_USER_COUNT,
                referenceMessage.totalUserCount
            )
            jsonRefObject.addProperty(
                Const.JsonFields.DELIVERED_COUNT,
                referenceMessage.deliveredCount
            )
            jsonRefObject.addProperty(Const.JsonFields.SEEN_COUNT, referenceMessage.seenCount)
            jsonRefObject.addProperty(Const.JsonFields.ROOM_ID, referenceMessage.roomId)
            jsonRefObject.addProperty(Const.JsonFields.TYPE, referenceMessage.type)
            jsonRefObject.add(Const.JsonFields.BODY, jsonInnerRefObject)
            jsonRefObject.addProperty(Const.JsonFields.CREATED_AT, referenceMessage.createdAt)
            jsonRefObject.addProperty(Const.JsonFields.MODIFIED_AT, referenceMessage.modifiedAt)
            jsonRefObject.addProperty(Const.JsonFields.LOCAL_ID, referenceMessage.modifiedAt)

            jsonObject.addProperty(Const.UserActions.MESSAGE_REPLY, true)
        }
        return jsonObject
    }*/

data class MessageBody(
    var referenceMessage: ReferenceMessage?,
    var text: String?,
    var fileId: Long?,
    var thumbId: Long?,
    var file: MessageFile?,
    var thumb: MessageFile?
)

data class ReferenceMessage(
    var id: Int?,
    var fromUserId: Int?,
    var totalDeviceCount: Int?,
    var deliveredCount: Int?,
    var seenCount: Int?,
    var roomId: Int?,
    var type: String?,
    var body: ReplyBody,
    var createdAt: Long?,
    val modifiedAt: Long?,
    val deleted: Boolean?,
    val reply: Boolean?,
)

data class ReplyBody(
    var text: String?,
    var file: MessageFile?,
    var thumb: MessageFile?,
)

data class MessageFile(
    val fileName: String,
    val mimeType: String,
    val path: String,
    val size: Long
)

