package com.clover.studio.exampleapp.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.data.models.UserAndPhoneUser
import com.clover.studio.exampleapp.data.models.junction.UserWithRooms

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM user")
    fun getUsers(): LiveData<List<User>>

    @Query("SELECT * FROM user")
    suspend fun getUsersLocally(): List<User>

    @Query("SELECT * FROM user WHERE id LIKE :userId LIMIT 1")
    fun getUserById(userId: Int): LiveData<User>

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM user")
    suspend fun removeUsers()

    @Transaction
    @Query("SELECT * FROM user")
    fun getUserAndPhoneUser(): LiveData<List<UserAndPhoneUser>>

    @Transaction
    @Query("SELECT * FROM user")
    fun getUserAndRooms(): LiveData<List<UserWithRooms>>

    // TODO add getMe() method for our user object
}