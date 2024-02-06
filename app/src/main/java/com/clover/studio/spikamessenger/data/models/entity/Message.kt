package com.clover.studio.spikamessenger.data.models.entity

import android.os.Parcelable
import androidx.room.*
import com.clover.studio.spikamessenger.data.AppDatabase
import com.clover.studio.spikamessenger.data.models.FileMetadata
import com.clover.studio.spikamessenger.utils.Const
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = AppDatabase.TablesInfo.TABLE_MESSAGE)
data class Message @JvmOverloads constructor(

    @PrimaryKey
    @ColumnInfo(name = AppDatabase.TablesInfo.ID)
    var id: Int,

    @SerializedName("fromUserId")
    @ColumnInfo(name = "from_user_id")
    val fromUserId: Int?,

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
    @ColumnInfo(name = "room_id_message")
    val roomId: Int?,

    @SerializedName("type")
    @ColumnInfo(name = "type_message")
    var type: String?,

    @SerializedName("body")
    @ColumnInfo(name = "body")
    @TypeConverters(TypeConverter::class)
    val body: MessageBody?,

    @ColumnInfo("reference_message")
    @TypeConverters(TypeConverter::class)
    var referenceMessage: ReferenceMessage?,

    @SerializedName("createdAt")
    @ColumnInfo(name = "created_at_message")
    val createdAt: Long?,

    @SerializedName("modifiedAt")
    @ColumnInfo(name = "modified_at_message")
    val modifiedAt: Long?,

    @SerializedName("deleted")
    @ColumnInfo(name = "deleted_message")
    val deleted: Boolean?,

    @SerializedName("replyId")
    @ColumnInfo(name = "reply_id")
    val replyId: Long?,

    @SerializedName("localId")
    @ColumnInfo(name = "local_id")
    val localId: String?,

    @ColumnInfo("message_status")
    var messageStatus: String?,

    @ColumnInfo("uri")
    var originalUri: String?,

    @Ignore
    val unreadCount: Int = 0,

    // Two field below are used for firebase messaging and are not needed in the local db
    @Ignore
    @SerializedName("fromUserName")
    val userName: String = "",

    @SerializedName("isForwarded")
    @ColumnInfo("is_forwarded")
    val isForwarded: Boolean,

    @Ignore
    val groupName: String? = "",

    @Ignore
    val muted: Boolean? = false,

    @Ignore
    var reaction: String = "",

    @Ignore
    var messagePosition: Int = 0,

    @Ignore
    var uploadProgress: Int = 0

    // @Ignore
    // var roomUser: String = ""
) : Parcelable {
    init {
        handleReferenceMessage()
    }

    fun canDelete(): Boolean {
        return deleted == null || deleted == true || Const.JsonFields.SYSTEM_TYPE == type
    }

    fun handleReferenceMessage(): Message {
        if (body?.referenceMessage != null) {
            referenceMessage = body.referenceMessage
            body.referenceMessage = null
        }

        return this@Message
    }
}

@Parcelize
data class MessageBody(
    var referenceMessage: ReferenceMessage?,
    var text: String?,
    var fileId: Long?,
    var thumbId: Long?,
    var file: MessageFile?,
    var thumb: MessageFile?,
    val type: String?,
    val subjectId: Int?,
    val subject: String?,
    val objectIds: List<Int>?,
    val objects: List<String>?,
) : Parcelable

@Parcelize
data class ReferenceMessage(
    var id: Int?,
    var fromUserId: Int?,
    var totalUserCount: Int?,
    var deliveredCount: Int?,
    var seenCount: Int?,
    var roomId: Int?,
    var type: String?,
    var body: ReplyBody,
    var createdAt: Long?,
    val modifiedAt: Long?,
    val deleted: Boolean?,
    val reply: Boolean?,
) : Parcelable

@Parcelize
data class ReplyBody(
    var text: String?,
    var fileId: Long?,
    var thumbId: Long?,
    var file: MessageFile?,
    var thumb: MessageFile?,
) : Parcelable

@Parcelize
data class MessageFile(
    val id: Long,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val metaData: FileMetadata?,
    val uri: String?
) : Parcelable
