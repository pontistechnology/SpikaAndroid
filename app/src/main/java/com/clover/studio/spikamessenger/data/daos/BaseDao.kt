package com.clover.studio.spikamessenger.data.daos

import androidx.room.Delete
import androidx.room.Upsert

interface BaseDao<T> {

    @Upsert
    fun upsert(obj: T)

    @Upsert
    fun upsert(obj: List<T>)

    @Delete
    fun delete(obj: T)
}