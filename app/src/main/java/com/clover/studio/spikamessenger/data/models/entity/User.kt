package com.clover.studio.spikamessenger.data.models.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.clover.studio.spikamessenger.MainApplication
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.AppDatabase
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = AppDatabase.TablesInfo.TABLE_USER)
data class User(

    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val id: Int,

    @ColumnInfo(name = "display_name")
    val displayName: String?,

    @ColumnInfo(name = "avatar_file_id")
    val avatarFileId: Long?,

    @ColumnInfo(name = "telephone_number")
    val telephoneNumber: String?,

    @ColumnInfo(name = "telephone_number_hashed")
    val telephoneNumberHashed: String?,

    @ColumnInfo(name = "email_address")
    val emailAddress: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: String?,

    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long?,

    @ColumnInfo(name = "selected")
    var selected: Boolean = false,

    @ColumnInfo(name = "is_bot")
    var isBot: Boolean = false,

    @ColumnInfo(name = "deleted")
    val deleted: Boolean

) : Parcelable {
    @Ignore
    @IgnoredOnParcel
    var isAdmin: Boolean = false

    @Ignore
    @IgnoredOnParcel
    val hasAvatar: Boolean = avatarFileId != null && avatarFileId > 0L

    @get:Ignore
    @IgnoredOnParcel
    val formattedDisplayName: String
        get() = displayName?.takeIf { it.isNotEmpty() }
            ?: MainApplication.appContext.getString(R.string.unknown_user)
}
