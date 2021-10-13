package com.clover.studio.exampleapp.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clover.studio.exampleapp.data.AppDatabase
import com.google.gson.annotations.SerializedName

@Entity(tableName = AppDatabase.TablesInfo.TABLE_MESSAGE)
data class Message(

    @PrimaryKey
    @ColumnInfo(name = AppDatabase.TablesInfo.ID)
    val id: String,

    @SerializedName("user_id")
    @ColumnInfo(name = "user_id")
    val userId: String,

    @SerializedName("chat_id")
    @ColumnInfo(name = "chat_id")
    val chatId: String,

    @SerializedName("message")
    @ColumnInfo(name = "message")
    val message: String,

    @SerializedName("message_type")
    @ColumnInfo(name = "message_type")
    val messageType: String,

    @SerializedName("from_device_type")
    @ColumnInfo(name = "from_device_type")
    val fromDeviceType: String,

    @SerializedName("to_device_type")
    @ColumnInfo(name = "to_device_type")
    val toDeviceType: String,

    @SerializedName("file_path")
    @ColumnInfo(name = "file_path")
    val filePath: String,

    @SerializedName("file_mime_type")
    @ColumnInfo(name = "file_mime_type")
    val fileMimeType: String,

    @SerializedName("state")
    @ColumnInfo(name = "state")
    val state: String,

    @SerializedName("reply_message_id")
    @ColumnInfo(name = "reply_message_id")
    val replyMessageId: String,

    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @SerializedName("modified_at")
    @ColumnInfo(name = "modified_at")
    val modifiedAt: String
)
