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
    val id: Int,

    @SerializedName("fromUserId")
    @ColumnInfo(name = "from_user_id")
    val fromUserId: Int,

    @SerializedName("fromDeviceId")
    @ColumnInfo(name = "from_device_id")
    val fromDeviceId: Int,

    @SerializedName("sentCount")
    @ColumnInfo(name = "sent_count")
    val sentCount: Int,

    @SerializedName("receivedCount")
    @ColumnInfo(name = "received_count")
    val receivedCount: Int,

    @SerializedName("seenCount")
    @ColumnInfo(name = "seen_count")
    val seenCount: Int,

    @SerializedName("roomId")
    @ColumnInfo(name = "room_id")
    val roomId: Int,

    @SerializedName("messageType")
    @ColumnInfo(name = "message_type")
    val messageType: Int,

    @SerializedName("message")
    @ColumnInfo(name = "message")
    val message: String
)
