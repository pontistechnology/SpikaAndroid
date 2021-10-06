package com.clover.studio.exampleapp.data.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.clover.studio.exampleapp.data.models.Model

@Dao
interface ModelDao {
    @Query("SELECT * FROM model")
    fun getAllModels(): LiveData<List<Model>>

    // TODO add insert, delete, get single item
}