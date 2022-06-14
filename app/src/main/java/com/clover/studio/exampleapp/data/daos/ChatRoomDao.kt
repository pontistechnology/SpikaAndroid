package com.clover.studio.exampleapp.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.exampleapp.data.models.ChatRoom
import com.clover.studio.exampleapp.data.models.RoomAndMessageAndRecords
import com.clover.studio.exampleapp.data.models.junction.RoomUser
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers

@Dao
interface ChatRoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chatRoom: ChatRoom): Long

    @Query("SELECT * FROM room")
    fun getRooms(): LiveData<List<ChatRoom>>

    @Query("SELECT * FROM room")
    suspend fun getRoomsLocally(): List<ChatRoom>

    @Query("SELECT * FROM room WHERE room_id LIKE :roomId LIMIT 1")
    suspend fun getRoomById(roomId: Int): ChatRoom

    @Delete
    suspend fun deleteRoom(chatRoom: ChatRoom)

    @Query("DELETE FROM room")
    suspend fun removeRooms()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoomWithUsers(roomUser: RoomUser)

    // This method copies locally added fields to the database if present
    @Transaction
    suspend fun updateRoomTable(oldData: ChatRoom?, newData: ChatRoom) {
        if (oldData?.visitedRoom != null && newData.visitedRoom == null) {
            newData.visitedRoom = oldData.visitedRoom
        }
        insert(newData)
    }

    @Transaction
    @Query("SELECT * FROM room")
    fun getChatRoomAndMessageAndRecords(): LiveData<List<RoomAndMessageAndRecords>>

    @Transaction
    @Query("SELECT * FROM room WHERE room_id LIKE :roomId LIMIT 1")
    suspend fun getRoomAndUsers(roomId: Int): RoomWithUsers
}