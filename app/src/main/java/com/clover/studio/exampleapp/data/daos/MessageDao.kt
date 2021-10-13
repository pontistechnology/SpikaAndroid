package com.clover.studio.exampleapp.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.exampleapp.data.models.Message

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message): Long

    @Query("SELECT * FROM message")
    fun getMessages(): LiveData<List<Message>>

    @Query("SELECT * FROM message WHERE id LIKE :messageId LIMIT 1")
    fun getMessageById(messageId: String): LiveData<Message>

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("DELETE FROM message")
    suspend fun removeMessages()
}