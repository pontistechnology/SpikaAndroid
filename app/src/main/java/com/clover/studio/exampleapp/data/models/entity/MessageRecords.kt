package com.clover.studio.exampleapp.data.models.entity

import androidx.room.*
import com.clover.studio.exampleapp.data.AppDatabase
import com.google.gson.annotations.SerializedName
import com.clover.studio.exampleapp.utils.helpers.TypeConverter

@Entity(tableName = AppDatabase.TablesInfo.TABLE_MESSAGE_RECORDS)
data class MessageRecords(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "message_id")
    val messageId: Int,

    @ColumnInfo(name = "user_id")
    val userId: Int,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "reaction")
    val reaction: String?,

    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @SerializedName("message")
    @TypeConverters(TypeConverter::class)
    val recordMessage: RecordMessage?
)

data class RecordMessage(
    val id: Long,
    val totalUserCount: Int,
    val deliveredCount: Int,
    val seenCount: Int,
    val roomId: Int,
    val modifiedAt: Long?,
)