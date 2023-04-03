package com.clover.studio.exampleapp.data.models.entity

import android.os.Parcelable
import androidx.room.*
import com.clover.studio.exampleapp.data.AppDatabase
import com.clover.studio.exampleapp.utils.helpers.TypeConverter
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
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

    @ColumnInfo(name = "record_message")
    @SerializedName("message")
    @TypeConverters(TypeConverter::class)
    val recordMessage: RecordMessage?
) : Parcelable

@Parcelize
data class RecordMessage(
    val id: Long,
    val totalUserCount: Int,
    val deliveredCount: Int,
    val seenCount: Int,
    val roomId: Int,
) : Parcelable