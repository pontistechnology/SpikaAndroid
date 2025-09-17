package com.clover.studio.spikamessenger.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.MessageAndRecords
import com.clover.studio.spikamessenger.data.models.entity.MessageBody
import com.clover.studio.spikamessenger.data.models.entity.ReferenceMessage

@Dao
interface MessageDao : BaseDao<Message> {

    @Query(
        "UPDATE message SET id = :id, from_user_id = :fromUserId, total_user_count = :totalUserCount," +
                " delivered_count = :deliveredCount, seen_count = :seenCount, type_message = :type, body = :body," +
                " created_at_message = :createdAt, modified_at_message = :modifiedAt, deleted_message = :deleted," +
                " reply_id = :replyId, reference_message = :referenceMessage, message_status=:status WHERE local_id = :localId"
    )
    suspend fun updateMessage(
        id: Int,
        fromUserId: Int,
        totalUserCount: Int,
        deliveredCount: Int,
        seenCount: Int,
        type: String,
        body: MessageBody,
        referenceMessage: ReferenceMessage?,
        createdAt: Long,
        modifiedAt: Long,
        deleted: Boolean,
        replyId: Long,
        localId: String,
        status: String
    )

    @Transaction
    @Query("SELECT * FROM message WHERE room_id_message= :roomId ORDER BY created_at_message DESC LIMIT :limit OFFSET :offset")
    fun getMessagesAndRecords(
        roomId: Int,
        limit: Int,
        offset: Int
    ): LiveData<List<MessageAndRecords>>

    @Query("SELECT COUNT(*) FROM message WHERE room_id_message= :roomId")
    suspend fun getMessageCount(roomId: Int): Int

    @Query("SELECT * FROM message WHERE id=:messageId LIMIT 1")
    suspend fun getMessage(messageId: Long): Message

    @Query("SELECT * FROM message WHERE local_id=:localId LIMIT 1")
    suspend fun getMessageByLocalId(localId: String): Message

    @Transaction
    @Query("UPDATE message SET seen_count=:seenCount WHERE id=:messageId")
    suspend fun updateMessageSeenCount(
        messageId: Long,
        seenCount: Int,
    )

    @Transaction
    @Query("UPDATE message SET delivered_count=:deliveredCount WHERE id=:messageId")
    suspend fun updateMessageDeliveredCount(
        messageId: Long,
        deliveredCount: Int,
    )

    @Query("DELETE FROM message WHERE id IN (:messageId)")
    suspend fun deleteMessage(messageId: List<Long>)

    @Query("DELETE FROM message")
    suspend fun removeMessages()

    @Query("UPDATE message SET message_status=:messageStatus WHERE local_id=:localId")
    suspend fun updateMessageStatus(messageStatus: String, localId: String)

    @Query("UPDATE message SET uri=:uri WHERE local_id=:localId ")
    suspend fun updateLocalUri(localId: String, uri: String)

    @Query("UPDATE message SET thumb_uri=:uri WHERE local_id=:localId ")
    suspend fun updateThumbUri(localId: String, uri: String)
}
