package com.clover.studio.exampleapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model")
data class Model(

    @PrimaryKey
    val id: Int,
    val name: String
)