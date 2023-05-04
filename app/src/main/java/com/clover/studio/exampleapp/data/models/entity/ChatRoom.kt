package com.clover.studio.exampleapp.data.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.clover.studio.exampleapp.data.AppDatabase
import com.clover.studio.exampleapp.data.models.networking.responses.RoomUsers
import com.google.gson.annotations.SerializedName
import com.vanniktech.emoji.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = AppDatabase.TablesInfo.TABLE_CHAT_ROOM)
data class ChatRoom @JvmOverloads constructor(
    @PrimaryKey
    @SerializedName("id")
    @ColumnInfo(name = "room_id")
    val roomId: Int,

    @ColumnInfo(name = "name")
    val name: String?,

    @ColumnInfo(name = "type")
    val type: String?,

    // Used only for local db insert logic
    @Ignore
    val deleted: Boolean = false,

    @ColumnInfo(name = "avatar_file_id")
    val avatarFileId: Long?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long?,

    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long?,

    @ColumnInfo(name = "muted")
    var muted: Boolean,

    @ColumnInfo(name = "pinned")
    var pinned: Boolean,

    @ColumnInfo(name = "room_exit")
    var roomExit: Boolean?,

    @ColumnInfo(name = "unread_count")
    var unreadCount: Int = 0
) : Parcelable {
    @Ignore
    @IgnoredOnParcel
    val users: List<RoomUsers> = ArrayList()

    @Ignore
    @IgnoredOnParcel
    val hasAvatar: Boolean = avatarFileId != null && avatarFileId > 0L
}