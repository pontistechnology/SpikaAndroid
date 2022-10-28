package com.clover.studio.exampleapp.data.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clover.studio.exampleapp.data.AppDatabase
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = AppDatabase.TablesInfo.TABLE_MESSAGE_RECORDS)
data class MessageRecords(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,

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
    val createdAt: Long
) : Parcelable