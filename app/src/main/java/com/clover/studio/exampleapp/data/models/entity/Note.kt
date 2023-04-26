package com.clover.studio.exampleapp.data.models.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clover.studio.exampleapp.data.AppDatabase
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = AppDatabase.TablesInfo.TABLE_NOTES)
data class Note(
    @PrimaryKey
    @ColumnInfo(name = AppDatabase.TablesInfo.ID)
    val id: Int,

    @ColumnInfo(name = "title")
    val title: String?,

    @ColumnInfo(name = "content")
    val content: String?,

    @ColumnInfo(name = "room_id")
    val roomId: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long
): Parcelable
