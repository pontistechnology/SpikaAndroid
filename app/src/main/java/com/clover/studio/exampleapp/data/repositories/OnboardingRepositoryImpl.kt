package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.daos.PhoneUserDao
import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.entity.PhoneUser
import com.clover.studio.exampleapp.data.models.networking.responses.AuthResponse
import com.clover.studio.exampleapp.data.services.OnboardingService
import com.clover.studio.exampleapp.utils.Tools.getHeaderMap
import com.google.gson.JsonObject
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val retrofitService: OnboardingService,
    private val userDao: UserDao,
    private val phoneUserDao: PhoneUserDao,
    private val sharedPrefs: SharedPreferencesRepository
) : OnboardingRepository {
    override suspend fun sendUserData(
        jsonObject: JsonObject
    ) {
        val responseData = retrofitService.sendUserData(
            getHeaderMap(sharedPrefs.readToken()), jsonObject
        )

        sharedPrefs.setNewUser(responseData.data.isNewUser)
    }

    override suspend fun verifyUserCode(
        jsonObject: JsonObject
    ): AuthResponse {
        val responseData =
            retrofitService.verifyUserCode(getHeaderMap(sharedPrefs.readToken()), jsonObject)

        userDao.insert(responseData.data.user)
        sharedPrefs.writeUserId(responseData.data.user.id)

        return responseData
    }

    override suspend fun sendUserContacts(
        contacts: List<String>
    ): AuthResponse = retrofitService.sendContacts(getHeaderMap(sharedPrefs.readToken()), contacts)

    override suspend fun writePhoneUsers(phoneUsers: List<PhoneUser>) =
        phoneUserDao.insert(phoneUsers)


    override suspend fun updateUser(
       jsonObject: JsonObject
    ): AuthResponse {
        val responseData =
            retrofitService.updateUser(getHeaderMap(sharedPrefs.readToken()), jsonObject)

        userDao.insert(responseData.data.user)
        sharedPrefs.writeUserId(responseData.data.user.id)

        return responseData
    }
}

interface OnboardingRepository {
    suspend fun sendUserData(jsonObject: JsonObject)
    suspend fun verifyUserCode(jsonObject: JsonObject): AuthResponse
    suspend fun sendUserContacts(contacts: List<String>): AuthResponse
    suspend fun writePhoneUsers(phoneUsers: List<PhoneUser>)
    suspend fun updateUser(jsonObject: JsonObject): AuthResponse
}