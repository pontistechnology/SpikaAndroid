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

    // Two field below are used for firebase messaging and are not needed in the local db
    @Ignore
    @SerializedName("fromUserName")
    val userName: String = "",

    @Ignore
    val groupName: String? = "",

    @Ignore
    val muted: Int? = 0
)

data class MessageBody(
    var text: String?,
    var fileId: Long?,
    var thumbId: Long?,
    var file: MessageFile?,
    var thumb: MessageFile?
)

data class MessageFile(
    val fileName: String,
    val mimeType: String,
    val path: String,
    val size: Long
)

