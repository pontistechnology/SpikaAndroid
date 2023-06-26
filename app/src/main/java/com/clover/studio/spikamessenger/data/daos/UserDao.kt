package com.clover.studio.spikamessenger.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.data.models.entity.UserAndPhoneUser
import com.clover.studio.spikamessenger.data.models.junction.UserWithRooms
import com.clover.studio.spikamessenger.utils.helpers.Extensions.getDistinct

@Dao
interface UserDao: BaseDao<User> {
    @Query("SELECT * FROM user")
    fun getUsers(): LiveData<List<User>>

    @Query("SELECT * FROM user WHERE id LIKE :userId LIMIT 1")
    fun getUserById(userId: Int): LiveData<User>

    fun getDistinctUserById(userId: Int): LiveData<User> =
        getUserById(userId).getDistinct()

    @Query("SELECT * FROM user WHERE id IN (:userIds)")
    suspend fun getUsersByIds(userIds: List<Int>): List<User>

    @Query("DELETE FROM user")
    suspend fun removeUsers()

    @Transaction
    @Query("SELECT * FROM user WHERE id NOT LIKE :localId")
    fun getUserAndPhoneUser(localId: Int): LiveData<List<UserAndPhoneUser>>

    @Transaction
    @Query("SELECT * FROM user")
    fun getUserAndRooms(): LiveData<List<UserWithRooms>>

    fun getDistinctUserAndRooms(): LiveData<List<UserWithRooms>> =
        getUserAndRooms().getDistinct()
}