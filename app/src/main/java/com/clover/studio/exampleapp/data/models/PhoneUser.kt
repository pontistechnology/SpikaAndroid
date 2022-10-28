package com.clover.studio.exampleapp.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clover.studio.exampleapp.data.AppDatabase
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = AppDatabase.TablesInfo.TABLE_PHONE_USER)
data class PhoneUser(
    val name: String,

    @PrimaryKey
    val number: String
) : Parcelable