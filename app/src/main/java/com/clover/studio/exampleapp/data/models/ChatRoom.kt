package com.clover.studio.exampleapp.data.models

import androidx.room.*
import com.clover.studio.exampleapp.data.AppDatabase
import com.clover.studio.exampleapp.data.models.networking.RoomUsers

@Entity(tableName = AppDatabase.TablesInfo.TABLE_CHAT_ROOM)
data class ChatRoom(

    @PrimaryKey
    @ColumnInfo(name = AppDatabase.TablesInfo.ID)
    val id: Int,

    @ColumnInfo(name = "name")
    val name: String?,

    @ColumnInfo(name = "users")
    @TypeConverters(TypeConverter::class)
    val users: ArrayList<RoomUsers>? = ArrayList(),

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,

    @ColumnInfo(name = "type")
    val type: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long?
)