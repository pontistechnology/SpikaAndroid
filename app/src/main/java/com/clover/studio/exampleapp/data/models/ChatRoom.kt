package com.clover.studio.exampleapp.data.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clover.studio.exampleapp.data.AppDatabase
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = AppDatabase.TablesInfo.TABLE_CHAT_ROOM)
data class ChatRoom(

    @PrimaryKey
    @ColumnInfo(name = AppDatabase.TablesInfo.ID)
    val id: Int,

    @ColumnInfo(name = "name")
    val name: String?,

//    @ColumnInfo(name = "users")
//    val users: List<RoomUsers>?,

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,

    @ColumnInfo(name = "type")
    val type: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long?
) : Parcelable