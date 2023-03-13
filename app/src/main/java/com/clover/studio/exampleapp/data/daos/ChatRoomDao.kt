package com.clover.studio.exampleapp.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.exampleapp.data.models.entity.ChatRoom
import com.clover.studio.exampleapp.data.models.entity.RoomAndMessageAndRecords
import com.clover.studio.exampleapp.data.models.junction.RoomUser
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.data.models.networking.ChatRoomUpdate

@Dao
interface ChatRoomDao {

    // room table functions
    @Upsert
    suspend fun upsert(chatRoom: ChatRoom): Long

    // room table functions
    @Upsert
    suspend fun upsert(chatRooms: List<ChatRoom>)

    @Query("SELECT * FROM room")
    fun getRooms(): LiveData<List<ChatRoom>>

    @Query("SELECT * FROM room WHERE room_id LIKE :roomId LIMIT 1")
    suspend fun getRoomById(roomId: Int): ChatRoom

    @Query("SELECT * FROM room WHERE room_id LIKE :roomId LIMIT 1")
    fun getRoomByIdLiveData(roomId: Int): LiveData<ChatRoom>

    @Delete
    suspend fun deleteRoom(chatRoom: ChatRoom)

    @Query("UPDATE room SET muted = :muted WHERE room_id LIKE :roomId")
    suspend fun updateRoomMuted(muted: Boolean, roomId: Int)

    @Query("UPDATE room SET pinned = :pinned WHERE room_id LIKE :roomId")
    suspend fun updateRoomPinned(pinned: Boolean, roomId: Int)

    @Query("UPDATE room SET visited_room =:visitedRoom WHERE room_id LIKE :roomId")
    suspend fun updateRoomVisited(visitedRoom: Long, roomId: Int)

    /**
     * Use this method to delete rooms from local db. We cannot rely on the above one because
     * chat rooms might come with a field that is ignored.
     */
    @Query("DELETE FROM room WHERE room_id = :roomId")
    suspend fun deleteRoom(roomId: Int)

    @Query("DELETE FROM room")
    suspend fun removeRooms()

    @Transaction
    @Query("SELECT * FROM room")
    fun getChatRoomAndMessageAndRecords(): LiveData<List<RoomAndMessageAndRecords>>

    @Transaction
    @Query("SELECT * FROM room WHERE room_id LIKE :roomId LIMIT 1")
    suspend fun getSingleRoomData(roomId: Int): RoomAndMessageAndRecords

    @Transaction
    @Query("SELECT * FROM room WHERE room_id LIKE :roomId LIMIT 1")
    fun getChatRoomAndMessageAndRecordsById(roomId: Int): LiveData<RoomAndMessageAndRecords>

    @Transaction
    @Query("SELECT * FROM room WHERE room_id LIKE :roomId LIMIT 1")
    suspend fun getRoomAndUsers(roomId: Int): RoomWithUsers

    @Transaction
    @Query("SELECT * FROM room WHERE room_id LIKE :roomId LIMIT 1")
    fun getRoomAndUsersLiveData(roomId: Int): LiveData<RoomWithUsers>

    // This method copies locally added fields to the database if present
    @Transaction
    suspend fun updateRoomTable(oldData: ChatRoom?, newData: ChatRoom) {
        if (oldData?.visitedRoom != null && newData.visitedRoom == null) {
            newData.visitedRoom = oldData.visitedRoom
        }
        upsert(newData)
    }

    @Transaction
    suspend fun updateRoomTable(chatRoomUpdate: List<ChatRoomUpdate>) {
        val chatRooms: MutableList<ChatRoom> = ArrayList()
        for (chatRoom in chatRoomUpdate) {
            if (chatRoom.oldRoom?.visitedRoom != null && chatRoom.newRoom.visitedRoom == null) {
                chatRoom.newRoom.visitedRoom = chatRoom.oldRoom.visitedRoom
            }
            chatRooms.add(chatRoom.newRoom)
        }
        upsert(chatRooms)
    }

    /** room_user table functions */
    @Upsert
    suspend fun insertRoomWithUsers(roomUser: List<RoomUser>)

    // Delete all room users with specified user_id
    @Transaction
    @Query("DELETE FROM room_user WHERE id IN (:userIds)")
    suspend fun deleteRoomUsers(userIds: List<Int>)

    @Delete
    suspend fun deleteRoomUser(roomUser: RoomUser)

    @Query("SELECT * FROM room_user WHERE room_id LIKE :roomId AND id LIKE :userId LIMIT 1")
    suspend fun getRoomUserById(roomId: Int, userId: Int): RoomUser

    @Transaction
    @Query("DELETE FROM message_records WHERE id LIKE :id AND user_id LIKE :userId")
    suspend fun deleteReactionRecord(id: Int, userId: Int)

    // Private chat: delete all records
    @Transaction
    @Query("DELETE FROM message_records WHERE message_id LIKE :id AND type='reaction'")
    suspend fun deleteAllReactions(id: Int)

    @Query("UPDATE room SET room_exit =:roomExit WHERE room_id LIKE :roomId")
    suspend fun updateRoomExit(roomId: Int, roomExit: Boolean)

    @Query("UPDATE room_user SET isAdmin = 0 WHERE room_id LIKE :roomId AND id LIKE :userId")
    suspend fun removeAdmin(roomId: Int, userId: Int)
}
