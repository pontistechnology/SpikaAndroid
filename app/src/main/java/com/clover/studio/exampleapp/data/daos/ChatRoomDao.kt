package com.clover.studio.exampleapp.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.exampleapp.data.models.ChatRoom
import com.clover.studio.exampleapp.data.models.RoomAndMessageAndRecords
import com.clover.studio.exampleapp.data.models.junction.RoomUser
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.data.models.networking.ChatRoomUpdate

@Dao
interface ChatRoomDao {

    // room table functions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chatRoom: ChatRoom): Long

    // room table functions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chatRooms: List<ChatRoom>)

    @Query("SELECT * FROM room")
    fun getRooms(): LiveData<List<ChatRoom>>

    @Query("SELECT * FROM room")
    suspend fun getRoomsLocally(): List<ChatRoom>

    @Query("SELECT * FROM room WHERE room_id LIKE :roomId LIMIT 1")
    suspend fun getRoomById(roomId: Int): ChatRoom

    @Delete
    suspend fun deleteRoom(chatRoom: ChatRoom)

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
        insert(newData)
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
        insert(chatRooms)
    }

    // room_user table functions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoomWithUsers(roomUser: List<RoomUser>)

    @Delete
    suspend fun deleteRoomUser(roomUser: RoomUser)

    @Query("SELECT * FROM room_user WHERE room_id LIKE :roomId AND id LIKE :userId LIMIT 1")
    suspend fun getRoomUserById(roomId: Int, userId: Int): RoomUser
}