package com.clover.studio.spikamessenger.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.spikamessenger.data.models.entity.ChatRoom
import com.clover.studio.spikamessenger.data.models.entity.RoomAndMessageAndRecords
import com.clover.studio.spikamessenger.data.models.entity.RoomWithLatestMessage
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.utils.helpers.Extensions.getDistinct

@Dao
interface ChatRoomDao : BaseDao<ChatRoom> {

    @Query("SELECT * FROM room")
    fun getAllRooms(): List<ChatRoom>

    @Query("SELECT * FROM room WHERE room_id LIKE :roomId LIMIT 1")
    suspend fun getRoomById(roomId: Int): ChatRoom

    @Query("SELECT COUNT(DISTINCT room.room_id)\n" +
            "FROM room\n" +
            "JOIN message ON Room.room_id = message.room_id_message\n" +
            "WHERE room.unread_count > 0 AND message.created_at_message IS NOT NULL")
    fun getRoomsUnreadCountLiveData(): LiveData<Int>

    fun getDistinctRoomsUnreadCount(): LiveData<Int> =
        getRoomsUnreadCountLiveData().getDistinct()

    @Query("SELECT * FROM room WHERE room_id LIKE :roomId LIMIT 1")
    fun getRoomByIdLiveData(roomId: Int): LiveData<ChatRoom>

    fun getDistinctRoomById(roomId: Int): LiveData<ChatRoom> =
        getRoomByIdLiveData(roomId).getDistinct()

    @Query("UPDATE room SET muted = :muted WHERE room_id LIKE :roomId")
    suspend fun updateRoomMuted(muted: Boolean, roomId: Int)

    @Query("UPDATE room SET pinned = :pinned WHERE room_id LIKE :roomId")
    suspend fun updateRoomPinned(pinned: Boolean, roomId: Int)

    /**
     * Use this method to delete rooms from local db. We cannot rely on the above one because
     * chat rooms might come with a field that is ignored.
     */
    @Query("DELETE FROM room WHERE room_id = :roomId")
    suspend fun deleteRoom(roomId: Int)

    @Query("DELETE FROM room")
    suspend fun removeRooms()

    @Transaction
    @Query(
        "SELECT room.*, message.* FROM room\n" +
                "LEFT JOIN (SELECT room_id_message, MAX(created_at_message) AS max_created_at FROM message GROUP BY room_id_message)\n" +
                "AS latestMessageTime ON room.room_id = latestMessageTime.room_id_message LEFT JOIN message\n" +
                "ON message.room_id_message = room.room_id AND message.created_at_message = latestMessageTime.max_created_at\n"
    )
    fun getAllRoomsWithLatestMessageAndRecord(): LiveData<List<RoomWithLatestMessage>>

    @Transaction
    @Query("SELECT * FROM room")
    fun getChatRoomAndMessageAndRecords(): LiveData<List<RoomAndMessageAndRecords>>

    @Transaction
    @Query("SELECT * FROM room WHERE room_id LIKE :roomId LIMIT 1")
    suspend fun getSingleRoomData(roomId: Int): RoomAndMessageAndRecords

    @Transaction
    @Query("SELECT * FROM room WHERE room_id LIKE :roomId LIMIT 1")
    fun getChatRoomAndMessageAndRecordsById(roomId: Int): LiveData<RoomAndMessageAndRecords>

    fun getDistinctChatRoomAndMessageAndRecordsById(roomId: Int): LiveData<RoomAndMessageAndRecords> =
        getChatRoomAndMessageAndRecordsById(roomId).getDistinct()

    @Transaction
    @Query("SELECT * FROM room WHERE room_id LIKE :roomId LIMIT 1")
    suspend fun getRoomAndUsers(roomId: Int): RoomWithUsers

    @Transaction
    @Query("SELECT * FROM room WHERE room_id LIKE :roomId LIMIT 1")
    fun getRoomAndUsersLiveData(roomId: Int): LiveData<RoomWithUsers>

    @Query("UPDATE room SET unread_count = 0 WHERE room_id LIKE :roomId")
    suspend fun resetUnreadCount(roomId: Int)

    @Query("UPDATE room SET room_exit =:roomExit WHERE room_id LIKE :roomId")
    suspend fun updateRoomExit(roomId: Int, roomExit: Boolean)

    @Query("UPDATE room SET deleted =:deleted WHERE room_id LIKE :roomId")
    suspend fun updateRoomDeleted(roomId: Int, deleted: Boolean)
}
