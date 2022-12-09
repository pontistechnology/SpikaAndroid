package com.clover.studio.exampleapp.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.exampleapp.data.models.entity.MessageRecords

@Dao
interface MessageRecordsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(messageRecords: MessageRecords): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(messageRecords: List<MessageRecords>)

    @Query("SELECT * FROM message_records WHERE message_id LIKE :messageId")
    fun getMessageRecords(messageId: Int): LiveData<List<MessageRecords>>

    @Query("SELECT * FROM message_records")
    suspend fun getMessageRecordsLocally(): List<MessageRecords>

    @Query("SELECT * FROM message_records WHERE id LIKE :messageId LIMIT 1")
    fun getMessageRecordById(messageId: String): LiveData<MessageRecords>

    @Delete
    suspend fun deleteMessageRecord(messageRecords: MessageRecords)

    @Query("DELETE FROM message_records")
    suspend fun removeMessageRecords()
}