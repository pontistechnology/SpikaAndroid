package com.clover.studio.spikamessenger.data.daos

import androidx.room.*
import com.clover.studio.spikamessenger.data.models.junction.RoomUser

@Dao
interface RoomUserDao: BaseDao<RoomUser> {

    // Delete all room users with specified user_id
    @Transaction
    @Query("DELETE FROM room_user WHERE user_id LIKE :userId AND room_id LIKE :roomId")
    suspend fun deleteRoomUser(userId: Int, roomId: Int)

    @Transaction
    @Query("DELETE FROM room_user WHERE user_id IN (:userIds) AND room_id LIKE :roomId")
    suspend fun deleteRoomUsers(userIds: List<Int>, roomId: Int)

    @Query("SELECT * FROM room_user WHERE room_id LIKE :roomId AND user_id LIKE :userId LIMIT 1")
    suspend fun getRoomUserById(roomId: Int, userId: Int): RoomUser

    @Query("UPDATE room_user SET isAdmin = 0 WHERE room_id LIKE :roomId AND user_id LIKE :userId")
    suspend fun removeAdmin(roomId: Int, userId: Int)

    @Query("SELECT room_id FROM room_user WHERE user_id = :userId AND room_id IN (SELECT room_id FROM room WHERE type = 'private')")
    suspend fun doesPrivateRoomExistForUser(userId: Int): Int?
}
