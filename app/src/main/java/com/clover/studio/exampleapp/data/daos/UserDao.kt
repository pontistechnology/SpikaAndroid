package com.clover.studio.exampleapp.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.data.models.entity.UserAndPhoneUser
import com.clover.studio.exampleapp.data.models.junction.UserWithRooms

@Dao
interface UserDao {

    @Upsert
    suspend fun upsert(user: User): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(users: List<User>)

    @Query("SELECT * FROM user")
    fun getUsers(): LiveData<List<User>>

    @Query("SELECT * FROM user")
    suspend fun getUsersLocally(): List<User>

    @Query("SELECT * FROM user WHERE id LIKE :userId LIMIT 1")
    fun getUserById(userId: Int): LiveData<User>

    @Query("SELECT * FROM user WHERE id IN (:userIds)")
    suspend fun getUsersByIds(userIds: List<Int>): List<User>

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM user")
    suspend fun removeUsers()

    @Transaction
    @Query("SELECT * FROM user WHERE id NOT LIKE :localId")
    fun getUserAndPhoneUser(localId: Int): LiveData<List<UserAndPhoneUser>>

    @Transaction
    @Query("SELECT * FROM user")
    fun getUserAndRooms(): LiveData<List<UserWithRooms>>

    // TODO add getMe() method for our user object
}