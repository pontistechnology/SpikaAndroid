package com.clover.studio.exampleapp.data.models

import androidx.room.*
import com.clover.studio.exampleapp.data.AppDatabase
import com.google.gson.annotations.SerializedName

@Entity(tableName = AppDatabase.TablesInfo.TABLE_MESSAGE)
data class Message(

    @PrimaryKey
    @ColumnInfo(name = AppDatabase.TablesInfo.ID)
    val id: Int,

    @SerializedName("fromUserId")
    @ColumnInfo(name = "from_user_id")
    val fromUserId: Int,

    @SerializedName("fromDeviceId")
    @ColumnInfo(name = "from_device_id")
    val fromDeviceId: Int,

    @SerializedName("totalDeviceCount")
    @ColumnInfo(name = "total_device_count")
    val totalDeviceCount: Int,

    @SerializedName("receivedCount")
    @ColumnInfo(name = "received_count")
    val receivedCount: Int,

    @SerializedName("seenCount")
    @ColumnInfo(name = "seen_count")
    val seenCount: Int,

    @SerializedName("roomId")
    @ColumnInfo(name = "room_id")
    val roomId: Int,

    @SerializedName("type")
    @ColumnInfo(name = "type")
    val type: String,

    @SerializedName("messageBody")
    @ColumnInfo(name = "message_body")
    @TypeConverters(TypeConverter::class)
    val messageBody: MessageBody,

    @SerializedName("createdAt")
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

data class MessageBody(
    val text: String,
    val type: String
)
