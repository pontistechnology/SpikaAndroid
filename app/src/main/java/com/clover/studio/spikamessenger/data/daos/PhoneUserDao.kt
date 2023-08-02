package com.clover.studio.spikamessenger.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.spikamessenger.data.models.entity.PhoneUser
import com.clover.studio.spikamessenger.utils.helpers.Extensions.getDistinct

@Dao
interface PhoneUserDao: BaseDao<PhoneUser> {
    @Query("SELECT * FROM phone_user")
    fun getPhoneUsers(): LiveData<List<PhoneUser>>

    @Query("SELECT * FROM phone_user WHERE number LIKE :number LIMIT 1")
    fun getUserByNumber(number: String): LiveData<PhoneUser>

    fun getDistinctUserByNumber(number: String): LiveData<PhoneUser> =
        getUserByNumber(number).getDistinct()

    @Query("DELETE FROM phone_user")
    suspend fun removePhoneUsers()
}