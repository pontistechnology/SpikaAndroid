package com.clover.studio.exampleapp.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clover.studio.exampleapp.data.AppDatabase
import com.google.gson.annotations.SerializedName

@Entity(tableName = AppDatabase.TablesInfo.TABLE_CHAT)
data class Chat(

    @PrimaryKey
    @ColumnInfo(name = AppDatabase.TablesInfo.ID)
    val id: String,

    @SerializedName("name")
    @ColumnInfo(name = "name")
    val name: String,

    @SerializedName("type")
    @ColumnInfo(name = "type")
    val type: String,

    @SerializedName("muted")
    @ColumnInfo(name = "muted")
    val muted: Boolean,

    @SerializedName("group_url")
    @ColumnInfo(name = "group_url")
    val groupUrl: String,

    @SerializedName("pinned")
    @ColumnInfo(name = "pinned")
    val pinned: Boolean,

    @SerializedName("typing")
    @ColumnInfo(name = "typing")
    val typing: Boolean
)
