package com.clover.studio.exampleapp.data.models.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.clover.studio.exampleapp.data.AppDatabase
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = AppDatabase.TablesInfo.TABLE_USER)
data class User(

    @PrimaryKey
    @ColumnInfo(name = AppDatabase.TablesInfo.ID)
    val id: Int,

    @ColumnInfo(name = "display_name")
    val displayName: String?,

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,

    @ColumnInfo(name = "avatar_file_id")
    val avatarFileId: Int?,

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
    var selected: Boolean = false
) : Parcelable {
    @Ignore
    @IgnoredOnParcel
    var isAdmin: Boolean = false
}
