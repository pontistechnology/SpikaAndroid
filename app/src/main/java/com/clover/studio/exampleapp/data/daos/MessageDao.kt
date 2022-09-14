package com.clover.studio.exampleapp.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.exampleapp.data.models.Message

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(messages: List<Message>)

    @Query("SELECT * FROM message WHERE room_id LIKE :messageId")
    fun getMessages(messageId: Int): LiveData<List<Message>>

    @Query("SELECT * FROM message WHERE id LIKE :messageId LIMIT 1")
    fun getMessageById(messageId: String): LiveData<Message>

    @Query("SELECT * FROM message")
    suspend fun getMessagesLocally(): List<Message>

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("DELETE FROM message WHERE id IN (:messageId)")
    suspend fun deleteMessage(messageId: List<Long>)

    @Query("DELETE FROM message")
    suspend fun removeMessages()
}