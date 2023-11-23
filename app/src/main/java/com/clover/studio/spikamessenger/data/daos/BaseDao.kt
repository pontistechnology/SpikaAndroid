package com.clover.studio.spikamessenger.data.daos

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Upsert

interface BaseDao<T> {

    @Upsert
    fun upsert(obj: T)

    @Upsert
    fun upsert(obj: List<T>)

    @Delete
    fun delete(obj: T)

    @Insert
    fun insert(obj: T)
}
