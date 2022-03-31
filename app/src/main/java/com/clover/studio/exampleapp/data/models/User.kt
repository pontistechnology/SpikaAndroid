package com.clover.studio.exampleapp.data.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clover.studio.exampleapp.data.AppDatabase
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

    @ColumnInfo(name = "telephone_number")
    val telephoneNumber: String?,

    @ColumnInfo(name = "telephone_number_hashed")
    val telephoneNumberHashed: String?,

    @ColumnInfo(name = "email_address")
    val emailAddress: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: String?
) : Parcelable
