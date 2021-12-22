package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.networking.AuthResponse
import com.clover.studio.exampleapp.data.services.OnboardingService
import com.google.gson.JsonObject
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val retrofitService: OnboardingService,
    private val userDao: UserDao,
    private val sharedPrefs: SharedPreferencesRepository
) : OnboardingRepository {
    override suspend fun sendUserData(
        phoneNumber: String,
        phoneNumberHashed: String,
        countryCode: String,
        deviceId: String
    ) {
        retrofitService.sendUserData(phoneNumber, phoneNumberHashed, countryCode, deviceId)
    }

    override suspend fun verifyUserCode(
        code: String,
        deviceId: String
    ): AuthResponse {
        val responseData = retrofitService.verifyUserCode(code, deviceId)

        userDao.insert(responseData.data.user)
        sharedPrefs.writeUserId(responseData.data.user.id)

        return responseData
    }

    override suspend fun sendUserContacts(
        token: String,
        contacts: List<String>
    ): AuthResponse = retrofitService.sendContacts(token, contacts)

    override suspend fun updateUser(
        token: String,
        userMap: Map<String, String>
    ): AuthResponse {
        val responseData = retrofitService.updateUser(token, userMap)

        userDao.insert(responseData.data.user)
        sharedPrefs.writeUserId(responseData.data.user.id)

        return responseData
    }
}

interface OnboardingRepository {
    suspend fun sendUserData(
        phoneNumber: String,
        phoneNumberHashed: String,
        countryCode: String,
        deviceId: String
    )

    suspend fun verifyUserCode(
        code: String,
        deviceId: String
    ): AuthResponse

    suspend fun sendUserContacts(
        token: String,
        contacts: List<String>
    ): AuthResponse

    suspend fun updateUser(
        token: String,
        userMap: Map<String, String>
    ): AuthResponse
}