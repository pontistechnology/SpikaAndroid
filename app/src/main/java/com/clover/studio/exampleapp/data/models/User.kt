package com.clover.studio.exampleapp.data.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.versionedparcelable.VersionedParcelize
import com.clover.studio.exampleapp.data.AppDatabase
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = AppDatabase.TablesInfo.TABLE_USER)
data class User(

    @PrimaryKey
    @ColumnInfo(name = AppDatabase.TablesInfo.ID)
    val id: String,

    @SerializedName("login_name")
    @ColumnInfo(name = "login_name")
    val loginName: String,

    @SerializedName("nick_name")
    @ColumnInfo(name = "nick_name")
    val nickname: String,

    @SerializedName("avatar_url")
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String,

    @SerializedName("local_name")
    @ColumnInfo(name = "local_name")
    val localName: String,

    @SerializedName("blocked")
    @ColumnInfo(name = "blocked")
    val blocked: Boolean,

    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @SerializedName("modified_at")
    @ColumnInfo(name = "modified_at")
    val modifiedAt: String
) : Parcelable
