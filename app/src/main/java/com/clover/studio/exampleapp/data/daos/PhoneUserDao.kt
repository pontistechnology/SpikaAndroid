package com.clover.studio.exampleapp.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.exampleapp.data.models.PhoneUser

@Dao
interface PhoneUserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(phoneUser: PhoneUser): Long

    @Query("SELECT * FROM phone_user")
    fun getPhoneUsers(): LiveData<List<PhoneUser>>

    @Query("SELECT * FROM phone_user WHERE number LIKE :number LIMIT 1")
    fun getUserByNumber(number: String): LiveData<PhoneUser>

    @Delete
    suspend fun deletePhoneUser(phoneUser: PhoneUser)

    @Query("DELETE FROM phone_user")
    suspend fun removePhoneUsers()
}