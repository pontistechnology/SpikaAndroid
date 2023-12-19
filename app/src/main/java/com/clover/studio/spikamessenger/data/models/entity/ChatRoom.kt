package com.clover.studio.spikamessenger.data.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.clover.studio.spikamessenger.data.AppDatabase
import com.clover.studio.spikamessenger.data.models.networking.responses.RoomUsers
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
    var name: String?,

    @ColumnInfo(name = "type")
    val type: String?,

    // Used only for local db insert logic
    @ColumnInfo(name = "avatar_file_id")
    var avatarFileId: Long?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long?,

    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long?,

    @ColumnInfo(name = "muted")
    var muted: Boolean,

    @ColumnInfo(name = "pinned")
    var pinned: Boolean,

    @ColumnInfo(name = "room_exit")
    var roomExit: Boolean = false,

    @ColumnInfo(name = "deleted")
    var deleted: Boolean = false,

    @ColumnInfo(name = "unread_count")
    var unreadCount: Int = 0
) : Parcelable {
    @Ignore
    @IgnoredOnParcel
    val users: List<RoomUsers> = ArrayList()

    @Ignore
    @IgnoredOnParcel
    val hasAvatar: Boolean = avatarFileId != null && avatarFileId!! > 0L

    @Ignore
    @IgnoredOnParcel
    var isForwarded : Boolean = false

    @Ignore
    @IgnoredOnParcel
    var selected : Boolean = false
}