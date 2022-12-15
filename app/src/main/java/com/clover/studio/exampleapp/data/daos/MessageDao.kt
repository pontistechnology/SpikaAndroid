package com.clover.studio.exampleapp.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.exampleapp.data.models.entity.Message
import com.clover.studio.exampleapp.data.models.entity.MessageBody

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(messages: List<Message>)

    @Query("UPDATE message SET id = :id, from_user_id = :fromUserId, total_user_count = :totalUserCount, delivered_count = :deliveredCount, seen_count = :seenCount, type = :type, body = :body, created_at = :createdAt, modified_at = :modifiedAt, deleted = :deleted, replyId = :replyId WHERE local_id = :localId")
    suspend fun updateMessage(
        id: Int,
        fromUserId: Int,
        totalUserCount: Int,
        deliveredCount: Int,
        seenCount: Int,
        type: String,
        body: MessageBody,
        createdAt: Long,
        modifiedAt: Long,
        deleted: Boolean,
        replyId: Long,
        localId: String
    )

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